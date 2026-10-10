package br.com.minhagibiteca.reports.infrastructure;

import br.com.minhagibiteca.reports.application.ReportData.*;
import br.com.minhagibiteca.reports.application.ReportFilter;
import br.com.minhagibiteca.shared.application.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReportQueries {
    private final NamedParameterJdbcTemplate jdbc;
    public ReportQueries(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    private String where(ReportFilter f) {
        String sql = " from editions e where e.owner_user_id=:owner";
        if (!f.q().isEmpty()) sql += " and (e.title ilike :q escape '!' or e.isbn ilike :q escape '!' or e.editorial_number ilike :q escape '!')";
        if (f.seriesId() != null) sql += " and e.series_id=:series";
        if (f.publisherId() != null) sql += " and e.publisher_id=:publisher";
        if (f.start() != null) sql += " and e.purchase_date between :start and :end";
        if (!f.mediaType().equals("ALL")) sql += f.mediaType().equals("UNSPECIFIED") ? " and e.media_type is null" : " and e.media_type=:media";
        if (!f.reading().equals("ALL")) sql += " and e.is_read=:read";
        if (f.creatorId() != null || f.creatorFunction() != null) {
            sql += " and exists(select 1 from edition_creators ec join creators c on c.id=ec.creator_id where ec.edition_id=e.id and c.owner_user_id=:owner";
            if (f.creatorId() != null) sql += " and c.id=:creator";
            if (f.creatorFunction() != null) sql += " and exists(select 1 from creator_functions cf where cf.creator_id=c.id and cf.function=:function)";
            sql += ")";
        }
        if (f.characterId() != null) sql += " and exists(select 1 from edition_characters ec where ec.edition_id=e.id and ec.owner_user_id=:owner and ec.character_id=:character)";
        return sql;
    }
    private MapSqlParameterSource params(UUID owner, ReportFilter f) {
        return new MapSqlParameterSource("owner", owner)
                .addValue("q", "%" + f.q().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%")
                .addValue("series", f.seriesId()).addValue("publisher", f.publisherId()).addValue("creator", f.creatorId())
                .addValue("character", f.characterId()).addValue("function", f.creatorFunction() == null ? null : f.creatorFunction().name())
                .addValue("media", f.mediaType()).addValue("read", f.reading().equals("READ"))
                .addValue("start", f.start()).addValue("end", f.end());
    }
    public Summary summary(UUID owner, ReportFilter f) {
        return jdbc.queryForObject("with filtered as (select e.*" + where(f) + ") " + """
                select count(*) n, count(*) filter(where media_type='PHYSICAL') physical,
                count(*) filter(where media_type='DIGITAL') digital, count(*) filter(where media_type is null) unknown_media,
                count(reference_paid_price) priced, coalesce(sum(reference_paid_price),0) paid,
                count(*) filter(where is_read) nread, count(total_pages) paged,
                coalesce(sum(total_pages),0) total_pages, coalesce(sum(pages_read),0) pages_read,
                (select count(*) from physical_copies pc join filtered f on f.id=pc.edition_id where pc.owner_user_id=:owner) copies
                from filtered
                """, params(owner, f), (r, i) -> new Summary(r.getLong("n"), r.getLong("physical"), r.getLong("digital"),
                r.getLong("unknown_media"), r.getLong("copies"), r.getBigDecimal("paid"), r.getLong("priced"), r.getLong("nread"),
                r.getLong("total_pages"), r.getLong("pages_read"), r.getLong("paged")));
    }
    public List<Entry> entries(UUID owner, ReportFilter f, long offset, int limit) {
        String sql = """
                select e.id,e.title,e.editorial_number,e.isbn,e.media_type,e.is_read,e.total_pages,e.pages_read,
                    e.reference_paid_price,e.purchase_date,
                    (select s.name from series s where s.id=e.series_id and s.owner_user_id=:owner) series,
                    (select p.name from publishers p where p.id=e.publisher_id and p.owner_user_id=:owner) publisher,
                    (select string_agg(c.name || ' (' || coalesce((select string_agg(case cf.function
                        when 'ROTEIRISTA' then 'Roteirista' when 'DESENHISTA' then 'Desenhista' when 'ARTE_FINALISTA' then 'Arte-finalista'
                        when 'COLORISTA' then 'Colorista' when 'LETRISTA' then 'Letrista' when 'CAPISTA' then 'Capista'
                        when 'EDITOR' then 'Editor' when 'TRADUTOR' then 'Tradutor' else 'Outra função' end, ', ' order by cf.function)
                        from creator_functions cf where cf.creator_id=c.id), 'Funções não informadas') || ')', '; ' order by c.name,c.id)
                        from creators c where c.owner_user_id=:owner and exists(select 1 from edition_creators ec where ec.edition_id=e.id and ec.creator_id=c.id)) creators,
                    (select string_agg(c.name, '; ' order by c.name,c.id) from edition_characters ec join comic_characters c
                        on c.id=ec.character_id and c.owner_user_id=ec.owner_user_id where ec.edition_id=e.id and ec.owner_user_id=:owner) characters,
                    (select count(*) from physical_copies pc where pc.edition_id=e.id and pc.owner_user_id=:owner) copies,
                    (select string_agg(x.name, '; ' order by x.name) from (select distinct coalesce(l.name,'Não informada') name
                        from physical_copies pc left join storage_locations l on l.id=pc.storage_location_id and l.owner_user_id=:owner
                        where pc.edition_id=e.id and pc.owner_user_id=:owner) x) locations
                """ + where(f) + " order by e.normalized_title,e.id limit :limit offset :offset";
        return jdbc.query(sql, params(owner, f).addValue("offset", offset).addValue("limit", limit), (r, i) -> new Entry(
                r.getObject("id", UUID.class), r.getString("title"), r.getString("editorial_number"), r.getString("isbn"),
                missing(r.getString("series")), missing(r.getString("publisher")), missing(r.getString("creators")),
                missing(r.getString("characters")), switch (missing(r.getString("media_type"))) {case "PHYSICAL" -> "Físico"; case "DIGITAL" -> "Digital"; default -> "Não informado";},
                r.getBoolean("is_read"), r.getObject("total_pages", Integer.class), r.getInt("pages_read"), r.getBigDecimal("reference_paid_price"),
                r.getObject("purchase_date", LocalDate.class), r.getLong("copies"), missing(r.getString("locations"))));
    }
    private static String missing(String value) { return value == null ? "Não informado" : value; }
    private String table(String kind) {
        return switch (kind) { case "series" -> "series"; case "publisher" -> "publishers"; case "creator" -> "creators"; case "character" -> "comic_characters"; default -> throw new IllegalArgumentException(); };
    }
    public List<Option> options(UUID owner, String kind) {
        return jdbc.query("select id,name from " + table(kind) + " where owner_user_id=:owner order by lower(name),id",
                new MapSqlParameterSource("owner", owner), (r,i) -> new Option(r.getObject("id",UUID.class),r.getString("name")));
    }
    public String requireName(UUID owner, String kind, UUID id) {
        var names = jdbc.queryForList("select name from " + table(kind) + " where id=:id and owner_user_id=:owner",
                new MapSqlParameterSource("owner",owner).addValue("id",id), String.class);
        if (names.isEmpty()) throw new ResourceNotFoundException();
        return names.getFirst();
    }
}
