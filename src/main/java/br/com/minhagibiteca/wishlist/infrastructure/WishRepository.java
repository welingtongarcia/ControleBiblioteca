package br.com.minhagibiteca.wishlist.infrastructure;

import br.com.minhagibiteca.catalog.domain.PublicationType;
import br.com.minhagibiteca.wishlist.domain.*;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

@Repository
public class WishRepository {
    private final JdbcTemplate jdbc;
    public WishRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    private static final RowMapper<WishItem> ROW=(rs,n)->new WishItem(rs.getObject("id",UUID.class),rs.getString("title"),
            PublicationType.valueOf(rs.getString("publication_type")),rs.getString("author"),rs.getString("publisher"),rs.getString("isbn"),
            rs.getBigDecimal("estimated_price"),WishPriority.valueOf(rs.getString("priority")),rs.getBoolean("acquired"),rs.getString("notes"));
    public Optional<WishItem> find(UUID id,UUID owner){return jdbc.query("select * from wishlist_items where id=? and owner_user_id=?",ROW,id,owner).stream().findFirst();}
    public Page<WishItem> list(UUID owner,String q,Boolean acquired,int page){
        String where=" where owner_user_id=? and (strpos(lower(title),?)>0 or strpos(lower(coalesce(author,'')),?)>0 or strpos(lower(coalesce(isbn,'')),?)>0)";
        var args=new ArrayList<Object>(List.of(owner,q,q,q));
        if(acquired!=null){where+=" and acquired=?";args.add(acquired);}
        long total=Objects.requireNonNull(jdbc.queryForObject("select count(*) from wishlist_items"+where,Long.class,args.toArray()));
        var pageable=PageRequest.of(Math.max(0,page),20);
        args.add(pageable.getPageSize());args.add(pageable.getOffset());
        var rows=jdbc.query("select * from wishlist_items"+where+" order by case priority when 'HIGH' then 0 when 'NORMAL' then 1 else 2 end,lower(title),id limit ? offset ?",ROW,args.toArray());
        return new PageImpl<>(rows,pageable,total);
    }
    public void insert(UUID owner,WishItem w){jdbc.update("insert into wishlist_items(id,owner_user_id,title,publication_type,author,publisher,isbn,estimated_price,priority,notes) values(?,?,?,?,?,?,?,?,?,?)",
            w.id(),owner,w.title(),w.publicationType().name(),w.author(),w.publisher(),w.isbn(),w.estimatedPrice(),w.priority().name(),w.notes());}
    public int update(UUID owner,WishItem w){return jdbc.update("update wishlist_items set title=?,publication_type=?,author=?,publisher=?,isbn=?,estimated_price=?,priority=?,notes=?,updated_at=now() where id=? and owner_user_id=?",
            w.title(),w.publicationType().name(),w.author(),w.publisher(),w.isbn(),w.estimatedPrice(),w.priority().name(),w.notes(),w.id(),owner);}
    public int acquired(UUID id,UUID owner,boolean value){return jdbc.update("update wishlist_items set acquired=?,updated_at=now() where id=? and owner_user_id=?",value,id,owner);}
    public int delete(UUID id,UUID owner){return jdbc.update("delete from wishlist_items where id=? and owner_user_id=?",id,owner);}
}
