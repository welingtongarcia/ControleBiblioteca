package br.com.minhagibiteca.catalog.application;

import java.util.List;

public record MetadataCandidate(String id, String isbn, String title, Integer year,
        List<String> publishers, List<String> authors, Source source) {
    public enum Source {
        OPEN_LIBRARY("Open Library"), GOOGLE_BOOKS("Google Books (exige chave configurada)");
        private final String label;
        Source(String label) { this.label = label; }
        public String label() { return label; }
    }
    public MetadataCandidate {
        if (source == null || id == null || !(source == Source.OPEN_LIBRARY
                ? id.matches("OL[0-9]+M") : id.matches("[A-Za-z0-9_-]{1,100}")))
            throw new IllegalArgumentException("Identificador externo inválido.");
        publishers = List.copyOf(publishers); authors = List.copyOf(authors);
    }
    public String sourceName() { return source == Source.OPEN_LIBRARY ? "Open Library" : "Google Books"; }
    public String sourceUrl() { return source == Source.OPEN_LIBRARY
            ? "https://openlibrary.org/books/" + id : "https://books.google.com/books?id=" + id; }
}
