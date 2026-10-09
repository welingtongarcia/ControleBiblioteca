package br.com.minhagibiteca.catalog.infrastructure;

import br.com.minhagibiteca.catalog.application.*;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenLibraryIsbnProvider implements IsbnProvider {
    private static final int MAX_BYTES = 256 * 1024;
    private final RestClient client;
    private final ObjectMapper mapper;
    public OpenLibraryIsbnProvider(@Qualifier("isbnRestClient") RestClient client, ObjectMapper mapper) { this.client = client; this.mapper = mapper; }
    @Override public Optional<IsbnMetadata> lookup(String input) {
        String isbn = IsbnLookupService.normalize(input);
        try {
            return client.get().uri(b -> b.path("/api/books").queryParam("bibkeys", "ISBN:" + isbn)
                    .queryParam("format", "json").queryParam("jscmd", "data").build()).exchange((request, response) -> {
                if (!response.getStatusCode().is2xxSuccessful()) throw unavailable();
                byte[] bytes = response.getBody().readNBytes(MAX_BYTES + 1);
                if (bytes.length > MAX_BYTES) throw unavailable();
                JsonNode root = mapper.readTree(bytes);
                if (root == null || !root.isObject()) throw unavailable();
                JsonNode book = root.get("ISBN:" + isbn);
                if (book == null) return Optional.empty();
                if (!book.isObject()) throw unavailable();
                String title = text(book.get("title"), 240);
                if (title == null) throw unavailable();
                String date = text(book.get("publish_date"), 120);
                Integer year = null;
                if (date != null) {
                    var match = Pattern.compile("(?<![0-9])(18[0-9]{2}|19[0-9]{2}|20[0-9]{2}|2100)(?![0-9])").matcher(date);
                    if (match.find()) year = Integer.valueOf(match.group());
                }
                return Optional.of(new IsbnMetadata(isbn, title, year, names(book.get("publishers")), names(book.get("authors")), book.path("cover").isObject()));
            });
        } catch (IsbnLookupService.LookupException error) { throw error;
        } catch (Exception error) { throw unavailable(); }
    }
    private String text(JsonNode node, int max) {
        if (node == null || !node.isTextual()) return null;
        String value = node.asText().replaceAll("[\\p{Cntrl}]", " ").strip();
        return value.isEmpty() ? null : value.substring(0, Math.min(max, value.length()));
    }
    private List<String> names(JsonNode array) {
        if (array == null || !array.isArray()) return List.of();
        List<String> result = new ArrayList<>();
        for (JsonNode item : array) {
            String name = text(item.get("name"), 180);
            if (name != null && !result.contains(name)) result.add(name);
            if (result.size() == 10) break;
        }
        return List.copyOf(result);
    }
    private IsbnLookupService.LookupException unavailable() {
        return new IsbnLookupService.LookupException("Não foi possível consultar a Open Library agora. Tente novamente depois ou continue o cadastro manualmente.");
    }
}
