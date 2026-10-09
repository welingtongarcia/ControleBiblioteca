package br.com.minhagibiteca.catalog;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import br.com.minhagibiteca.catalog.application.*;
import br.com.minhagibiteca.catalog.infrastructure.OpenLibraryIsbnProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenLibraryIsbnProviderTest {
    private MockRestServiceServer server;
    private OpenLibraryIsbnProvider provider;
    @BeforeEach void setup() {
        var builder = RestClient.builder().baseUrl("https://openlibrary.org");
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new OpenLibraryIsbnProvider(builder.build(), new ObjectMapper());
    }
    private void response(String json) {
        server.expect(request -> {
            assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
            assertThat(request.getURI().getHost()).isEqualTo("openlibrary.org");
            assertThat(request.getURI().getPath()).isEqualTo("/api/books");
            assertThat(request.getURI().getQuery()).contains("bibkeys=ISBN:9780140328721", "format=json", "jscmd=data");
        }).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }
    @Test void mapsOnlyExpectedFieldsAndGeneratesTrustedSourceUrl() {
        response("""
                {"ISBN:9780140328721":{"title":"HQ teste","publish_date":"March 3, 2001",
                  "publishers":[{"name":"Editora"}],"authors":[{"name":"Autor"}],
                  "url":"javascript:alert(1)","cover":{"large":"http://127.0.0.1/private"}}}
                """);
        var found = provider.lookup("9780140328721").orElseThrow();
        assertThat(found.title()).isEqualTo("HQ teste"); assertThat(found.year()).isEqualTo(2001);
        assertThat(found.authors()).containsExactly("Autor"); assertThat(found.publishers()).containsExactly("Editora");
        assertThat(found.sourceUrl()).isEqualTo("https://openlibrary.org/isbn/9780140328721");
        assertThat(found.coverAvailable()).isTrue(); server.verify();
    }
    @Test void handlesMissingDataAndIgnoresUnusableYear() {
        response("{\"ISBN:9780140328721\":{\"title\":\"HQ\",\"publish_date\":\"unknown 1700\"}}");
        var found = provider.lookup("9780140328721").orElseThrow();
        assertThat(found.year()).isNull(); assertThat(found.authors()).isEmpty(); assertThat(found.coverAvailable()).isFalse();
    }
    @Test void returnsEmptyWhenNotFound() {
        response("{}"); assertThat(provider.lookup("9780140328721")).isEmpty();
    }
    @Test void rejectsMalformedAndOversizeResponses() {
        response("{invalid");
        assertThatThrownBy(() -> provider.lookup("9780140328721")).isInstanceOf(IsbnLookupService.LookupException.class);
        server.verify(); server.reset(); response(" ".repeat(256 * 1024 + 1));
        assertThatThrownBy(() -> provider.lookup("9780140328721")).isInstanceOf(IsbnLookupService.LookupException.class);
    }
    @Test void handlesServiceErrorsWithoutLeakingResponse() {
        server.expect(request -> {}).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("secret"));
        assertThatThrownBy(() -> provider.lookup("9780140328721")).hasMessageContaining("continue o cadastro manualmente").hasMessageNotContaining("secret");
    }
    @Test void handlesTimeoutAsManualFallback() {
        server.expect(request -> {}).andRespond(withException(new java.net.SocketTimeoutException("timeout")));
        assertThatThrownBy(() -> provider.lookup("9780140328721")).isInstanceOf(IsbnLookupService.LookupException.class)
                .hasMessageContaining("continue o cadastro manualmente");
    }
}
