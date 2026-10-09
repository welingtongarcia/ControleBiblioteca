package br.com.minhagibiteca.catalog.application;

import br.com.minhagibiteca.catalog.domain.MediaType;
import java.util.UUID;

public record CatalogFilter(String query, UUID publisherId, UUID seriesId, String format, MediaType mediaType) {
    public CatalogFilter {
        query = query == null ? "" : query.trim();
        format = format == null ? "" : format.trim();
    }
}
