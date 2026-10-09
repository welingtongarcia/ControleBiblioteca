package br.com.minhagibiteca.inventory.infrastructure;

import br.com.minhagibiteca.inventory.application.LocationView;
import br.com.minhagibiteca.inventory.domain.LocationLevel;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LocationQueries {
    private static final String SELECT = """
            select l.id, l.parent_id, l.level, l.name, l.notes,
                   concat_ws(' › ', p3.name, p2.name, p1.name, l.name) as path
            from storage_locations l
            left join storage_locations p1 on p1.id = l.parent_id and p1.owner_user_id = l.owner_user_id
            left join storage_locations p2 on p2.id = p1.parent_id and p2.owner_user_id = l.owner_user_id
            left join storage_locations p3 on p3.id = p2.parent_id and p3.owner_user_id = l.owner_user_id
            """;
    private final JdbcTemplate jdbc;
    public LocationQueries(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    private LocationView map(java.sql.ResultSet row, int index) throws java.sql.SQLException {
        return new LocationView(row.getObject("id", UUID.class), row.getObject("parent_id", UUID.class),
                LocationLevel.valueOf(row.getString("level")), row.getString("name"), row.getString("notes"), row.getString("path"));
    }
    public Page<LocationView> list(UUID owner, String query, int page) {
        String term = "%" + query.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        var pageable = PageRequest.of(Math.max(0, page), 20);
        var rows = jdbc.query(SELECT + " where l.owner_user_id = ? and l.normalized_name like ? escape '\\' order by l.normalized_name, l.id limit 20 offset ?",
                this::map, owner, term, pageable.getOffset());
        Long count = jdbc.queryForObject("select count(*) from storage_locations where owner_user_id = ? and normalized_name like ? escape '\\'", Long.class, owner, term);
        return new PageImpl<>(rows, pageable, count);
    }
    public List<LocationView> options(UUID owner) {
        return jdbc.query(SELECT + " where l.owner_user_id = ? order by path, l.id", this::map, owner);
    }
    public LocationView find(UUID id, UUID owner) {
        return jdbc.query(SELECT + " where l.id = ? and l.owner_user_id = ?", this::map, id, owner)
                .stream().findFirst().orElseThrow(br.com.minhagibiteca.shared.application.ResourceNotFoundException::new);
    }
}
