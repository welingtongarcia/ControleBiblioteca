package br.com.minhagibiteca.catalog.application;

import br.com.minhagibiteca.catalog.domain.ComicSeries;
import br.com.minhagibiteca.catalog.domain.SeriesStatus;
import br.com.minhagibiteca.catalog.infrastructure.ComicSeriesRepository;
import br.com.minhagibiteca.shared.application.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComicSeriesService {
    private final ComicSeriesRepository repository;
    private final PublisherService publisherService;

    public ComicSeriesService(ComicSeriesRepository repository, PublisherService publisherService) {
        this.repository = repository;
        this.publisherService = publisherService;
    }

    @Transactional(readOnly = true)
    public Page<ComicSeries> list(UUID ownerUserId, String query, Pageable pageable) {
        return repository.findByOwnerUserIdAndNormalizedNameContaining(ownerUserId,
                query == null ? "" : query.trim().toLowerCase(), pageable);
    }

    @Transactional(readOnly = true)
    public ComicSeries findOwned(UUID id, UUID ownerUserId) {
        return repository.findByIdAndOwnerUserId(id, ownerUserId).orElseThrow(ResourceNotFoundException::new);
    }

    @Transactional
    public ComicSeries create(UUID ownerUserId, Command command) {
        validatePublisher(ownerUserId, command.publisherId());
        ensureUnique(ownerUserId, command.name(), null);
        return repository.save(ComicSeries.create(ownerUserId, command.publisherId(), command.name(), command.startYear(),
                command.endYear(), command.status(), command.notes()));
    }

    @Transactional
    public void update(UUID id, UUID ownerUserId, Command command) {
        ComicSeries series = findOwned(id, ownerUserId);
        validatePublisher(ownerUserId, command.publisherId());
        ensureUnique(ownerUserId, command.name(), series);
        series.update(command.publisherId(), command.name(), command.startYear(), command.endYear(), command.status(), command.notes());
    }

    @Transactional
    public void delete(UUID id, UUID ownerUserId) { repository.delete(findOwned(id, ownerUserId)); }

    private void validatePublisher(UUID ownerUserId, UUID publisherId) {
        if (publisherId != null) publisherService.findOwned(publisherId, ownerUserId);
    }

    private void ensureUnique(UUID ownerUserId, String name, ComicSeries current) {
        String normalized = name.trim().toLowerCase();
        if ((current == null || !current.getName().equalsIgnoreCase(name))
                && repository.existsByOwnerUserIdAndNormalizedName(ownerUserId, normalized)) throw new DuplicateSeriesException();
    }

    public record Command(UUID publisherId, String name, Integer startYear, Integer endYear, SeriesStatus status, String notes) { }
    public static class DuplicateSeriesException extends RuntimeException { }
}
