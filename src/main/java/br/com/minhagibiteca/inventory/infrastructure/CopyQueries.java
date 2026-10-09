package br.com.minhagibiteca.inventory.infrastructure;

import br.com.minhagibiteca.inventory.application.CopyView;
import br.com.minhagibiteca.inventory.domain.CopyCondition;
import br.com.minhagibiteca.shared.application.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CopyQueries {
    private static final String SELECT = """
            select c.*, concat_ws(' › ', p3.name, p2.name, p1.name, l.name) as location_path
            from physical_copies c
            left join storage_locations l on l.id = c.storage_location_id and l.owner_user_id = c.owner_user_id
            left join storage_locations p1 on p1.id = l.parent_id and p1.owner_user_id = c.owner_user_id
            left join storage_locations p2 on p2.id = p1.parent_id and p2.owner_user_id = c.owner_user_id
            left join storage_locations p3 on p3.id = p2.parent_id and p3.owner_user_id = c.owner_user_id
            """;
    private final JdbcTemplate jdbc;
    public CopyQueries(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    private CopyView map(java.sql.ResultSet r, int i) throws java.sql.SQLException {
        String path = r.getString("location_path");
        return new CopyView(r.getObject("id", UUID.class), r.getObject("edition_id", UUID.class), r.getObject("storage_location_id", UUID.class),
                CopyCondition.valueOf(r.getString("condition")), r.getString("condition_notes"), r.getString("acquisition_source"),
                r.getObject("acquisition_date", java.time.LocalDate.class), r.getBigDecimal("acquisition_price"), r.getString("notes"), path.isBlank() ? "Não informada" : path);
    }
    public CopyView find(UUID id, UUID owner) {
        return jdbc.query(SELECT + " where c.id = ? and c.owner_user_id = ?", this::map, id, owner).stream().findFirst().orElseThrow(ResourceNotFoundException::new);
    }
    public Page<CopyView> list(UUID edition, UUID owner, int page) {
        var pageable = PageRequest.of(Math.max(0, page), 20);
        var rows = jdbc.query(SELECT + " where c.edition_id = ? and c.owner_user_id = ? order by c.created_at, c.id limit 20 offset ?", this::map, edition, owner, pageable.getOffset());
        Long count = jdbc.queryForObject("select count(*) from physical_copies where edition_id = ? and owner_user_id = ?", Long.class, edition, owner);
        return new PageImpl<>(rows, pageable, count);
    }
}
