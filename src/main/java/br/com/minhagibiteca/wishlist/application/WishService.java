package br.com.minhagibiteca.wishlist.application;

import br.com.minhagibiteca.catalog.domain.PublicationType;
import br.com.minhagibiteca.wishlist.domain.*;
import br.com.minhagibiteca.wishlist.infrastructure.WishRepository;
import br.com.minhagibiteca.shared.application.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service @Validated
public class WishService {
    private final WishRepository repo;
    public WishService(WishRepository repo){this.repo=repo;}
    @Transactional(readOnly=true) public Page<WishItem> list(UUID owner,String q,Boolean acquired,int page){return repo.list(owner,q==null?"":q.trim().toLowerCase(Locale.ROOT),acquired,page);}
    @Transactional(readOnly=true) public WishItem find(UUID id,UUID owner){return repo.find(id,owner).orElseThrow(ResourceNotFoundException::new);}
    @Transactional public UUID create(UUID owner,@Valid Command c){UUID id=UUID.randomUUID();repo.insert(owner,item(id,c));return id;}
    @Transactional public void update(UUID id,UUID owner,@Valid Command c){if(repo.update(owner,item(id,c))!=1)throw new ResourceNotFoundException();}
    @Transactional public void acquired(UUID id,UUID owner,boolean value){if(repo.acquired(id,owner,value)!=1)throw new ResourceNotFoundException();}
    @Transactional public void delete(UUID id,UUID owner){if(repo.delete(id,owner)!=1)throw new ResourceNotFoundException();}
    private WishItem item(UUID id,Command c){return new WishItem(id,c.title().trim(),c.publicationType(),trim(c.author()),trim(c.publisher()),trim(c.isbn()),c.estimatedPrice(),c.priority(),false,trim(c.notes()));}
    private String trim(String s){return s==null||s.isBlank()?null:s.trim();}
    public record Command(@NotBlank @Size(max=240) String title,@NotNull PublicationType publicationType,@Size(max=180) String author,
            @Size(max=180) String publisher,@Size(max=32) String isbn,@DecimalMin("0") @Digits(integer=10,fraction=2) BigDecimal estimatedPrice,
            @NotNull WishPriority priority,@Size(max=2000) String notes){}
}
