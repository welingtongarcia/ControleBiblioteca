package br.com.minhagibiteca.collection.application;
import br.com.minhagibiteca.catalog.application.ComicSeriesService;
import br.com.minhagibiteca.collection.domain.ChecklistItem;
import br.com.minhagibiteca.collection.infrastructure.*;
import br.com.minhagibiteca.shared.application.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChecklistService {
    private final ChecklistRepository repository;
    private final ChecklistQueries queries;
    private final ComicSeriesService series;
    private final JdbcTemplate jdbc;
    public ChecklistService(ChecklistRepository repository, ChecklistQueries queries, ComicSeriesService series, JdbcTemplate jdbc) {
        this.repository = repository; this.queries = queries; this.series = series; this.jdbc = jdbc;
    }
    @Transactional(readOnly = true) public ChecklistView find(UUID id, UUID owner) { return queries.find(id, owner); }
    @Transactional(readOnly = true) public Page<ChecklistView> list(UUID seriesId, UUID owner, boolean missing, int page) {
        series.viewOwned(seriesId, owner); return queries.list(seriesId, owner, missing, page);
    }
    @Transactional public UUID create(UUID seriesId, UUID owner, Command command) {
        lock(owner); series.viewOwned(seriesId, owner); validate(owner, seriesId, command, null);
        return repository.saveAndFlush(ChecklistItem.create(owner, seriesId, command.number(), command.label(), command.owned(), command.notes())).getId();
    }
    @Transactional public UUID update(UUID id, UUID owner, Command command) {
        lock(owner); var item = owned(id, owner); validate(owner, item.getSeriesId(), command, item);
        item.update(command.number(), command.label(), command.owned(), command.notes()); repository.flush(); return item.getSeriesId();
    }
    @Transactional public UUID delete(UUID id, UUID owner) {
        lock(owner); var item = owned(id, owner); UUID seriesId = item.getSeriesId(); repository.delete(item); repository.flush(); return seriesId;
    }
    private ChecklistItem owned(UUID id, UUID owner) { return repository.findByIdAndOwnerUserId(id, owner).orElseThrow(ResourceNotFoundException::new); }
    private void lock(UUID owner) { if (jdbc.queryForList("select id from users where id = ? for update", UUID.class, owner).isEmpty()) throw new ResourceNotFoundException(); }
    private void validate(UUID owner, UUID seriesId, Command command, ChecklistItem current) {
        String normalized;
        try { normalized = ChecklistItem.normalize(command.number()); }
        catch (IllegalArgumentException error) { throw new RuleViolation("editorialNumber", error.getMessage()); }
        if ((current == null || !current.getNormalizedNumber().equals(normalized)) && repository.existsByOwnerUserIdAndSeriesIdAndNormalizedNumber(owner, seriesId, normalized))
            throw new RuleViolation("editorialNumber", "Já existe um item com este número editorial nesta série.");
        if (command.label() != null && command.label().length() > 180) throw new RuleViolation("label", "Informe até 180 caracteres.");
        if (command.notes() != null && command.notes().length() > 2000) throw new RuleViolation("notes", "Informe até 2000 caracteres.");
    }
    public record Command(String number, String label, boolean owned, String notes) { }
    public static class RuleViolation extends RuntimeException {
        private final String field;
        public RuleViolation(String field, String message) { super(message); this.field = field; }
        public String field() { return field; }
    }
}
