package br.com.minhagibiteca.catalog.application;
import br.com.minhagibiteca.catalog.domain.Edition; import br.com.minhagibiteca.catalog.infrastructure.EditionRepository; import br.com.minhagibiteca.shared.application.ResourceNotFoundException; import java.util.*; import org.springframework.data.domain.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service public class EditionService { private final EditionRepository repo; private final PublisherService publishers; private final ComicSeriesService series; public EditionService(EditionRepository r,PublisherService p,ComicSeriesService s){repo=r;publishers=p;series=s;}
 @Transactional(readOnly=true) public Page<Edition> list(UUID owner,String q,Pageable p){return repo.findByOwnerUserIdAndNormalizedTitleContaining(owner,q==null?"":q.trim().toLowerCase(),p);} @Transactional(readOnly=true) public Edition findOwned(UUID id,UUID owner){return repo.findByIdAndOwnerUserId(id,owner).orElseThrow(ResourceNotFoundException::new);}
 @Transactional public Edition create(UUID owner,Command c){validate(owner,c);Edition e=Edition.create(owner,c.publisherId(),c.seriesId(),c.title(),c.editorialNumber(),c.variant(),c.isbn(),c.publicationYear(),c.format(),c.notes());e.updatePricingAndMedia(c.mediaType(),c.coverPrice(),c.referencePaidPrice());return repo.save(e);}
 @Transactional public void update(UUID id,UUID owner,Command c){validate(owner,c); Edition e=findOwned(id,owner);e.update(c.publisherId(),c.seriesId(),c.title(),c.editorialNumber(),c.variant(),c.isbn(),c.publicationYear(),c.format(),c.notes());e.updatePricingAndMedia(c.mediaType(),c.coverPrice(),c.referencePaidPrice());}
 @Transactional public String updateCover(UUID id,UUID owner,String fileName,String contentType){Edition e=findOwned(id,owner);String previous=e.getCoverFileName();e.updateCover(fileName,contentType);return previous;}
 @Transactional public void delete(UUID id,UUID owner){repo.delete(findOwned(id,owner));}
 @Transactional(readOnly=true) public EditionView viewOwned(UUID id,UUID owner){return EditionView.from(findOwned(id,owner));}
 @Transactional public UUID createId(UUID owner,Command command){return create(owner,command).getId();}
 @Transactional(readOnly=true) public Page<EditionView> listViews(UUID owner,String q,Pageable p){return list(owner,q,p).map(EditionView::from);}
 @Transactional(readOnly=true) public Page<EditionView> search(UUID owner,CatalogFilter filter,CatalogOrder order,int page){
     if(filter.publisherId()!=null)publishers.findOwned(filter.publisherId(),owner);
     if(filter.seriesId()!=null)series.findOwned(filter.seriesId(),owner);
     return repo.findAll(br.com.minhagibiteca.catalog.infrastructure.EditionSpecifications.ownedCatalog(owner,filter),PageRequest.of(Math.max(0,page),20,order.sort())).map(EditionView::from);
 }
 @Transactional(readOnly=true) public List<String> formats(UUID owner){return repo.formats(owner);}
 private void validate(UUID o,Command c){if(c.publisherId()!=null)publishers.findOwned(c.publisherId(),o);if(c.seriesId()!=null)series.findOwned(c.seriesId(),o);}
 public record Command(UUID publisherId,UUID seriesId,String title,String editorialNumber,String variant,String isbn,Integer publicationYear,String format,String notes,br.com.minhagibiteca.catalog.domain.MediaType mediaType,java.math.BigDecimal coverPrice,java.math.BigDecimal referencePaidPrice){
     public Command(UUID publisherId,UUID seriesId,String title,String editorialNumber,String variant,String isbn,Integer publicationYear,String format,String notes){this(publisherId,seriesId,title,editorialNumber,variant,isbn,publicationYear,format,notes,null,null,null);}
 }
}
