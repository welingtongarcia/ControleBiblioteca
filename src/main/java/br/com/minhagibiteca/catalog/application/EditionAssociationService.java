package br.com.minhagibiteca.catalog.application;

import br.com.minhagibiteca.catalog.domain.Creator;
import br.com.minhagibiteca.catalog.domain.Work;
import br.com.minhagibiteca.catalog.domain.CreatorFunction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EditionAssociationService {
    private final EditionService editions; private final WorkService works; private final CreatorService creators; private final JdbcTemplate jdbc;
    public EditionAssociationService(EditionService editions, WorkService works, CreatorService creators, JdbcTemplate jdbc) { this.editions = editions; this.works = works; this.creators = creators; this.jdbc = jdbc; }
    @Transactional
    public void replace(UUID editionId, UUID ownerId, List<UUID> workIds, List<UUID> creatorIds) {
        replace(editionId, ownerId, workIds, creatorIds, null);
    }

    @Transactional
    public void replace(UUID editionId, UUID ownerId, List<UUID> workIds, List<UUID> creatorIds, List<String> creditTokens) {
        editions.findOwned(editionId, ownerId);
        List<UUID> uniqueWorks = unique(workIds); List<UUID> uniqueCreators = unique(creatorIds);
        uniqueWorks.forEach(id -> works.findOwned(id, ownerId)); uniqueCreators.forEach(id -> creators.findOwned(id, ownerId));
        Map<UUID, List<String>> roles = creditTokens == null ? existingRoles(editionId) : parseCredits(uniqueCreators, creditTokens);
        jdbc.update("delete from edition_works where edition_id = ?", editionId); jdbc.update("delete from edition_creators where edition_id = ?", editionId);
        for (int i = 0; i < uniqueWorks.size(); i++) jdbc.update("insert into edition_works (edition_id, work_id, sequence) values (?, ?, ?)", editionId, uniqueWorks.get(i), i + 1);
        for (int i = 0; i < uniqueCreators.size(); i++) {
            UUID creator = uniqueCreators.get(i);
            for (String role : roles.getOrDefault(creator, List.of("NAO_INFORMADO"))) {
                jdbc.update("insert into edition_creators (edition_id, creator_id, creator_role, sequence) values (?, ?, ?, ?)",
                        editionId, creator, role, i + 1);
            }
        }
    }
    @Transactional(readOnly = true) public List<UUID> workIds(UUID editionId) { return jdbc.queryForList("select work_id from edition_works where edition_id = ? order by sequence nulls last", UUID.class, editionId); }
    @Transactional(readOnly = true) public List<UUID> creatorIds(UUID editionId) {
        return jdbc.queryForList("select creator_id from edition_creators where edition_id = ? group by creator_id order by min(sequence) nulls last, creator_id", UUID.class, editionId);
    }
    @Transactional(readOnly = true)
    public List<String> creatorCredits(UUID editionId, UUID owner) {
        editions.findOwned(editionId, owner);
        return jdbc.query("""
                select ec.creator_id, ec.creator_role from edition_creators ec
                join creators c on c.id = ec.creator_id and c.owner_user_id = ?
                where ec.edition_id = ? and ec.creator_role <> 'NAO_INFORMADO'
                order by ec.sequence nulls last, ec.creator_id, ec.creator_role
                """, (row, index) -> row.getObject("creator_id", UUID.class) + ":" + row.getString("creator_role"), owner, editionId);
    }

    private Map<UUID, List<String>> existingRoles(UUID edition) {
        var roles = new HashMap<UUID, List<String>>();
        jdbc.query("select creator_id, creator_role from edition_creators where edition_id = ?", row -> {
            roles.computeIfAbsent(row.getObject("creator_id", UUID.class), key -> new ArrayList<>()).add(row.getString("creator_role"));
        }, edition);
        return roles;
    }

    private Map<UUID, List<String>> parseCredits(List<UUID> selected, List<String> tokens) {
        if (tokens.size() > 1000) throw new InvalidCreditsException();
        var roles = new HashMap<UUID, List<String>>();
        try {
            for (String token : tokens) {
                String[] parts = token.split(":", 2);
                UUID creator = UUID.fromString(parts[0]);
                if (parts.length != 2 || !creator.toString().equalsIgnoreCase(parts[0]) || !selected.contains(creator)) {
                    throw new InvalidCreditsException();
                }
                String role = CreatorFunction.valueOf(parts[1]).name();
                var creatorRoles = roles.computeIfAbsent(creator, key -> new ArrayList<>());
                if (!creatorRoles.contains(role)) creatorRoles.add(role);
            }
        } catch (IllegalArgumentException | NullPointerException error) {
            throw new InvalidCreditsException();
        }
        return roles;
    }

    public static class InvalidCreditsException extends IllegalArgumentException {
        public InvalidCreditsException() { super("Selecione funções válidas apenas para os criadores vinculados à edição."); }
    }
    @Transactional(readOnly = true) public List<Work> works(UUID editionId, UUID ownerId) { return workIds(editionId).stream().map(id -> works.findOwned(id, ownerId)).toList(); }
    @Transactional(readOnly = true) public List<Creator> creators(UUID editionId, UUID ownerId) { return creatorIds(editionId).stream().map(id -> creators.findOwned(id, ownerId)).toList(); }
    private List<UUID> unique(List<UUID> ids) { return ids == null ? List.of() : new java.util.ArrayList<>(new LinkedHashSet<>(ids)); }
}
