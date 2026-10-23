package br.com.minhagibiteca.digital.infrastructure;

import br.com.minhagibiteca.digital.domain.*;
import br.com.minhagibiteca.shared.application.ResourceNotFoundException;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class OneDriveRepository {
    private final JdbcTemplate jdbc;
    public OneDriveRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public record Connection(UUID generation,String driveId,String driveName,String accessToken,String refreshToken,Instant expiresAt,String scopes,Instant lastSync,String error){@Override public String toString(){return "OneDrive connection [protected]";}}
    public record Folder(UUID id,String remoteId,String name){}
    public Optional<Connection> connection(UUID owner){return jdbc.query("select * from onedrive_connections where owner_user_id=?",(rs,n)->new Connection(rs.getObject("generation",UUID.class),rs.getString("drive_id"),rs.getString("drive_name"),rs.getString("access_token"),rs.getString("refresh_token"),rs.getTimestamp("token_expires_at").toInstant(),rs.getString("scopes"),rs.getTimestamp("last_synced_at")==null?null:rs.getTimestamp("last_synced_at").toInstant(),rs.getString("sync_error")),owner).stream().findFirst();}
    @Transactional public void connect(UUID owner,String driveId,String name,String access,String refresh,Instant expires,String scopes){
        jdbc.queryForList("select id from users where id=? for update",UUID.class,owner);
        jdbc.update("delete from onedrive_connections where owner_user_id=?",owner);
        jdbc.update("insert into onedrive_connections(owner_user_id,generation,drive_id,drive_name,access_token,refresh_token,token_expires_at,scopes) values(?,?,?,?,?,?,?,?)",owner,UUID.randomUUID(),driveId,name,access,refresh,java.sql.Timestamp.from(expires),scopes);
    }
    public void refreshed(UUID owner,UUID generation,String access,String refresh,Instant expires,String scopes){if(jdbc.update("update onedrive_connections set access_token=?,refresh_token=?,token_expires_at=?,scopes=? where owner_user_id=? and generation=?",access,refresh,java.sql.Timestamp.from(expires),scopes,owner,generation)!=1)throw disconnected();}
    @Transactional public void disconnect(UUID owner){jdbc.queryForList("select id from users where id=? for update",UUID.class,owner);jdbc.update("delete from onedrive_connections where owner_user_id=?",owner);}
    public List<Folder> folders(UUID owner){return jdbc.query("select * from onedrive_folders where owner_user_id=? order by lower(name),id",(rs,n)->new Folder(rs.getObject("id",UUID.class),rs.getString("remote_id"),rs.getString("name")),owner);}
    private void lock(UUID owner){if(jdbc.queryForList("select owner_user_id from onedrive_connections where owner_user_id=? for update",UUID.class,owner).isEmpty())throw disconnected();}
    private void editable(UUID owner){lock(owner);if(Boolean.TRUE.equals(jdbc.queryForObject("select sync_token is not null and sync_started_at>now()-interval '5 minutes' from onedrive_connections where owner_user_id=?",Boolean.class,owner)))throw new OneDriveException("Aguarde a sincronização terminar antes de alterar as pastas.");}
    @Transactional public void addFolder(UUID owner,UUID generation,String remoteId,String name){editable(owner);if(!connection(owner).orElseThrow(OneDriveRepository::disconnected).generation().equals(generation))throw disconnected();if(folders(owner).size()>=20)throw new OneDriveException("Selecione no máximo 20 pastas. Subpastas já são incluídas na sincronização.");jdbc.update("insert into onedrive_folders(id,owner_user_id,remote_id,name) values(?,?,?,?) on conflict(owner_user_id,remote_id) do nothing",UUID.randomUUID(),owner,remoteId,name);}
    @Transactional public void removeFolder(UUID id,UUID owner){editable(owner);if(jdbc.update("delete from onedrive_folders where id=? and owner_user_id=?",id,owner)!=1)throw new ResourceNotFoundException();}
    @Transactional public UUID begin(UUID owner,UUID generation){lock(owner);UUID run=UUID.randomUUID();if(jdbc.update("update onedrive_connections set sync_token=?,sync_started_at=now(),sync_error=null where owner_user_id=? and generation=? and (sync_token is null or sync_started_at<now()-interval '5 minutes')",run,owner,generation)!=1)throw new OneDriveException("Já há uma sincronização em andamento. Aguarde e tente novamente.");return run;}
    @Transactional public void publish(UUID owner,UUID generation,UUID run,List<DigitalFile> files){
        lock(owner);if(!Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from onedrive_connections where owner_user_id=? and generation=? and sync_token=?)",Boolean.class,owner,generation,run)))throw disconnected();
        jdbc.update("update onedrive_files set available=false where owner_user_id=?",owner);
        jdbc.batchUpdate("insert into onedrive_files(id,owner_user_id,remote_id,name,extension,folder_path,size_bytes,remote_modified_at,web_url,available) values(?,?,?,?,?,?,?,?,?,true) on conflict(owner_user_id,remote_id) do update set name=excluded.name,extension=excluded.extension,folder_path=excluded.folder_path,size_bytes=excluded.size_bytes,remote_modified_at=excluded.remote_modified_at,web_url=excluded.web_url,available=true",files,200,(s,f)->{s.setObject(1,UUID.randomUUID());s.setObject(2,owner);s.setString(3,f.remoteId());s.setString(4,f.name());s.setString(5,f.extension());s.setString(6,f.folderPath());s.setLong(7,f.sizeBytes());s.setTimestamp(8,f.modifiedAt()==null?null:java.sql.Timestamp.from(f.modifiedAt()));s.setString(9,f.webUrl());});
        jdbc.update("update onedrive_connections set sync_token=null,last_synced_at=now(),sync_error=null where owner_user_id=?",owner);
    }
    public void failed(UUID owner,UUID generation,UUID run){jdbc.update("update onedrive_connections set sync_token=null,sync_error='Não foi possível concluir a sincronização. A listagem anterior foi mantida.' where owner_user_id=? and generation=? and sync_token=?",owner,generation,run);}
    private static final String FILES="select f.*,e.title edition_title from onedrive_files f left join editions e on e.id=f.edition_id and e.owner_user_id=f.owner_user_id";
    private static final RowMapper<DigitalFile> FILE=(rs,n)->new DigitalFile(rs.getObject("id",UUID.class),rs.getString("remote_id"),rs.getString("name"),rs.getString("extension"),rs.getString("folder_path"),rs.getLong("size_bytes"),rs.getTimestamp("remote_modified_at")==null?null:rs.getTimestamp("remote_modified_at").toInstant(),rs.getString("web_url"),rs.getBoolean("available"),rs.getObject("edition_id",UUID.class),rs.getString("edition_title"));
    public DigitalFile file(UUID id,UUID owner){return jdbc.query(FILES+" where f.id=? and f.owner_user_id=?",FILE,id,owner).stream().findFirst().orElseThrow(ResourceNotFoundException::new);}
    public Page<DigitalFile> files(UUID owner,String query,String extension,int page){
        String q=query==null?"":query.trim().toLowerCase(Locale.ROOT);String where=" where f.owner_user_id=? and (strpos(lower(f.name),?)>0 or strpos(lower(f.folder_path),?)>0)";var args=new ArrayList<Object>(List.of(owner,q,q));
        if(extension!=null&&!extension.isBlank()){where+=" and f.extension=?";args.add(extension.toLowerCase(Locale.ROOT));}
        long count=Objects.requireNonNull(jdbc.queryForObject("select count(*) from onedrive_files f"+where,Long.class,args.toArray()));var p=PageRequest.of(Math.max(0,page),20);args.add(20);args.add(p.getOffset());return new PageImpl<>(jdbc.query(FILES+where+" order by lower(f.name),f.id limit ? offset ?",FILE,args.toArray()),p,count);
    }
    @Transactional public void link(UUID id,UUID owner,UUID edition){
        file(id,owner);if(edition!=null&&!Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from editions where id=? and owner_user_id=?)",Boolean.class,edition,owner)))throw new ResourceNotFoundException();
        if(jdbc.update("update onedrive_files set edition_id=? where id=? and owner_user_id=?",edition,id,owner)!=1)throw new ResourceNotFoundException();
    }
    private static OneDriveException disconnected(){return new OneDriveException("A conexão OneDrive foi alterada ou desconectada. Recarregue a página e tente novamente.");}
}
