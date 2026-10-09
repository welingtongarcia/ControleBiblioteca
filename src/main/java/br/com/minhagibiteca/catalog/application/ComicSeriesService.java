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
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    public ComicSeriesService(ComicSeriesRepository repository, PublisherService publisherService, org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.repository = repository;
        this.publisherService = publisherService;
        this.jdbc = jdbc;
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
    public void delete(UUID id, UUID ownerUserId) {
        if (jdbc.queryForList("select id from users where id = ? for update", UUID.class, ownerUserId).isEmpty()) throw new ResourceNotFoundException();
        var item = findOwned(id, ownerUserId);
        if (Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from series_checklist_items where series_id = ? and owner_user_id = ?)", Boolean.class, id, ownerUserId)))
            throw new ReferencedSeriesException("Esta série possui itens de checklist. Remova os itens antes de excluí-la.");
        if (Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from editions where series_id = ? and owner_user_id = ?)", Boolean.class, id, ownerUserId)))
            throw new ReferencedSeriesException("Esta série possui edições. Desassocie as edições antes de excluí-la.");
        repository.delete(item); repository.flush();
    }

    @Transactional(readOnly = true) public View viewOwned(UUID id, UUID owner) {
        var item = findOwned(id, owner); return new View(item.getId(), item.getName());
    }
    public record View(UUID id, String name) { }
    public static class ReferencedSeriesException extends RuntimeException { public ReferencedSeriesException(String message) { super(message); } }

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
