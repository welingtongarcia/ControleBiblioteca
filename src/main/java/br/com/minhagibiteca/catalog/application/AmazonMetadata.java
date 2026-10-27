package br.com.minhagibiteca.catalog.application;

import java.util.List;

public record AmazonMetadata(String isbn, String title, Integer year, Integer pages, String format,
        List<String> publishers, List<String> authors, String summary, String coverUrl, String sourceName, String sourceUrl) {
    public AmazonMetadata { publishers=List.copyOf(publishers); authors=List.copyOf(authors); }
}
