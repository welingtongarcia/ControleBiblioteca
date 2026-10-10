package br.com.minhagibiteca.collection.infrastructure;

import br.com.minhagibiteca.collection.application.ReadingProgress;
import java.util.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReadingQueries {
    private final NamedParameterJdbcTemplate jdbc;
    public ReadingQueries(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
    public ReadingProgress overall(UUID owner) {
        return jdbc.queryForObject("select count(*) as total, count(*) filter (where is_read) as read, coalesce(sum(total_pages),0) as total_pages, coalesce(sum(pages_read),0) as pages_read, count(total_pages) as known_pages from editions where owner_user_id = :owner",
                Map.of("owner", owner), (r, i) -> new ReadingProgress(r.getLong("total"), r.getLong("read"), r.getLong("total_pages"), r.getLong("pages_read"), r.getLong("known_pages")));
    }
    public Map<UUID, ReadingProgress> bySeries(UUID owner, List<UUID> ids) {
        Map<UUID, ReadingProgress> result = new HashMap<>();
        ids.forEach(id -> result.put(id, new ReadingProgress(0, 0)));
        if (ids.isEmpty()) return result;
        jdbc.query("""
                select series_id, count(*) as total, count(*) filter (where is_read) as read,
                    coalesce(sum(total_pages),0) as total_pages, coalesce(sum(pages_read),0) as pages_read, count(total_pages) as known_pages
                from editions where owner_user_id = :owner and series_id in (:ids) group by series_id
                """, Map.of("owner", owner, "ids", ids), r -> {
            result.put(r.getObject("series_id", UUID.class), new ReadingProgress(r.getLong("total"), r.getLong("read"), r.getLong("total_pages"), r.getLong("pages_read"), r.getLong("known_pages")));
        });
        return result;
    }
}
