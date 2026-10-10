package br.com.minhagibiteca.catalog.application;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EditionCatalogViewService {
    private final PublisherService publishers;
    private final ComicSeriesService series;
    private final WorkService works;
    private final CreatorService creators;
    private final EditionAssociationService associations;
    private final JdbcTemplate jdbc;

    public EditionCatalogViewService(PublisherService publishers, ComicSeriesService series,
            WorkService works, CreatorService creators, EditionAssociationService associations, JdbcTemplate jdbc) {
        this.publishers = publishers; this.series = series; this.works = works;
        this.creators = creators; this.associations = associations;
        this.jdbc = jdbc;
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
        return creators.options(owner).stream().map(c -> new NamedOption(c.getId(), c.getName(), c.getFunctionsDescription())).toList();
    }
    public List<WorkOption> editionWorks(UUID id, UUID owner) {
        return jdbc.query("""
                select w.id, w.title from edition_works ew
                join editions e on e.id = ew.edition_id
                join works w on w.id = ew.work_id
                where e.id = ? and e.owner_user_id = ? and w.owner_user_id = ?
                order by ew.sequence nulls last, w.id
                """, (row, index) -> new WorkOption(row.getObject("id", UUID.class), row.getString("title")), id, owner, owner);
    }
    public List<NamedOption> editionCreators(UUID id, UUID owner) {
        return jdbc.query("""
                select distinct c.id, c.name,
                    (select string_agg(cf.function, ',') from creator_functions cf where cf.creator_id = c.id) as functions
                from edition_creators ec
                join editions e on e.id = ec.edition_id
                join creators c on c.id = ec.creator_id
                where e.id = ? and e.owner_user_id = ? and c.owner_user_id = ?
                order by c.name, c.id
                """, (row, index) -> new NamedOption(row.getObject("id", UUID.class), row.getString("name"),
                        describeFunctions(row.getString("functions"))), id, owner, owner);
    }
    public List<NamedOption> seriesOptions(UUID owner) {
        return jdbc.query("select id, name from series where owner_user_id = ? order by normalized_name, id",
                (row, index) -> new NamedOption(row.getObject("id", UUID.class), row.getString("name")), owner);
    }
    public String publisherName(UUID id, UUID owner) { return id == null ? "Não informada" : publishers.findOwned(id, owner).getName(); }
    public String seriesName(UUID id, UUID owner) { return id == null ? "Não informada" : series.findOwned(id, owner).getName(); }
    public record NamedOption(UUID id, String name, String functionsDescription) {
        public NamedOption(UUID id, String name) { this(id, name, null); }
    }
    private static String describeFunctions(String functions) {
        return functions == null ? "Funções não informadas" : java.util.Arrays.stream(functions.split(","))
                .map(br.com.minhagibiteca.catalog.domain.CreatorFunction::valueOf).sorted()
                .map(br.com.minhagibiteca.catalog.domain.CreatorFunction::getLabel)
                .collect(java.util.stream.Collectors.joining(", "));
    }
    public record WorkOption(UUID id, String title) { }
}
