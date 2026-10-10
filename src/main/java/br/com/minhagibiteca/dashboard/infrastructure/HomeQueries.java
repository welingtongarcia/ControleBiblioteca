package br.com.minhagibiteca.dashboard.infrastructure;

import br.com.minhagibiteca.catalog.domain.MediaType;
import br.com.minhagibiteca.dashboard.application.HomeEdition;
import br.com.minhagibiteca.dashboard.application.HomeShelf;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class HomeQueries {
    private final JdbcTemplate jdbc;
    public HomeQueries(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<HomeEdition> shelf(UUID owner, HomeShelf category) {
        // Predicados internos fechados; nenhum fragmento SQL vem da requisição.
        var predicate = switch (category) {
            case RECENT -> "";
            case UNREAD -> " and e.is_read = false";
            case READ -> " and e.is_read = true";
            case PHYSICAL -> " and e.media_type = 'PHYSICAL'";
            case DIGITAL -> " and e.media_type = 'DIGITAL'";
        };
        return jdbc.query("""
            select e.id, e.title, e.editorial_number, e.publication_year,
                s.name as series_name, p.name as publisher_name,
                e.cover_file_name is not null as has_cover, e.media_type, e.is_read
            from editions e
            left join series s on s.id = e.series_id and s.owner_user_id = e.owner_user_id
            left join publishers p on p.id = e.publisher_id and p.owner_user_id = e.owner_user_id
            where e.owner_user_id = ?
            """ + predicate + " order by e.created_at desc, e.id desc limit 12", (r, i) -> {
                var media = r.getString("media_type");
                return new HomeEdition(r.getObject("id", UUID.class), r.getString("title"),
                        r.getString("editorial_number"), r.getObject("publication_year", Integer.class),
                        r.getString("series_name"), r.getString("publisher_name"), r.getBoolean("has_cover"),
                        media == null ? null : MediaType.valueOf(media), r.getBoolean("is_read"));
            }, owner);
    }
}
