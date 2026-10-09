package br.com.minhagibiteca.catalog.application;

import br.com.minhagibiteca.catalog.domain.Creator;
import br.com.minhagibiteca.catalog.domain.Work;
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
        editions.findOwned(editionId, ownerId);
        List<UUID> uniqueWorks = unique(workIds); List<UUID> uniqueCreators = unique(creatorIds);
        uniqueWorks.forEach(id -> works.findOwned(id, ownerId)); uniqueCreators.forEach(id -> creators.findOwned(id, ownerId));
        jdbc.update("delete from edition_works where edition_id = ?", editionId); jdbc.update("delete from edition_creators where edition_id = ?", editionId);
        for (int i = 0; i < uniqueWorks.size(); i++) jdbc.update("insert into edition_works (edition_id, work_id, sequence) values (?, ?, ?)", editionId, uniqueWorks.get(i), i + 1);
        for (int i = 0; i < uniqueCreators.size(); i++) jdbc.update("insert into edition_creators (edition_id, creator_id, creator_role, sequence) values (?, ?, 'OUTRO', ?)", editionId, uniqueCreators.get(i), i + 1);
    }
    @Transactional(readOnly = true) public List<UUID> workIds(UUID editionId) { return jdbc.queryForList("select work_id from edition_works where edition_id = ? order by sequence nulls last", UUID.class, editionId); }
    @Transactional(readOnly = true) public List<UUID> creatorIds(UUID editionId) { return jdbc.queryForList("select creator_id from edition_creators where edition_id = ? order by sequence nulls last", UUID.class, editionId); }
    @Transactional(readOnly = true) public List<Work> works(UUID editionId, UUID ownerId) { return workIds(editionId).stream().map(id -> works.findOwned(id, ownerId)).toList(); }
    @Transactional(readOnly = true) public List<Creator> creators(UUID editionId, UUID ownerId) { return creatorIds(editionId).stream().map(id -> creators.findOwned(id, ownerId)).toList(); }
    private List<UUID> unique(List<UUID> ids) { return ids == null ? List.of() : new java.util.ArrayList<>(new LinkedHashSet<>(ids)); }
}
