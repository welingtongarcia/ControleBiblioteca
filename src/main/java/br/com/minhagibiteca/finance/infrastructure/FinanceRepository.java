package br.com.minhagibiteca.finance.infrastructure;
import br.com.minhagibiteca.finance.application.*;
import br.com.minhagibiteca.finance.application.FinanceView.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.*;
@Repository
public class FinanceRepository {
 private final NamedParameterJdbcTemplate jdbc;
 public FinanceRepository(NamedParameterJdbcTemplate jdbc){this.jdbc=jdbc;}
 private MapSqlParameterSource params(UUID owner,FinanceFilter f){
  return new MapSqlParameterSource("owner",owner).addValue("start",f.start()).addValue("end",f.end())
   .addValue("series",f.seriesId()).addValue("creator",f.creatorId()).addValue("character",f.characterId())
   .addValue("publisher",f.publisherId()).addValue("media",f.mediaType()==null?null:f.mediaType().name());
 }
 private String where(FinanceFilter f){
  String sql=" from financial_purchases p join editions e on e.id=p.edition_id and e.owner_user_id=p.owner_user_id where p.owner_user_id=:owner";
  if(f.start()!=null)sql+=" and p.purchase_date between :start and :end";
  if(f.seriesId()!=null)sql+=" and e.series_id=:series";
  if(f.publisherId()!=null)sql+=" and e.publisher_id=:publisher";
  if(f.mediaType()!=null)sql+=" and e.media_type=:media";
  if(f.unknownMedia())sql+=" and e.media_type is null";
  if(f.creatorId()!=null)sql+=" and exists(select 1 from edition_creators ec join creators c on c.id=ec.creator_id where ec.edition_id=e.id and c.owner_user_id=:owner and c.id=:creator)";
  if(f.characterId()!=null)sql+=" and exists(select 1 from edition_characters ec where ec.edition_id=e.id and ec.owner_user_id=:owner and ec.character_id=:character)";
  return sql;
 }
 public Summary summary(UUID owner,FinanceFilter f){
  return jdbc.queryForObject("select count(*) n,coalesce(sum(p.amount),0) total,count(*) filter(where p.purchase_date is null) undated,coalesce(sum(p.amount) filter(where p.purchase_date is null),0) undated_amount,coalesce(sum(p.amount) filter(where not e.is_read),0) unread"+where(f),params(owner,f),
   (r,n)->new Summary(r.getLong("n"),r.getBigDecimal("total"),r.getLong("undated"),r.getBigDecimal("undated_amount"),r.getBigDecimal("unread")));
 }
 public Page<Purchase> list(UUID owner,FinanceFilter f,int page,long count){
  var param=params(owner,f).addValue("offset",(long)Math.max(0,page)*20);
  var rows=jdbc.query("select p.*,e.title"+where(f)+" order by p.purchase_date desc nulls last,p.created_at desc,p.id limit 20 offset :offset",param,
   (r,n)->new Purchase(r.getObject("id",UUID.class),r.getObject("edition_id",UUID.class),r.getString("title"),r.getBigDecimal("amount"),r.getObject("purchase_date",LocalDate.class),r.getString("vendor"),r.getString("notes")));
  return new PageImpl<>(rows,PageRequest.of(Math.max(0,page),20),count);
 }
 public List<Bucket> groups(UUID owner,FinanceFilter f,String dimension,boolean quantity){
  String key,label,join="";
  switch(dimension){
   case "seriesId" -> {key="g.id::text";label="coalesce(g.name,'Sem série')";join=" left join series g on g.id=e.series_id and g.owner_user_id=:owner";}
   case "publisherId" -> {key="g.id::text";label="coalesce(g.name,'Sem editora')";join=" left join publishers g on g.id=e.publisher_id and g.owner_user_id=:owner";}
   case "creatorId" -> {key="g.id::text";label="g.name";join=" join (select distinct ec.edition_id,c.id,c.name from edition_creators ec join creators c on c.id=ec.creator_id where c.owner_user_id=:owner) g on g.edition_id=e.id";}
   case "characterId" -> {key="g.id::text";label="g.name";join=" join (select ec.edition_id,c.id,c.name from edition_characters ec join comic_characters c on c.id=ec.character_id and c.owner_user_id=ec.owner_user_id where c.owner_user_id=:owner) g on g.edition_id=e.id";}
   case "mediaType" -> {key="coalesce(e.media_type,'UNSPECIFIED')";label="case e.media_type when 'PHYSICAL' then 'Físico' when 'DIGITAL' then 'Digital' else 'Não informado' end";}
   default -> throw new IllegalArgumentException("Dimensão inválida");
  }
  String base=where(f).replace(" where p.owner_user_id",join+" where p.owner_user_id");
  return jdbc.query("select "+key+" k,"+label+" label,count(*) n,sum(p.amount) amount"+base+" group by "+key+","+label+" order by "+(quantity?"n":"amount")+" desc,label,k nulls last limit 11",params(owner,f),
   (r,n)->new Bucket(r.getString("k"),r.getString("label"),r.getLong("n"),r.getBigDecimal("amount")));
 }
 public List<Bucket> months(UUID owner,FinanceFilter f){
  return jdbc.query("select to_char(p.purchase_date,'YYYY-MM') k,count(*) n,sum(p.amount) amount"+where(f)+" group by k order by k",params(owner,f),
   (r,n)->new Bucket(r.getString("k"),r.getString("k"),r.getLong("n"),r.getBigDecimal("amount")));
 }
 public List<Option> options(UUID owner,String kind){
  String table,column;
  switch(kind){case "edition"->{table="editions";column="title";} case "series"->{table="series";column="name";}case "creator"->{table="creators";column="name";}case "publisher"->{table="publishers";column="name";}case "character"->{table="comic_characters";column="name";}default->throw new IllegalArgumentException();}
  return jdbc.query("select id,"+column+" name from "+table+" where owner_user_id=:owner order by lower("+column+"),id",Map.of("owner",owner),(r,n)->new Option(r.getObject("id",UUID.class),r.getString("name")));
 }
 public void requireOption(UUID owner,String kind,UUID id){
  if(id==null)return;
  String table=switch(kind){case "edition"->"editions";case "series"->"series";case "creator"->"creators";case "publisher"->"publishers";case "character"->"comic_characters";default->throw new IllegalArgumentException();};
  if(!Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from "+table+" where id=:id and owner_user_id=:owner)",Map.of("id",id,"owner",owner),Boolean.class)))throw new br.com.minhagibiteca.shared.application.ResourceNotFoundException();
 }
 public Purchase find(UUID owner,UUID id){
  return jdbc.query("select p.*,e.title from financial_purchases p join editions e on e.id=p.edition_id and e.owner_user_id=p.owner_user_id where p.id=:id and p.owner_user_id=:owner",Map.of("id",id,"owner",owner),
   (r,n)->new Purchase(id,r.getObject("edition_id",UUID.class),r.getString("title"),r.getBigDecimal("amount"),r.getObject("purchase_date",LocalDate.class),r.getString("vendor"),r.getString("notes"))).stream().findFirst().orElseThrow(br.com.minhagibiteca.shared.application.ResourceNotFoundException::new);
 }
 public void lock(UUID owner){jdbc.queryForObject("select id from users where id=:owner for update",Map.of("owner",owner),UUID.class);}
 public void save(UUID owner,UUID id,UUID edition,BigDecimal amount,LocalDate date,String vendor,String notes,boolean update){
  var p=new MapSqlParameterSource("owner",owner).addValue("id",id).addValue("edition",edition).addValue("amount",amount).addValue("date",date).addValue("vendor",vendor).addValue("notes",notes);
  jdbc.update(update?"update financial_purchases set edition_id=:edition,amount=:amount,purchase_date=:date,vendor=:vendor,notes=:notes,updated_at=current_timestamp where id=:id and owner_user_id=:owner":"insert into financial_purchases(id,owner_user_id,edition_id,amount,purchase_date,vendor,notes) values(:id,:owner,:edition,:amount,:date,:vendor,:notes)",p);
 }
 public void delete(UUID owner,UUID id){jdbc.update("delete from financial_purchases where id=:id and owner_user_id=:owner",Map.of("id",id,"owner",owner));}
}
