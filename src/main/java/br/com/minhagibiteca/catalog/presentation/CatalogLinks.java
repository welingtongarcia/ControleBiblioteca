package br.com.minhagibiteca.catalog.presentation;

import br.com.minhagibiteca.catalog.application.*;
import org.springframework.web.util.UriComponentsBuilder;

public final class CatalogLinks {
    private CatalogLinks() {}

    public static String url(CatalogFilter filter, CatalogOrder order) {
        return UriComponentsBuilder.fromPath("/edicoes")
                .queryParam("q", filter.query()).queryParam("publisherId", filter.publisherId() == null ? "" : filter.publisherId())
                .queryParam("seriesId", filter.seriesId() == null ? "" : filter.seriesId())
                .queryParam("format", filter.format()).queryParam("mediaType", filter.mediaType() == null ? "" : filter.mediaType())
                .queryParam("publicationType", filter.publicationType() == null ? "" : filter.publicationType())
                .queryParam("reading", filter.reading()).queryParam("missing", filter.missing())
                .queryParam("order", order).build().encode().toUriString();
    }
}
