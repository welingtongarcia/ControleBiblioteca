package br.com.minhagibiteca.inventory.application;

import br.com.minhagibiteca.inventory.domain.*;
import br.com.minhagibiteca.inventory.infrastructure.*;
import br.com.minhagibiteca.shared.application.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocationService {
    private final StorageLocationRepository locations;
    private final LocationQueries queries;
    private final JdbcTemplate jdbc;

    public LocationService(StorageLocationRepository locations, LocationQueries queries, JdbcTemplate jdbc) {
        this.locations = locations; this.queries = queries; this.jdbc = jdbc;
    }
    @Transactional(readOnly = true) public Page<LocationView> list(UUID owner, String q, int page) { return queries.list(owner, q == null ? "" : q, page); }
    @Transactional(readOnly = true) public LocationView find(UUID id, UUID owner) { return queries.find(id, owner); }
    @Transactional(readOnly = true) public List<LocationView> options(UUID owner, UUID excludedId) {
        return queries.options(owner).stream().filter(l -> !l.id().equals(excludedId)).toList();
    }
    @Transactional public UUID create(UUID owner, Command command) {
        lockOwner(owner); validate(null, owner, command);
        return locations.saveAndFlush(StorageLocation.create(owner, command.parentId(), command.level(), command.name(), command.notes())).getId();
    }
    @Transactional public void update(UUID id, UUID owner, Command command) {
        lockOwner(owner); var location = owned(id, owner); validate(id, owner, command);
        location.update(command.parentId(), command.level(), command.name(), command.notes()); locations.flush();
    }
    @Transactional public void delete(UUID id, UUID owner) {
        lockOwner(owner); var location = owned(id, owner);
        if (locations.existsByParentIdAndOwnerUserId(id, owner)) throw new RuleViolation("parentId", "Esta localização contém outras localizações. Mova ou exclua as localizações internas antes de continuar.");
        locations.delete(location); locations.flush();
    }
    private StorageLocation owned(UUID id, UUID owner) { return locations.findByIdAndOwnerUserId(id, owner).orElseThrow(ResourceNotFoundException::new); }
    private void lockOwner(UUID owner) {
        if (jdbc.queryForList("select id from users where id = ? for update", UUID.class, owner).isEmpty()) throw new ResourceNotFoundException();
    }
    private void validate(UUID id, UUID owner, Command command) {
        if (command.level() == null) throw new RuleViolation("level", "Selecione o tipo de localização.");
        if (command.name() == null || command.name().isBlank() || command.name().trim().length() > 120) throw new RuleViolation("name", "Informe um nome com até 120 caracteres.");
        if (command.notes() != null && command.notes().length() > 2000) throw new RuleViolation("notes", "Informe até 2000 caracteres nas observações.");
        if (command.parentId() != null) {
            var parent = owned(command.parentId(), owner);
            UUID ancestor = parent.getId();
            for (int depth = 0; ancestor != null; depth++) {
                if (ancestor.equals(id) || depth >= 4) throw new RuleViolation("parentId", "Uma localização não pode ficar dentro de si mesma ou de suas localizações internas.");
                ancestor = owned(ancestor, owner).getParentId();
            }
            if (!parent.getLevel().canContain(command.level())) throw new RuleViolation("parentId", "O tipo da localização superior deve vir antes: cômodo, estante, prateleira e caixa.");
        }
        if (id != null && locations.findByParentIdAndOwnerUserId(id, owner).stream().anyMatch(child -> !command.level().canContain(child.getLevel()))) {
            throw new RuleViolation("level", "Este tipo não permite as localizações internas existentes. Realoque-as antes de alterar o tipo.");
        }
        if (locations.findByOwnerUserIdAndParentIdAndNormalizedName(owner, command.parentId(), StorageLocation.normalize(command.name()))
                .stream().anyMatch(l -> !l.getId().equals(id))) throw new RuleViolation("name", "Já existe uma localização com este nome dentro da mesma localização superior.");
    }
    public record Command(UUID parentId, LocationLevel level, String name, String notes) { }
    public static class RuleViolation extends RuntimeException {
        private final String field;
        public RuleViolation(String field, String message) { super(message); this.field = field; }
        public String field() { return field; }
    }
}
