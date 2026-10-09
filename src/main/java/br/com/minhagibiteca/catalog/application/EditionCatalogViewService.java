package br.com.minhagibiteca.catalog.application;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EditionCatalogViewService {
    private final PublisherService publishers;
    private final ComicSeriesService series;
    private final WorkService works;
    private final CreatorService creators;
    private final EditionAssociationService associations;

    public EditionCatalogViewService(PublisherService publishers, ComicSeriesService series,
            WorkService works, CreatorService creators, EditionAssociationService associations) {
        this.publishers = publishers; this.series = series; this.works = works;
        this.creators = creators; this.associations = associations;
    }

    public List<NamedOption> publishers(UUID owner) {
        return publishers.options(owner).stream().map(p -> new NamedOption(p.getId(), p.getName())).toList();
    }
    public Page<NamedOption> series(UUID owner) {
        return series.list(owner, "", PageRequest.of(0, 200, Sort.by("normalizedName")))
                .map(s -> new NamedOption(s.getId(), s.getName()));
    }
    public List<WorkOption> works(UUID owner) {
        return works.options(owner).stream().map(w -> new WorkOption(w.getId(), w.getTitle())).toList();
    }
    public List<NamedOption> creators(UUID owner) {
        return creators.options(owner).stream().map(c -> new NamedOption(c.getId(), c.getName())).toList();
    }
    public List<WorkOption> editionWorks(UUID id, UUID owner) {
        return associations.works(id, owner).stream().map(w -> new WorkOption(w.getId(), w.getTitle())).toList();
    }
    public List<NamedOption> editionCreators(UUID id, UUID owner) {
        return associations.creators(id, owner).stream().map(c -> new NamedOption(c.getId(), c.getName())).toList();
    }
    public record NamedOption(UUID id, String name) { }
    public record WorkOption(UUID id, String title) { }
}
