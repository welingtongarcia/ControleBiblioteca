package br.com.minhagibiteca.catalog.application;

import java.util.List;

public record IsbnMetadata(String isbn, String title, Integer year, List<String> publishers, List<String> authors, boolean coverAvailable) {
    public String sourceUrl() { return "https://openlibrary.org/isbn/" + isbn; }
}
