package br.com.minhagibiteca.catalog.application;

import br.com.minhagibiteca.catalog.domain.MediaType;
import java.util.UUID;

public record CatalogFilter(String query, UUID publisherId, UUID seriesId, String format, MediaType mediaType,
        br.com.minhagibiteca.catalog.domain.PublicationType publicationType, CatalogReading reading, CatalogMissing missing) {
    public CatalogFilter(String query, UUID publisherId, UUID seriesId, String format, MediaType mediaType,
            br.com.minhagibiteca.catalog.domain.PublicationType publicationType) {
        this(query, publisherId, seriesId, format, mediaType, publicationType, CatalogReading.ALL, CatalogMissing.NONE);
    }
    public CatalogFilter(String query, UUID publisherId, UUID seriesId, String format, MediaType mediaType) { this(query,publisherId,seriesId,format,mediaType,null); }
    public CatalogFilter {
        query = query == null ? "" : query.trim();
        format = format == null ? "" : format.trim();
        reading = reading == null ? CatalogReading.ALL : reading;
        missing = missing == null ? CatalogMissing.NONE : missing;
    }
}
