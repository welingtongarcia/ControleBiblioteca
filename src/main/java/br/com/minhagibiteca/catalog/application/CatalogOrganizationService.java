package br.com.minhagibiteca.catalog.application;

import br.com.minhagibiteca.catalog.domain.MediaType;
import br.com.minhagibiteca.catalog.domain.PublicationType;
import br.com.minhagibiteca.shared.application.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogOrganizationService {
    public record SavedFilter(UUID id, String name, CatalogFilter filter, CatalogOrder order, boolean available) {}
    public record Pending(CatalogMissing kind, long count) {}
    private final JdbcTemplate jdbc;

    public CatalogOrganizationService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(readOnly = true)
    public List<Pending> pending(UUID owner) {
        return jdbc.query("""
                select count(*) filter(where cover_file_name is null) as cover,
                       count(*) filter(where total_pages is null) as pages,
                       count(*) filter(where publisher_id is null) as publisher,
                       count(*) filter(where series_id is null) as series,
                       count(*) filter(where reference_paid_price is null) as paid
                from editions where owner_user_id = ?
                """, (row, index) -> List.of(
                new Pending(CatalogMissing.COVER, row.getLong("cover")),
                new Pending(CatalogMissing.PAGES, row.getLong("pages")),
                new Pending(CatalogMissing.PUBLISHER, row.getLong("publisher")),
                new Pending(CatalogMissing.SERIES, row.getLong("series")),
                new Pending(CatalogMissing.PAID, row.getLong("paid"))), owner).getFirst();
    }

    @Transactional(readOnly = true)
    public List<SavedFilter> saved(UUID owner) {
        return jdbc.query("""
                select f.*,
                    (publisher_id is null or exists(select 1 from publishers p where p.id = f.publisher_id and p.owner_user_id = f.owner_user_id))
                    and (series_id is null or exists(select 1 from series s where s.id = f.series_id and s.owner_user_id = f.owner_user_id)) as available
                from saved_catalog_filters f where owner_user_id = ? order by lower(name), id limit 50
                """, (row, index) -> new SavedFilter(row.getObject("id", UUID.class), row.getString("name"),
                new CatalogFilter(row.getString("query"), row.getObject("publisher_id", UUID.class),
                    row.getObject("series_id", UUID.class), row.getString("format"),
                    row.getString("media_type") == null ? null : MediaType.valueOf(row.getString("media_type")),
                    row.getString("publication_type") == null ? null : PublicationType.valueOf(row.getString("publication_type")),
                    CatalogReading.valueOf(row.getString("reading")), CatalogMissing.valueOf(row.getString("missing"))),
                CatalogOrder.valueOf(row.getString("catalog_order")), row.getBoolean("available")), owner);
    }

    @Transactional
    public void save(UUID owner, String name, CatalogFilter filter, CatalogOrder order) {
        if (name == null || name.isBlank() || name.strip().length() > 80
                || filter.query().length() > 240 || filter.format().length() > 60) {
            throw new IllegalArgumentException("Informe um nome de até 80 caracteres e filtros válidos.");
        }
        if (jdbc.queryForList("select id from users where id = ? for update", UUID.class, owner).isEmpty()) {
            throw new ResourceNotFoundException();
        }
        assertReference(owner, filter.publisherId(), "publishers");
        assertReference(owner, filter.seriesId(), "series");
        if (jdbc.queryForObject("select count(*) from saved_catalog_filters where owner_user_id = ?", Long.class, owner) >= 50) {
            throw new IllegalArgumentException("Você pode salvar até 50 filtros. Remova um filtro para adicionar outro.");
        }
        if (Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists(select 1 from saved_catalog_filters where owner_user_id = ? and lower(name) = lower(?))",
                Boolean.class, owner, name.strip()))) {
            throw new IllegalArgumentException("Já existe um filtro salvo com este nome.");
        }
        jdbc.update("""
                insert into saved_catalog_filters(id, owner_user_id, name, query, publisher_id, series_id,
                    format, media_type, publication_type, reading, missing, catalog_order)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), owner, name.strip(), filter.query(), filter.publisherId(), filter.seriesId(),
                filter.format(), filter.mediaType() == null ? null : filter.mediaType().name(),
                filter.publicationType() == null ? null : filter.publicationType().name(),
                filter.reading().name(), filter.missing().name(), order.name());
    }

    @Transactional
    public void delete(UUID owner, UUID id) {
        if (jdbc.update("delete from saved_catalog_filters where id = ? and owner_user_id = ?", id, owner) == 0) {
            throw new ResourceNotFoundException();
        }
    }

    private void assertReference(UUID owner, UUID id, String table) {
        if (id != null && jdbc.queryForList("select id from " + table + " where id = ? and owner_user_id = ?",
                UUID.class, id, owner).isEmpty()) throw new ResourceNotFoundException();
    }
}
