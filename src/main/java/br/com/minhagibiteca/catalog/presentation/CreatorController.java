package br.com.minhagibiteca.catalog.presentation;

import br.com.minhagibiteca.auth.application.CurrentAuthenticatedUser;
import br.com.minhagibiteca.catalog.application.CreatorService;
import br.com.minhagibiteca.catalog.domain.Creator;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CreatorController {
    @ModelAttribute("creatorFunctions")
    br.com.minhagibiteca.catalog.domain.CreatorFunction[] creatorFunctions() {
        return br.com.minhagibiteca.catalog.domain.CreatorFunction.values();
    }
    private final CreatorService creators; private final CurrentAuthenticatedUser user;
    private final br.com.minhagibiteca.catalog.application.CreatorRegistrationService registration;
    private final br.com.minhagibiteca.catalog.application.CreatorPhotoService photos;
    public CreatorController(CreatorService creators, CurrentAuthenticatedUser user, br.com.minhagibiteca.catalog.application.CreatorRegistrationService registration, br.com.minhagibiteca.catalog.application.CreatorPhotoService photos) { this.creators = creators; this.user = user; this.registration=registration;this.photos=photos; }
    @org.springframework.web.bind.annotation.InitBinder("creatorForm") void bind(org.springframework.web.bind.WebDataBinder binder){binder.setAllowedFields("name","sortName","functions");}
    @GetMapping("/criadores/{id}/foto") org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> photo(@PathVariable UUID id){var photo=photos.load(id,user.id());return org.springframework.http.ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).contentType(org.springframework.http.MediaType.parseMediaType(photo.contentType())).header("X-Content-Type-Options","nosniff").body(photo.resource());}
    @PostMapping("/criadores/{id}/foto/remover") String removePhoto(@PathVariable UUID id,RedirectAttributes a){photos.remove(id,user.id());a.addFlashAttribute("successMessage","Foto removida com sucesso.");return "redirect:/criadores/"+id+"/editar";}
    @GetMapping("/criadores") String list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page, Model model) { model.addAttribute("creators", creators.list(user.id(), q, PageRequest.of(Math.max(0, page), 20, Sort.by("normalizedName")))); model.addAttribute("query", q); return "criadores/list"; }
    @GetMapping("/criadores/novo") String newForm(Model model) { model.addAttribute("creatorForm", new CreatorForm()); return form(model, "Novo criador", "/criadores"); }
    @PostMapping("/criadores") String create(@Valid @ModelAttribute CreatorForm form, BindingResult result, Model model, RedirectAttributes attributes, @RequestParam(required=false) org.springframework.web.multipart.MultipartFile photo) { if (result.hasErrors()) return form(model, "Novo criador", "/criadores"); try { registration.create(user.id(), command(form),photo); } catch (CreatorService.DuplicateCreatorException e) { result.rejectValue("name", "duplicate", "Já existe um criador com este nome."); return form(model, "Novo criador", "/criadores"); } catch(br.com.minhagibiteca.catalog.application.CreatorPhotoService.InvalidPhotoException e){model.addAttribute("photoError",e.getMessage());return form(model,"Novo criador","/criadores");} attributes.addFlashAttribute("successMessage", "Criador cadastrado com sucesso."); return "redirect:/criadores"; }
    @GetMapping("/criadores/{id}/editar") String edit(@PathVariable UUID id, Model model) { Creator creator = creators.findOwned(id, user.id()); CreatorForm form = new CreatorForm(); form.setName(creator.getName()); form.setSortName(creator.getSortName()); form.setFunctions(creator.getFunctions()); model.addAttribute("creatorForm", form); return form(model, "Editar criador", "/criadores/" + id + "/editar"); }
    @PostMapping("/criadores/{id}/editar") String update(@PathVariable UUID id, @Valid @ModelAttribute CreatorForm form, BindingResult result, Model model, RedirectAttributes attributes, @RequestParam(required=false) org.springframework.web.multipart.MultipartFile photo) { if (result.hasErrors()) return form(model, "Editar criador", "/criadores/" + id + "/editar"); try { registration.update(id, user.id(), command(form),photo); } catch (CreatorService.DuplicateCreatorException e) { result.rejectValue("name", "duplicate", "Já existe um criador com este nome."); return form(model, "Editar criador", "/criadores/" + id + "/editar"); } catch(br.com.minhagibiteca.catalog.application.CreatorPhotoService.InvalidPhotoException e){model.addAttribute("photoError",e.getMessage());return form(model,"Editar criador","/criadores/"+id+"/editar");} attributes.addFlashAttribute("successMessage", "Criador atualizado com sucesso."); return "redirect:/criadores"; }
    @PostMapping("/criadores/{id}/excluir") String delete(@PathVariable UUID id, RedirectAttributes attributes) {
        try { registration.delete(id, user.id()); attributes.addFlashAttribute("successMessage", "Criador excluído com sucesso."); }
        catch (br.com.minhagibiteca.catalog.application.ReferencedCatalogException error) { attributes.addFlashAttribute("errorMessage", error.getMessage()); }
        catch (org.springframework.dao.DataIntegrityViolationException error) { attributes.addFlashAttribute("errorMessage", "Não foi possível excluir este criador por um conflito com dados vinculados. Revise os vínculos nas edições e tente novamente. Nenhuma edição foi apagada."); }
        return "redirect:/criadores";
    }
    private String form(Model model, String title, String action) { if(action.matches("/criadores/[0-9a-f-]+/editar")){UUID id=UUID.fromString(action.split("/")[2]);model.addAttribute("creatorId",id);model.addAttribute("photoPresent",creators.findOwned(id,user.id()).getPhotoFileName()!=null);} model.addAttribute("pageTitle", title); model.addAttribute("formAction", action); return "criadores/form"; }
    private CreatorService.Command command(CreatorForm form) { return new CreatorService.Command(form.getName(), form.getSortName(), form.getFunctions()); }
}
