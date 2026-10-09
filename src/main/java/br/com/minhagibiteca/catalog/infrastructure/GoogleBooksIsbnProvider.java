package br.com.minhagibiteca.catalog.infrastructure;

import br.com.minhagibiteca.catalog.application.*;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GoogleBooksIsbnProvider implements IsbnProvider {
    private final RestClient client;
    private final ObjectMapper mapper;
    private final String apiKey;
    public GoogleBooksIsbnProvider(@Qualifier("googleBooksRestClient") RestClient client, ObjectMapper mapper,
            @Value("${app.isbn.google-books-api-key:}") String apiKey) {
        this.client = client; this.mapper = mapper; this.apiKey = apiKey.strip();
    }
    @Override public Optional<IsbnMetadata> lookup(String input) {
        String isbn = IsbnLookupService.normalize(input);
        if (apiKey.isEmpty()) throw new IsbnLookupService.LookupException("ISBN não encontrado na Open Library. A segunda fonte, Google Books, precisa de configuração pelo administrador. Continue o cadastro manualmente.");
        try {
            return client.get().uri(b -> b.path("/books/v1/volumes").queryParam("q", "isbn:" + isbn)
                    .queryParam("maxResults", 5).queryParam("key", apiKey).build()).exchange((request, response) -> {
                if (!response.getStatusCode().is2xxSuccessful()) throw unavailable();
                byte[] bytes = response.getBody().readNBytes(256 * 1024 + 1);
                if (bytes.length > 256 * 1024) throw unavailable();
                JsonNode root = mapper.readTree(bytes);
                if (root == null || !root.isObject() || root.has("error")) throw unavailable();
                JsonNode items = root.get("items");
                if (items == null && root.path("totalItems").isIntegralNumber() && root.path("totalItems").asLong() == 0) return Optional.empty();
                if (items == null || !items.isArray()) throw unavailable();
                for (JsonNode item : items) {
                    JsonNode info = item.path("volumeInfo");
                    if (!matches(info.path("industryIdentifiers"), isbn)) continue;
                    String title = text(info.get("title"), 240);
                    if (title == null) continue;
                    String date = text(info.get("publishedDate"), 120);
                    Integer year = null;
                    if (date != null && date.matches("(18[0-9]{2}|19[0-9]{2}|20[0-9]{2}|2100)(-[0-9]{2}(-[0-9]{2})?)?")) year = Integer.valueOf(date.substring(0, 4));
                    String publisher = text(info.get("publisher"), 180);
                    List<String> authors = new ArrayList<>();
                    if (info.path("authors").isArray()) for (JsonNode author : info.path("authors")) {
                        String name = text(author, 180);
                        if (name != null && !authors.contains(name)) authors.add(name);
                        if (authors.size() == 10) break;
                    }
                    return Optional.of(new IsbnMetadata(isbn, title, year, publisher == null ? List.of() : List.of(publisher),
                            List.copyOf(authors), info.path("imageLinks").isObject(), IsbnMetadata.Source.GOOGLE_BOOKS));
                }
                return Optional.empty();
            });
        } catch (IsbnLookupService.LookupException error) { throw error;
        } catch (Exception error) { throw unavailable(); }
    }
    private boolean matches(JsonNode identifiers, String isbn) {
        if (!identifiers.isArray()) return false;
        String canonical = IsbnLookupService.canonical(isbn);
        for (JsonNode identifier : identifiers) {
            String type = identifier.path("type").asText();
            if (!type.equals("ISBN_10") && !type.equals("ISBN_13")) continue;
            String value = text(identifier.get("identifier"), 32);
            if (value != null) try {
                if (IsbnLookupService.canonical(value).equals(canonical)) return true;
            } catch (IsbnLookupService.LookupException ignored) { /* Identificador inválido da fonte não é correspondência. */ }
        }
        return false;
    }
    private String text(JsonNode node, int max) {
        if (node == null || !node.isTextual()) return null;
        String value = node.asText().replaceAll("[\\p{Cntrl}]", " ").strip();
        return value.isEmpty() ? null : value.substring(0, Math.min(max, value.length()));
    }
    private IsbnLookupService.LookupException unavailable() {
        return new IsbnLookupService.LookupException("ISBN não encontrado na Open Library, mas não foi possível concluir a consulta ao Google Books. Verifique a configuração ou tente depois. Continue o cadastro manualmente.");
    }
}
