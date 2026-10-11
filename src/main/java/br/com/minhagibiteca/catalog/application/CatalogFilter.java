package br.com.minhagibiteca.catalog.application;

import br.com.minhagibiteca.catalog.domain.MediaType;
import java.util.UUID;

public record CatalogFilter(String query, UUID publisherId, UUID seriesId, String format, MediaType mediaType, br.com.minhagibiteca.catalog.domain.PublicationType publicationType) {
    public CatalogFilter(String query, UUID publisherId, UUID seriesId, String format, MediaType mediaType) { this(query,publisherId,seriesId,format,mediaType,null); }
    public CatalogFilter {
        query = query == null ? "" : query.trim();
        format = format == null ? "" : format.trim();
    }
}
