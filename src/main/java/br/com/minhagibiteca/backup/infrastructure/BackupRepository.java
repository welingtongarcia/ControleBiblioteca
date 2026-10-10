package br.com.minhagibiteca.backup.infrastructure;

import br.com.minhagibiteca.backup.domain.BackupTables;
import br.com.minhagibiteca.backup.application.InvalidBackupException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

@Repository
public class BackupRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public BackupRepository(JdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc = jdbc; this.mapper = mapper.copy().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS); }
    public String schemaVersion() {
        return jdbc.queryForObject("select version from flyway_schema_history where success and version is not null order by installed_rank desc limit 1", String.class);
    }
    public boolean empty(UUID owner) {
        for (var table : BackupTables.OWNED)
            if (Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from " + table + " where owner_user_id=?)", Boolean.class, owner))) return false;
        return true;
    }
    public void lock(UUID owner) {
        if (jdbc.queryForList("select id from users where id=? for update", UUID.class, owner).isEmpty())
            throw new InvalidBackupException("Conta de destino não encontrada.");
    }
    public Set<String> columns(String table) {
        return jdbc.query("select * from " + table + " limit 0", rs -> {
            var names = new HashSet<String>(); var metadata = rs.getMetaData();
            for (int i=1; i<=metadata.getColumnCount(); i++) names.add(metadata.getColumnName(i));
            return names;
        });
    }
    public ObjectNode snapshot(UUID owner) {
        var data = mapper.createObjectNode(); int[] count = {0}; long[] bytes = {0};
        for (var table : BackupTables.all()) {
            var rows = data.putArray(table);
            String query = "select row_to_json(t)::text from " + table + " t where t.owner_user_id=? order by t.id";
            Object[] args = {owner};
            if (table.equals("edition_characters")) query = "select row_to_json(t)::text from edition_characters t where owner_user_id=? order by edition_id,character_id";
            if (Set.of("edition_works", "edition_creators").contains(table)) {
                String target = table.equals("edition_works") ? "works" : "creators";
                String key = table.equals("edition_works") ? "work_id" : "creator_id";
                query = "select row_to_json(t)::text from " + table + " t join editions e on e.id=t.edition_id join " + target + " r on r.id=t." + key + " where e.owner_user_id=? and r.owner_user_id=? order by t.edition_id,t." + key;
                args = new Object[]{owner, owner};
            }
            if (table.equals("creator_functions")) query = "select row_to_json(t)::text from creator_functions t join creators c on c.id=t.creator_id where c.owner_user_id=? order by t.creator_id,t.function";
            String sql=query;Object[] parameters=args;
            jdbc.query(connection -> {
                var statement=connection.prepareStatement(sql);statement.setFetchSize(200);
                for(int i=0;i<parameters.length;i++)statement.setObject(i+1,parameters[i]);return statement;
            }, (RowCallbackHandler) rs -> {
                String json = rs.getString(1); bytes[0] += json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                if (++count[0] > BackupArchive.MAX_ROWS || bytes[0] > BackupArchive.MAX_JSON)
                    throw new InvalidBackupException("Sua coleção excede o limite deste backup (100.000 registros / 25 MB de dados).");
                try { rows.add(mapper.readTree(json)); }
                catch (java.io.IOException e) { throw new IllegalStateException("Dados do banco inválidos", e); }
            });
        }
        return data;
    }
    public void insertAll(String table, ArrayNode rows) {
        // Table comes only from BackupTables.all(); PostgreSQL performs type and constraint validation.
        String sql="insert into " + table + " select * from jsonb_populate_record(null::" + table + ",cast(? as jsonb))";
        for(int offset=0;offset<rows.size();offset+=200) {
            int start=offset;int size=Math.min(200,rows.size()-start);
            jdbc.batchUpdate(sql,new BatchPreparedStatementSetter() {
                @Override public int getBatchSize(){return size;}
                @Override public void setValues(java.sql.PreparedStatement statement,int index) throws java.sql.SQLException {statement.setString(1,rows.get(start+index).toString());}
            });
        }
    }
}
