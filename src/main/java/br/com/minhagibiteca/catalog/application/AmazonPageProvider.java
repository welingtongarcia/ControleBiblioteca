package br.com.minhagibiteca.catalog.application;

import java.util.Optional;

public interface AmazonPageProvider {
    Optional<AmazonMetadata> lookup(AmazonProductLink product);
}
