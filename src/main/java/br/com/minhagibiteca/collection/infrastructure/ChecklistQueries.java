package br.com.minhagibiteca.collection.infrastructure;
import br.com.minhagibiteca.collection.application.ChecklistView;
import br.com.minhagibiteca.shared.application.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ChecklistQueries {
    private final JdbcTemplate jdbc;
    public ChecklistQueries(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    private ChecklistView map(java.sql.ResultSet row, int index) throws java.sql.SQLException {
        return new ChecklistView(row.getObject("id", UUID.class), row.getObject("series_id", UUID.class),
                row.getString("editorial_number"), row.getString("label"), row.getBoolean("owned"), row.getString("notes"));
    }
    public ChecklistView find(UUID id, UUID owner) {
        return jdbc.query("select * from series_checklist_items where id = ? and owner_user_id = ?", this::map, id, owner)
                .stream().findFirst().orElseThrow(ResourceNotFoundException::new);
    }
    public Page<ChecklistView> list(UUID series, UUID owner, boolean missing, int page) {
        var pageable = PageRequest.of(Math.max(0, page), 20);
        String where = " where series_id = ? and owner_user_id = ?" + (missing ? " and owned = false" : "");
        Long total = jdbc.queryForObject("select count(*) from series_checklist_items" + where, Long.class, series, owner);
        var rows = jdbc.query("select * from series_checklist_items" + where + " order by created_at, id limit 20 offset ?", this::map, series, owner, pageable.getOffset());
        return new PageImpl<>(rows, pageable, total);
    }
}
