package br.com.minhagibiteca.catalog.infrastructure;

import br.com.minhagibiteca.catalog.application.*;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class FreeMetadataSearchProvider implements MetadataSearchProvider {
    private final RestClient openLibrary;
    private final RestClient googleBooks;
    private final ObjectMapper mapper;
    private final String googleKey;

    public FreeMetadataSearchProvider(@Qualifier("isbnRestClient") RestClient openLibrary,
            @Qualifier("googleBooksRestClient") RestClient googleBooks, ObjectMapper mapper,
            @Value("${app.isbn.google-books-api-key:}") String googleKey) {
        this.openLibrary = openLibrary; this.googleBooks = googleBooks; this.mapper = mapper; this.googleKey = googleKey.strip();
    }
    @Override public List<MetadataCandidate> search(MetadataCandidate.Source source, String title) {
        String query = MetadataSearchService.normalize(title);
        if (source == null) throw new MetadataSearchService.SearchException("Escolha uma fonte para pesquisar.");
        if (source == MetadataCandidate.Source.GOOGLE_BOOKS && googleKey.isEmpty())
            throw new MetadataSearchService.SearchException("Google Books precisa de uma chave gratuita configurada pelo administrador. Use Open Library ou continue manualmente.");
        try {
            RestClient.RequestHeadersSpec<?> request = source == MetadataCandidate.Source.OPEN_LIBRARY
                    ? openLibrary.get().uri(b -> b.path("/search.json").queryParam("q", query).queryParam("limit", 5)
                        .queryParam("lang", "pt").queryParam("fields", "key,title,author_name,editions,editions.key,editions.title,editions.isbn,editions.publisher,editions.publish_date").build())
                    : googleBooks.get().uri(b -> b.path("/books/v1/volumes").queryParam("q", "intitle:" + query)
                        .queryParam("maxResults", 5).queryParam("langRestrict", "pt").queryParam("key", googleKey).build());
            return request.exchange((req, response) -> {
                if (!response.getStatusCode().is2xxSuccessful()) throw unavailable(source);
                byte[] bytes = response.getBody().readNBytes(256 * 1024 + 1);
                if (bytes.length > 256 * 1024) throw unavailable(source);
                JsonNode root = mapper.readTree(bytes);
                if (root == null || !root.isObject() || root.has("error")) throw unavailable(source);
                JsonNode items = root.get(source == MetadataCandidate.Source.OPEN_LIBRARY ? "docs" : "items");
                if (items == null && source == MetadataCandidate.Source.GOOGLE_BOOKS && root.path("totalItems").isIntegralNumber() && root.path("totalItems").asLong() == 0) return List.of();
                if (items == null || !items.isArray()) throw unavailable(source);
                List<MetadataCandidate> found = new ArrayList<>();
                Set<String> ids = new HashSet<>();
                for (JsonNode item : items) {
                    if (source == MetadataCandidate.Source.OPEN_LIBRARY) {
                        JsonNode editions = item.path("editions").path("docs");
                        if (!editions.isArray()) continue;
                        for (JsonNode edition : editions) add(found, ids, edition, item, source);
                    } else add(found, ids, item.path("volumeInfo"), item, source);
                    if (found.size() >= 5) break;
                }
                return List.copyOf(found);
            });
        } catch (MetadataSearchService.SearchException error) { throw error;
        } catch (Exception error) { throw unavailable(source); }
    }
    private void add(List<MetadataCandidate> found, Set<String> ids, JsonNode info, JsonNode parent, MetadataCandidate.Source source) {
        if (found.size() >= 5) return;
        String title = text(info.get("title"), 240);
        String id = text(parent.get("id"), 100);
        if (source == MetadataCandidate.Source.OPEN_LIBRARY) {
            String key = text(info.get("key"), 120);
            id = key != null && key.matches("/books/OL[0-9]+M") ? key.substring(7) : null;
        }
        if (title == null || id == null || !(source == MetadataCandidate.Source.OPEN_LIBRARY ? id.matches("OL[0-9]+M") : id.matches("[A-Za-z0-9_-]{1,100}")) || !ids.add(id)) return;
        String isbn = null;
        JsonNode identifiers = info.path(source == MetadataCandidate.Source.OPEN_LIBRARY ? "isbn" : "industryIdentifiers");
        if (identifiers.isArray()) for (JsonNode identifier : identifiers) {
            if (source == MetadataCandidate.Source.GOOGLE_BOOKS && !Set.of("ISBN_10", "ISBN_13").contains(identifier.path("type").asText())) continue;
            String value = text(source == MetadataCandidate.Source.OPEN_LIBRARY ? identifier : identifier.get("identifier"), 32);
            try { isbn = IsbnLookupService.normalize(value); break; } catch (IsbnLookupService.LookupException ignored) { }
        }
        String date = text(info.get("publishedDate"), 120);
        if (source == MetadataCandidate.Source.OPEN_LIBRARY) {
            List<String> dates = texts(info.get("publish_date")); date = dates.size() == 1 ? dates.getFirst() : null;
        }
        Integer year = null;
        if (date != null) {
            var match = java.util.regex.Pattern.compile("(?<![0-9])(18[0-9]{2}|19[0-9]{2}|20[0-9]{2}|2100)(?![0-9])").matcher(date);
            if (match.find()) year = Integer.valueOf(match.group());
        }
        String publisher = text(info.get("publisher"), 180);
        List<String> publishers = source == MetadataCandidate.Source.OPEN_LIBRARY ? texts(info.get("publisher")) : publisher == null ? List.of() : List.of(publisher);
        List<String> authors = texts(source == MetadataCandidate.Source.OPEN_LIBRARY ? parent.get("author_name") : info.get("authors"));
        found.add(new MetadataCandidate(id, isbn, title, year, publishers, authors, source));
    }
    private String text(JsonNode node, int max) {
        if (node == null || !node.isTextual()) return null;
        String value = node.asText().replaceAll("[\\p{Cntrl}]", " ").strip();
        return value.isEmpty() ? null : value.substring(0, Math.min(max, value.length()));
    }
    private List<String> texts(JsonNode array) {
        List<String> result = new ArrayList<>();
        if (array != null && array.isArray()) for (JsonNode item : array) {
            String value = text(item, 180);
            if (value != null && !result.contains(value)) result.add(value);
            if (result.size() == 10) break;
        }
        return List.copyOf(result);
    }
    private MetadataSearchService.SearchException unavailable(MetadataCandidate.Source source) {
        return new MetadataSearchService.SearchException("Não foi possível consultar " + (source == MetadataCandidate.Source.OPEN_LIBRARY ? "Open Library" : "Google Books") + ". Tente depois ou continue manualmente; isso não significa que a edição não existe.");
    }
}
