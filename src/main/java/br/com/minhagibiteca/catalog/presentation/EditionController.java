package br.com.minhagibiteca.catalog.presentation;
import br.com.minhagibiteca.auth.application.CurrentAuthenticatedUser; import br.com.minhagibiteca.inventory.application.CopyService; import br.com.minhagibiteca.catalog.application.*; import br.com.minhagibiteca.catalog.domain.MediaType; import java.math.BigDecimal; import java.beans.PropertyEditorSupport; import org.springframework.web.bind.WebDataBinder; import jakarta.validation.Valid; import java.util.UUID; import org.springframework.core.io.Resource; import org.springframework.data.domain.*; import org.springframework.http.CacheControl; import org.springframework.http.ResponseEntity; import org.springframework.stereotype.Controller; import org.springframework.ui.Model; import org.springframework.validation.BindingResult; import org.springframework.web.bind.annotation.*; import org.springframework.web.multipart.MultipartFile; import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller public class EditionController { private final EditionService editions; private final PublisherService publishers; private final ComicSeriesService series; private final WorkService works; private final CreatorService creators; private final EditionAssociationService associations; private final EditionCoverService covers; private final EditionCatalogViewService catalogViews; private final CopyService copies; private final CurrentAuthenticatedUser user; private final IsbnLookupService isbnLookup; private final CharacterService characters; private final EditionRegistrationService registration; public EditionController(EditionService e,PublisherService p,ComicSeriesService s,WorkService w,CreatorService c,EditionAssociationService a,EditionCoverService coverService,EditionCatalogViewService views,CopyService copyService,CurrentAuthenticatedUser u,IsbnLookupService isbnLookup,CharacterService characters,EditionRegistrationService registration){editions=e;publishers=p;series=s;works=w;creators=c;associations=a;covers=coverService;catalogViews=views;copies=copyService;user=u;this.isbnLookup=isbnLookup;this.characters=characters;this.registration=registration;}

 @InitBinder("editionForm")
 void bindMoney(WebDataBinder binder) {
     binder.setAllowedFields("title", "editorialNumber", "variant", "isbn", "publicationYear", "format", "notes", "mediaType", "coverPrice", "referencePaidPrice", "publisherId", "seriesId", "workIds", "creatorIds", "characterIds");
     binder.registerCustomEditor(BigDecimal.class, new PropertyEditorSupport() {
         @Override public void setAsText(String text) {
             if (text == null || text.isBlank()) { setValue(null); return; }
             String value = text.trim();
             if (!value.matches("-?[0-9]+([.,][0-9]+)?")) throw new IllegalArgumentException("Valor monetário inválido.");
             setValue(new BigDecimal(value.replace(',', '.')));
         }
         @Override public String getAsText() {
             return getValue() == null ? "" : ((BigDecimal) getValue()).toPlainString().replace('.', ',');
         }
     });
 }
 @GetMapping("/edicoes")
 String list(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,
         @RequestParam(required=false) UUID publisherId,@RequestParam(required=false) UUID seriesId,
         @RequestParam(defaultValue="") String format,@RequestParam(required=false) MediaType mediaType,
         @RequestParam(defaultValue="TITLE_ASC") String order,Model model) {
     CatalogFilter filter = new CatalogFilter(q,publisherId,seriesId,format,mediaType);
     CatalogOrder selectedOrder = CatalogOrder.parse(order);
     model.addAttribute("editions",editions.search(user.id(),filter,selectedOrder,page));
     model.addAttribute("query",filter.query()); model.addAttribute("filter",filter);
     model.addAttribute("selectedOrder",selectedOrder); model.addAttribute("orders",CatalogOrder.values());
     model.addAttribute("mediaTypes",MediaType.values()); model.addAttribute("formats",editions.formats(user.id()));
     model.addAttribute("publishers",catalogViews.publishers(user.id()));
     model.addAttribute("seriesOptions",catalogViews.seriesOptions(user.id()));
     return "edicoes/list";
 }
 @GetMapping("/edicoes/nova") String n(Model m){if(!m.containsAttribute("editionForm"))m.addAttribute("editionForm",new EditionForm());return form(m,"Nova edição","/edicoes");}
 @PostMapping("/edicoes/isbn") String lookup(@ModelAttribute("editionForm") EditionForm f, BindingResult r, RedirectAttributes a) {
     a.addFlashAttribute("editionForm", f);
     if (r.hasErrors()) {
         a.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + "editionForm", r);
         a.addFlashAttribute("isbnMessage", "Corrija os campos com valores inválidos antes de buscar. Seus dados foram preservados.");
         return "redirect:/edicoes/nova";
     }
     try {
         var result = isbnLookup.lookup(f.getIsbn());
         if (result.isEmpty()) a.addFlashAttribute("isbnMessage", "Nenhum cadastro encontrado para este ISBN na Open Library ou no Google Books. Continue preenchendo manualmente.");
         else {
             var found = result.get(); f.setIsbn(found.isbn());
             if (f.getTitle() == null || f.getTitle().isBlank()) f.setTitle(found.title());
             if (f.getPublicationYear() == null) f.setPublicationYear(found.year());
             if (f.getPublisherId() == null && found.publishers().size() == 1) f.setPublisherId(uniqueName(catalogViews.publishers(user.id()), found.publishers().getFirst()));
             if (f.getCreatorIds() == null || f.getCreatorIds().isEmpty()) {
                 var options = catalogViews.creators(user.id()); var ids = new java.util.LinkedHashSet<UUID>();
                 for (String author : found.authors()) { UUID id = uniqueName(options, author); if (id != null) ids.add(id); }
                 f.setCreatorIds(java.util.List.copyOf(ids));
             }
             a.addFlashAttribute("isbnMetadata", found);
             a.addFlashAttribute("isbnMessage", "Dados encontrados. Preenchemos apenas campos vazios; revise as sugestões e salve quando estiver tudo certo.");
         }
     } catch (IsbnLookupService.LookupException error) { a.addFlashAttribute("isbnMessage", error.getMessage()); }
     return "redirect:/edicoes/nova";
 }
 private UUID uniqueName(java.util.List<EditionCatalogViewService.NamedOption> options, String name) {
     var matches = options.stream().filter(o -> o.name().strip().equalsIgnoreCase(name.strip())).toList();
     return matches.size() == 1 ? matches.getFirst().id() : null;
 }
 @PostMapping("/edicoes") String c(@Valid @ModelAttribute("editionForm")EditionForm f,BindingResult r,Model m,RedirectAttributes a){if(r.hasErrors())return form(m,"Nova edição","/edicoes");registration.save(user.id(),null,cmd(f),f.getWorkIds(),f.getCreatorIds(),f.getCharacterIds());a.addFlashAttribute("successMessage","Edição cadastrada com sucesso.");return "redirect:/edicoes";}
 @GetMapping("/edicoes/{id}") String detail(@PathVariable UUID id,@RequestParam(defaultValue="0") int copyPage,Model m){EditionView edition=editions.viewOwned(id,user.id());m.addAttribute("edition",edition);m.addAttribute("copies",copies.list(id,user.id(),copyPage));m.addAttribute("canAddCopy",edition.mediaType()!=MediaType.DIGITAL);m.addAttribute("publisherName",catalogViews.publisherName(edition.publisherId(),user.id()));m.addAttribute("seriesName",catalogViews.seriesName(edition.seriesId(),user.id()));m.addAttribute("works",catalogViews.editionWorks(id,user.id()));m.addAttribute("creators",catalogViews.editionCreators(id,user.id()));m.addAttribute("characters",characters.edition(user.id(),id));return "edicoes/detail";}
 @PostMapping("/edicoes/{id}/capa") String cover(@PathVariable UUID id,@RequestParam("cover") MultipartFile file,RedirectAttributes a){try{covers.replace(id,user.id(),file);a.addFlashAttribute("successMessage","Capa atualizada com sucesso.");}catch(EditionCoverService.InvalidCoverException e){a.addFlashAttribute("errorMessage",e.getMessage());}return "redirect:/edicoes/"+id;}
 @GetMapping("/edicoes/{id}/capa") ResponseEntity<Resource> cover(@PathVariable UUID id){EditionCoverService.Cover cover=covers.load(id,user.id());return ResponseEntity.ok().contentType(org.springframework.http.MediaType.parseMediaType(cover.contentType())).cacheControl(CacheControl.noStore()).body(cover.resource());}
 @GetMapping("/edicoes/{id}/editar") String e(@PathVariable UUID id,Model m){EditionView x=editions.viewOwned(id,user.id()); EditionForm f=new EditionForm();f.setPublisherId(x.publisherId());f.setSeriesId(x.seriesId());f.setTitle(x.title());f.setEditorialNumber(x.editorialNumber());f.setVariant(x.variant());f.setIsbn(x.isbn());f.setPublicationYear(x.publicationYear());f.setFormat(x.format());f.setNotes(x.notes());f.setMediaType(x.mediaType());f.setCoverPrice(x.coverPrice());f.setReferencePaidPrice(x.referencePaidPrice());f.setWorkIds(associations.workIds(id));f.setCreatorIds(associations.creatorIds(id));f.setCharacterIds(characters.ids(user.id(),id));m.addAttribute("editionForm",f);return form(m,"Editar edição","/edicoes/"+id+"/editar",id);}
 @PostMapping("/edicoes/{id}/editar") String u(@PathVariable UUID id,@Valid @ModelAttribute("editionForm")EditionForm f,BindingResult r,Model m,RedirectAttributes a){editions.viewOwned(id,user.id());if(r.hasErrors())return form(m,"Editar edição","/edicoes/"+id+"/editar",id);try{registration.save(user.id(),id,cmd(f),f.getWorkIds(),f.getCreatorIds(),f.getCharacterIds());}catch(EditionService.ReferencedEditionException error){r.rejectValue("mediaType","rule",error.getMessage());return form(m,"Editar edição","/edicoes/"+id+"/editar",id);}a.addFlashAttribute("successMessage","Edição atualizada com sucesso.");return "redirect:/edicoes/"+id;}
 @PostMapping("/edicoes/{id}/excluir") String d(@PathVariable UUID id,RedirectAttributes a){try{editions.delete(id,user.id());a.addFlashAttribute("successMessage","Edição excluída com sucesso.");}catch(EditionService.ReferencedEditionException error){a.addFlashAttribute("errorMessage",error.getMessage());}return "redirect:/edicoes";}
 private String form(Model m,String title,String action,UUID editionId){m.addAttribute("editionId",editionId);return form(m,title,action);}
 private String form(Model m,String title,String action){m.addAttribute("mediaTypes",MediaType.values());m.addAttribute("pageTitle",title);m.addAttribute("formAction",action);m.addAttribute("publishers",catalogViews.publishers(user.id()));m.addAttribute("seriesPage",catalogViews.series(user.id()));m.addAttribute("works",catalogViews.works(user.id()));m.addAttribute("creators",catalogViews.creators(user.id()));m.addAttribute("characters",characters.options(user.id()));return "edicoes/form";}
 private EditionService.Command cmd(EditionForm f){return new EditionService.Command(f.getPublisherId(),f.getSeriesId(),f.getTitle(),f.getEditorialNumber(),f.getVariant(),f.getIsbn(),f.getPublicationYear(),f.getFormat(),f.getNotes(),f.getMediaType(),f.getCoverPrice(),f.getReferencePaidPrice());}}
