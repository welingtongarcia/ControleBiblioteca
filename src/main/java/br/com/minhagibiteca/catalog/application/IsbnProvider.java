package br.com.minhagibiteca.catalog.application;

import java.util.Optional;

public interface IsbnProvider {
    Optional<IsbnMetadata> lookup(String normalizedIsbn);
}
