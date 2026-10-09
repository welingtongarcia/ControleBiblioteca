package br.com.minhagibiteca.catalog.infrastructure;
import br.com.minhagibiteca.catalog.application.CharacterView;
import br.com.minhagibiteca.shared.application.ResourceNotFoundException;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.*;
@Repository
public class CharacterRepository {
 private final JdbcTemplate jdbc;
 public CharacterRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public CharacterView find(UUID owner,UUID id){return jdbc.query("select id,name from comic_characters where id=? and owner_user_id=?",(r,n)->new CharacterView(r.getObject("id",UUID.class),r.getString("name")),id,owner).stream().findFirst().orElseThrow(ResourceNotFoundException::new);}
 public List<CharacterView> options(UUID owner){return jdbc.query("select id,name from comic_characters where owner_user_id=? order by normalized_name,id",(r,n)->new CharacterView(r.getObject("id",UUID.class),r.getString("name")),owner);}
 public Page<CharacterView> list(UUID owner,String q,int page){
  String pattern="%"+q.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";
  long count=jdbc.queryForObject("select count(*) from comic_characters where owner_user_id=? and normalized_name like ? escape '!'",Long.class,owner,pattern);
  var rows=jdbc.query("select id,name from comic_characters where owner_user_id=? and normalized_name like ? escape '!' order by normalized_name,id limit 20 offset ?",(r,n)->new CharacterView(r.getObject("id",UUID.class),r.getString("name")),owner,pattern,(long)Math.max(0,page)*20);
  return new PageImpl<>(rows,PageRequest.of(Math.max(0,page),20),count);
 }
 public void lock(UUID owner){jdbc.queryForObject("select id from users where id=? for update",UUID.class,owner);}
 public boolean duplicate(UUID owner,String normalized,UUID except){return jdbc.queryForObject("select count(*) from comic_characters where owner_user_id=? and normalized_name=? and id<>?",Long.class,owner,normalized,except)>0;}
 public void save(UUID owner,UUID id,String name,String normalized,boolean update){if(update)jdbc.update("update comic_characters set name=?,normalized_name=? where id=? and owner_user_id=?",name,normalized,id,owner);else jdbc.update("insert into comic_characters(id,owner_user_id,name,normalized_name) values(?,?,?,?)",id,owner,name,normalized);}
 public boolean referenced(UUID owner,UUID id){return jdbc.queryForObject("select count(*) from edition_characters where owner_user_id=? and character_id=?",Long.class,owner,id)>0;}
 public void delete(UUID owner,UUID id){jdbc.update("delete from comic_characters where id=? and owner_user_id=?",id,owner);}
 public List<UUID> ids(UUID owner,UUID edition){return jdbc.queryForList("select character_id from edition_characters where owner_user_id=? and edition_id=? order by character_id",UUID.class,owner,edition);}
 public List<CharacterView> edition(UUID owner,UUID edition){return jdbc.query("select c.id,c.name from comic_characters c join edition_characters ec on ec.character_id=c.id and ec.owner_user_id=c.owner_user_id where ec.owner_user_id=? and ec.edition_id=? order by c.normalized_name,c.id",(r,n)->new CharacterView(r.getObject("id",UUID.class),r.getString("name")),owner,edition);}
 public void replace(UUID owner,UUID edition,List<UUID> ids){jdbc.update("delete from edition_characters where owner_user_id=? and edition_id=?",owner,edition);for(UUID id:ids)jdbc.update("insert into edition_characters(owner_user_id,edition_id,character_id) values(?,?,?)",owner,edition,id);}
}
