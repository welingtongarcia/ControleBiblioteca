package br.com.minhagibiteca.collection.application;

import br.com.minhagibiteca.catalog.application.ComicSeriesService;
import br.com.minhagibiteca.catalog.infrastructure.EditionRepository;
import br.com.minhagibiteca.collection.infrastructure.ReadingQueries;
import br.com.minhagibiteca.shared.application.ResourceNotFoundException;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReadingService {
    private final EditionRepository editions;
    private final ReadingQueries queries;
    private final ComicSeriesService series;
    private final JdbcTemplate jdbc;
    public ReadingService(EditionRepository editions, ReadingQueries queries, ComicSeriesService series, JdbcTemplate jdbc) {
        this.editions = editions; this.queries = queries; this.series = series; this.jdbc = jdbc;
    }
    @Transactional public void mark(UUID edition, UUID owner, boolean read) {
        if (jdbc.queryForList("select id from users where id = ? for update", UUID.class, owner).isEmpty()) throw new ResourceNotFoundException();
        editions.findByIdAndOwnerUserId(edition, owner).orElseThrow(ResourceNotFoundException::new).markRead(read);
        editions.flush();
    }
    @Transactional(readOnly = true) public ReadingProgress overall(UUID owner) { return queries.overall(owner); }
    @Transactional(readOnly = true) public void assertOwned(UUID edition, UUID owner) {
        editions.findByIdAndOwnerUserId(edition, owner).orElseThrow(ResourceNotFoundException::new);
    }
    @Transactional public void updatePages(UUID edition, UUID owner, int completed) {
        if (jdbc.queryForList("select id from users where id = ? for update", UUID.class, owner).isEmpty()) throw new ResourceNotFoundException();
        var target = editions.findByIdAndOwnerUserId(edition, owner).orElseThrow(ResourceNotFoundException::new);
        if (target.getTotalPages() == null) throw new IllegalArgumentException("Informe o total de páginas em Editar edição antes de registrar sua leitura.");
        target.updatePages(target.getTotalPages(), completed);
        editions.flush();
    }
    @Transactional(readOnly = true) public ReadingProgress series(UUID id, UUID owner) {
        series.viewOwned(id, owner); return queries.bySeries(owner, List.of(id)).get(id);
    }
    @Transactional(readOnly = true) public Map<UUID, ReadingProgress> seriesPage(UUID owner, List<UUID> ids) {
        return queries.bySeries(owner, ids);
    }
}
