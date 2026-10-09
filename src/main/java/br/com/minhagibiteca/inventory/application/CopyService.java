package br.com.minhagibiteca.inventory.application;

import br.com.minhagibiteca.catalog.application.EditionService;
import br.com.minhagibiteca.catalog.domain.MediaType;
import br.com.minhagibiteca.inventory.domain.*;
import br.com.minhagibiteca.inventory.infrastructure.*;
import br.com.minhagibiteca.shared.application.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CopyService {
    private final PhysicalCopyRepository copies;
    private final CopyQueries queries;
    private final EditionService editions;
    private final LocationService locations;
    private final JdbcTemplate jdbc;
    public CopyService(PhysicalCopyRepository copies, CopyQueries queries, EditionService editions, LocationService locations, JdbcTemplate jdbc) {
        this.copies = copies; this.queries = queries; this.editions = editions; this.locations = locations; this.jdbc = jdbc;
    }
    @Transactional(readOnly = true) public CopyView find(UUID id, UUID owner) { return queries.find(id, owner); }
    @Transactional(readOnly = true) public Page<CopyView> list(UUID edition, UUID owner, int page) { editions.viewOwned(edition, owner); return queries.list(edition, owner, page); }
    @Transactional public UUID create(UUID edition, UUID owner, Command command) {
        lock(owner); validateEdition(edition, owner); validate(owner, command);
        return copies.saveAndFlush(PhysicalCopy.create(owner, edition, command.locationId(), command.condition(), command.conditionNotes(), command.source(), command.date(), command.price(), command.notes())).getId();
    }
    @Transactional public UUID update(UUID id, UUID owner, Command command) {
        lock(owner); var copy = copies.findByIdAndOwnerUserId(id, owner).orElseThrow(ResourceNotFoundException::new);
        validateEdition(copy.getEditionId(), owner); validate(owner, command);
        copy.update(command.locationId(), command.condition(), command.conditionNotes(), command.source(), command.date(), command.price(), command.notes()); copies.flush(); return copy.getEditionId();
    }
    @Transactional public UUID delete(UUID id, UUID owner) {
        lock(owner); var copy = copies.findByIdAndOwnerUserId(id, owner).orElseThrow(ResourceNotFoundException::new);
        UUID edition = copy.getEditionId(); copies.delete(copy); copies.flush(); return edition;
    }
    @Transactional(readOnly = true) public void validateEdition(UUID edition, UUID owner) {
        if (editions.viewOwned(edition, owner).mediaType() == MediaType.DIGITAL) throw new RuleViolation("edition", "Uma edição digital não possui exemplares físicos.");
    }
    private void lock(UUID owner) { if (jdbc.queryForList("select id from users where id = ? for update", UUID.class, owner).isEmpty()) throw new ResourceNotFoundException(); }
    private void validate(UUID owner, Command c) {
        if (c.locationId() != null) locations.find(c.locationId(), owner);
        if (c.condition() == null) throw new RuleViolation("condition", "Selecione a conservação.");
        if (c.price() != null && (c.price().signum() < 0 || c.price().scale() > 2 || c.price().compareTo(new BigDecimal("9999999999.99")) > 0)) throw new RuleViolation("acquisitionPrice", "Informe um valor não negativo com até duas casas decimais.");
        if (c.date() != null && c.date().isAfter(LocalDate.now(ZoneId.of("America/Sao_Paulo")))) throw new RuleViolation("acquisitionDate", "A data de aquisição não pode ser futura.");
        if (c.source() != null && c.source().length() > 160) throw new RuleViolation("acquisitionSource", "Informe até 160 caracteres.");
        if (c.conditionNotes() != null && c.conditionNotes().length() > 2000) throw new RuleViolation("conditionNotes", "Informe até 2000 caracteres.");
        if (c.notes() != null && c.notes().length() > 2000) throw new RuleViolation("notes", "Informe até 2000 caracteres.");
    }
    public record Command(UUID locationId, CopyCondition condition, String conditionNotes, String source, LocalDate date, BigDecimal price, String notes) { }
    public static class RuleViolation extends RuntimeException {
        private final String field;
        public RuleViolation(String field, String message) { super(message); this.field = field; }
        public String field() { return field; }
    }
}
