package br.com.minhagibiteca.catalog;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import br.com.minhagibiteca.catalog.application.*;
import br.com.minhagibiteca.catalog.infrastructure.FreeMetadataSearchProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class FreeMetadataSearchProviderTest {
    private MockRestServiceServer openServer, googleServer;
    private RestClient open, google;
    private FreeMetadataSearchProvider provider;
    @BeforeEach void setup() {
        var ob = RestClient.builder().baseUrl("https://openlibrary.org"); var gb = RestClient.builder().baseUrl("https://www.googleapis.com");
        openServer = MockRestServiceServer.bindTo(ob).build(); googleServer = MockRestServiceServer.bindTo(gb).build();
        open = ob.build(); google = gb.build(); provider = new FreeMetadataSearchProvider(open, google, new ObjectMapper(), "secret-key");
    }
    @Test void openLibraryUsesEditionYearNotWorkYearAndSafeAttribution() {
        openServer.expect(request -> {
            assertThat(request.getURI().getHost()).isEqualTo("openlibrary.org");
            assertThat(request.getURI().getQuery()).contains("q=Batman", "lang=pt", "limit=5", "editions.publish_date");
        }).andRespond(withSuccess("""
            {"docs":[{"title":"Original","first_publish_year":1987,"author_name":["Autor","Autor",99],"editions":{"docs":[
            {"key":"/books/OL123M","title":"Batman <script>","isbn":["bad","9780140328721"],"publish_date":["2021"],"publisher":["Editora"],"url":"http://127.0.0.1"},
            {"key":"/books/OL124M","title":"Sem data"}, {"key":"javascript:evil","title":"Inválido"}]} }]}
            """, MediaType.APPLICATION_JSON));
        var result = provider.search(MetadataCandidate.Source.OPEN_LIBRARY, "Batman");
        assertThat(result).hasSize(2); assertThat(result.getFirst().year()).isEqualTo(2021);
        assertThat(result.getFirst().isbn()).isEqualTo("9780140328721"); assertThat(result.getFirst().authors()).containsExactly("Autor");
        assertThat(result.getFirst().sourceUrl()).isEqualTo("https://openlibrary.org/books/OL123M"); assertThat(result.get(1).year()).isNull();
        openServer.verify(); googleServer.verify();
    }
    @Test void googleAllowsMissingIsbnAndBuildsLinksOnlyFromValidIds() {
        googleServer.expect(request -> assertThat(request.getURI().getQuery()).contains("q=intitle:Batman", "langRestrict=pt", "maxResults=5"))
            .andRespond(withSuccess("""
            {"items":[{"id":"safe-ID_1","volumeInfo":{"title":"Batman","publishedDate":"2020-05","publisher":"Editora","authors":["Autor"],"infoLink":"javascript:evil"}},
            {"id":"http://evil","volumeInfo":{"title":"Ruim"}}]}
            """, MediaType.APPLICATION_JSON));
        var result = provider.search(MetadataCandidate.Source.GOOGLE_BOOKS, "Batman");
        assertThat(result).hasSize(1); assertThat(result.getFirst().isbn()).isNull(); assertThat(result.getFirst().year()).isEqualTo(2020);
        assertThat(result.getFirst().sourceUrl()).isEqualTo("https://books.google.com/books?id=safe-ID_1");
        openServer.verify(); googleServer.verify();
    }
    @Test void missingGoogleKeyDoesNotCallEitherSource() {
        var unconfigured = new FreeMetadataSearchProvider(open, google, new ObjectMapper(), " ");
        assertThatThrownBy(() -> unconfigured.search(MetadataCandidate.Source.GOOGLE_BOOKS, "Batman")).hasMessageContaining("chave gratuita");
        openServer.verify(); googleServer.verify();
    }
    @Test void rejectsMalformedOversizedAndApiErrorWithoutSecrets() {
        for (String json : new String[]{"broken", "[]", "{}", "{\"items\":{}}", "{\"error\":\"secret-key\"}", " ".repeat(256 * 1024 + 1)}) {
            googleServer.reset(); googleServer.expect(request -> {}).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
            assertThatThrownBy(() -> provider.search(MetadataCandidate.Source.GOOGLE_BOOKS, "Batman")).hasMessageContaining("Não foi possível").hasMessageNotContaining("secret-key").hasCause(null);
        }
    }
    @Test void emptyResultsAreNotFailuresAndQuotaIsNotAbsence() {
        googleServer.expect(request -> {}).andRespond(withSuccess("{\"totalItems\":0}", MediaType.APPLICATION_JSON));
        assertThat(provider.search(MetadataCandidate.Source.GOOGLE_BOOKS, "Batman")).isEmpty();
        openServer.expect(request -> {}).andRespond(withSuccess("{\"docs\":[]}", MediaType.APPLICATION_JSON));
        assertThat(provider.search(MetadataCandidate.Source.OPEN_LIBRARY, "Batman")).isEmpty();
        googleServer.reset(); googleServer.expect(request -> {}).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("secret-key"));
        assertThatThrownBy(() -> provider.search(MetadataCandidate.Source.GOOGLE_BOOKS, "Batman")).hasMessageContaining("não significa").hasMessageNotContaining("secret-key");
    }
    @Test void validatesBeforeHttpAndTimeoutDoesNotExposeCause() {
        assertThatThrownBy(() -> provider.search(MetadataCandidate.Source.OPEN_LIBRARY, "ab")).isInstanceOf(MetadataSearchService.SearchException.class);
        openServer.expect(request -> {}).andRespond(withException(new java.net.SocketTimeoutException("secret-key")));
        assertThatThrownBy(() -> provider.search(MetadataCandidate.Source.OPEN_LIBRARY, "Batman")).hasMessageContaining("manualmente").hasCause(null);
        openServer.verify(); googleServer.verify();
    }
}
