package br.com.minhagibiteca.catalog.infrastructure;

import br.com.minhagibiteca.catalog.application.AssociationOptionsService.*;
import br.com.minhagibiteca.catalog.domain.CreatorFunction;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AssociationOptionsRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public AssociationOptionsRepository(JdbcTemplate jdbc) { this.jdbc = new NamedParameterJdbcTemplate(jdbc); }
    private String base(Kind kind) {
        String table = switch (kind) { case WORK -> "works"; case CREATOR -> "creators"; case CHARACTER -> "comic_characters"; };
        String label = kind == Kind.WORK ? "title" : "name";
        String functions = kind == Kind.CREATOR
                ? "(select string_agg(cf.function, ',' order by cf.function) from creator_functions cf where cf.creator_id = t.id)"
                : "null";
        return "select t.id, t." + label + " as label, " + functions + " as functions from " + table
                + " t where t.owner_user_id = :owner";
    }
    // Sem extensões PostgreSQL; mesmo alfabeto utilizado nos nomes em pt-BR.
    private String folded(String expression) {
        return "translate(lower(" + expression + "), 'áàâãäéèêëíìîïóòôõöúùûüç', 'aaaaaeeeeiiiiooooouuuuc')";
    }
    public List<Option> search(UUID owner, Kind kind, String q, CreatorFunction function, int page) {
        var params = new HashMap<String, Object>(); params.put("owner", owner);
        params.put("q", "%" + q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
        params.put("offset", page * 20);
        String label = kind == Kind.WORK ? "t.title" : "t.name";
        String sql = base(kind) + " and " + folded(label) + " like " + folded(":q") + " escape '\\'";
        if (function != null) { sql += " and exists(select 1 from creator_functions f where f.creator_id=t.id and f.function=:function)"; params.put("function", function.name()); }
        return jdbc.query(sql + " order by " + folded(label) + ", t.id limit 21 offset :offset", params, mapper(kind));
    }
    public List<Option> selected(UUID owner, Kind kind, List<UUID> ids) {
        if (ids.isEmpty()) return List.of();
        return jdbc.query(base(kind) + " and t.id in (:ids) order by label, t.id", Map.of("owner", owner, "ids", ids), mapper(kind));
    }
    private RowMapper<Option> mapper(Kind kind) {
        return (row, index) -> {
            String functions = row.getString("functions");
            String description = functions == null ? (kind == Kind.CREATOR ? "Funções não informadas" : "") : Arrays.stream(functions.split(","))
                    .map(CreatorFunction::valueOf).sorted().map(CreatorFunction::getLabel).collect(java.util.stream.Collectors.joining(", "));
            return new Option(row.getObject("id", UUID.class), row.getString("label"), description);
        };
    }
}
