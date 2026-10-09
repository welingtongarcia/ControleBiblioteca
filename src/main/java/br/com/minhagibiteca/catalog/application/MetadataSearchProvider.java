package br.com.minhagibiteca.catalog.application;

import java.util.List;

public interface MetadataSearchProvider {
    List<MetadataCandidate> search(MetadataCandidate.Source source, String title);
}
