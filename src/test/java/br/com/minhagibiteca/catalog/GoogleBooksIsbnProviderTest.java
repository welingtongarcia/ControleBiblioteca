package br.com.minhagibiteca.catalog;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import br.com.minhagibiteca.catalog.application.*;
import br.com.minhagibiteca.catalog.infrastructure.GoogleBooksIsbnProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GoogleBooksIsbnProviderTest {
    private MockRestServiceServer server;
    private RestClient client;
    private GoogleBooksIsbnProvider provider;
    @BeforeEach void setup() {
        var builder = RestClient.builder().baseUrl("https://www.googleapis.com");
        server = MockRestServiceServer.bindTo(builder).build(); client = builder.build();
        provider = new GoogleBooksIsbnProvider(client, new ObjectMapper(), "test-key");
    }
    private void response(String json) {
        server.expect(request -> {
            assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
            assertThat(request.getURI().getScheme()).isEqualTo("https");
            assertThat(request.getURI().getHost()).isEqualTo("www.googleapis.com");
            assertThat(request.getURI().getPath()).isEqualTo("/books/v1/volumes");
            assertThat(request.getURI().getQuery()).isEqualTo("q=isbn:9786559601325&maxResults=5&key=test-key");
        }).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }
    @Test void mapsMatchingResultAndIgnoresRemoteUrlsAndUnrelatedFirstItem() {
        response("""
            {"items":[{"volumeInfo":{"title":"Errado","industryIdentifiers":[{"type":"ISBN_13","identifier":"9780140328721"}]}},
            {"volumeInfo":{"title":"Quadrinho <script>","publisher":"Editora","publishedDate":"2021-05-01","authors":["Autor","Autor",99],
            "industryIdentifiers":[{"type":"ISBN_13","identifier":"978-6559601325"}],"imageLinks":{"thumbnail":"http://127.0.0.1"},"infoLink":"javascript:bad"}}]}
            """);
        var found = provider.lookup("978-6559601325").orElseThrow();
        assertThat(found.title()).isEqualTo("Quadrinho <script>"); assertThat(found.year()).isEqualTo(2021);
        assertThat(found.publishers()).containsExactly("Editora"); assertThat(found.authors()).containsExactly("Autor");
        assertThat(found.coverAvailable()).isTrue(); assertThat(found.sourceName()).isEqualTo("Google Books");
        assertThat(found.sourceUrl()).isEqualTo("https://books.google.com/books?vid=ISBN9786559601325"); server.verify();
    }
    @Test void acceptsEquivalentIsbn10AndMissingOptionalFields() {
        server.expect(request -> {}).andRespond(withSuccess("""
            {"items":[{"volumeInfo":{"title":"Livro","publishedDate":"desconhecida","industryIdentifiers":[{"type":"ISBN_10","identifier":"0140328726"}]}}]}
            """, MediaType.APPLICATION_JSON));
        var found = provider.lookup("9780140328721").orElseThrow();
        assertThat(found.year()).isNull(); assertThat(found.authors()).isEmpty(); assertThat(found.publishers()).isEmpty(); assertThat(found.coverAvailable()).isFalse();
    }
    @Test void rejectsUnrelatedInvalidOrMissingIdentifiers() {
        response("""
            {"items":[{"volumeInfo":{"title":"Sem ISBN"}},
            {"volumeInfo":{"title":"Inválido","industryIdentifiers":[{"type":"ISBN_13","identifier":"9786559601324"}]}},
            {"volumeInfo":{"title":"Outro","industryIdentifiers":[{"type":"OTHER","identifier":"9786559601325"}]}}]}
            """);
        assertThat(provider.lookup("9786559601325")).isEmpty();
    }
    @Test void acceptsExplicitEmptyResult() {
        response("{\"totalItems\":0}"); assertThat(provider.lookup("9786559601325")).isEmpty();
        server.reset(); response("{\"items\":[]}"); assertThat(provider.lookup("9786559601325")).isEmpty();
    }
    @Test void missingKeyDoesNotSendRequestOrClaimAbsenceInBothSources() {
        var unconfigured = new GoogleBooksIsbnProvider(client, new ObjectMapper(), " ");
        assertThatThrownBy(() -> unconfigured.lookup("9786559601325")).hasMessageContaining("precisa de configuração"); server.verify();
    }
    @Test void rejectsInvalidIsbnBeforeNetwork() {
        assertThatThrownBy(() -> provider.lookup("123")).hasMessageContaining("válido"); server.verify();
    }
    @Test void rejectsMalformedAndOversizedResponses() {
        for (String json : new String[]{"broken", "{}", "[]", "{\"items\":{}}", "{\"error\":{}}", " ".repeat(256 * 1024 + 1)}) {
            server.reset(); response(json);
            assertThatThrownBy(() -> provider.lookup("9786559601325")).hasMessageContaining("não foi possível concluir");
        }
    }
    @Test void quotaFailureDoesNotExposeKeyBodyOrCause() {
        server.expect(request -> {}).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("test-key secret"));
        assertThatThrownBy(() -> provider.lookup("9786559601325")).hasMessageNotContaining("test-key").hasMessageNotContaining("secret").hasCause(null);
    }
    @Test void timeoutPreservesSafeManualFallback() {
        server.expect(request -> {}).andRespond(withException(new java.net.SocketTimeoutException("test-key")));
        assertThatThrownBy(() -> provider.lookup("9786559601325")).hasMessageContaining("cadastro manualmente").hasMessageNotContaining("test-key").hasCause(null);
    }
}
