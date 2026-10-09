package br.com.minhagibiteca.catalog.application;

import java.util.List;

public record IsbnMetadata(String isbn, String title, Integer year, List<String> publishers, List<String> authors, boolean coverAvailable, Source source) {
    public enum Source { OPEN_LIBRARY, GOOGLE_BOOKS }
    public IsbnMetadata(String isbn, String title, Integer year, List<String> publishers, List<String> authors, boolean coverAvailable) {
        this(isbn, title, year, publishers, authors, coverAvailable, Source.OPEN_LIBRARY);
    }
    public String sourceName() { return source == Source.GOOGLE_BOOKS ? "Google Books" : "Open Library"; }
    public String sourceUrl() {
        String safeIsbn = IsbnLookupService.normalize(isbn);
        return source == Source.GOOGLE_BOOKS ? "https://books.google.com/books?vid=ISBN" + safeIsbn : "https://openlibrary.org/isbn/" + safeIsbn;
    }
}
