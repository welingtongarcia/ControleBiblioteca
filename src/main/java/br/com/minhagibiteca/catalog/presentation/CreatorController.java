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
    private final CreatorService creators; private final CurrentAuthenticatedUser user;
    public CreatorController(CreatorService creators, CurrentAuthenticatedUser user) { this.creators = creators; this.user = user; }
    @GetMapping("/criadores") String list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page, Model model) { model.addAttribute("creators", creators.list(user.id(), q, PageRequest.of(Math.max(0, page), 20, Sort.by("normalizedName")))); model.addAttribute("query", q); return "criadores/list"; }
    @GetMapping("/criadores/novo") String newForm(Model model) { model.addAttribute("creatorForm", new CreatorForm()); return form(model, "Novo criador", "/criadores"); }
    @PostMapping("/criadores") String create(@Valid @ModelAttribute CreatorForm form, BindingResult result, Model model, RedirectAttributes attributes) { if (result.hasErrors()) return form(model, "Novo criador", "/criadores"); try { creators.create(user.id(), command(form)); } catch (CreatorService.DuplicateCreatorException e) { result.rejectValue("name", "duplicate", "Já existe um criador com este nome."); return form(model, "Novo criador", "/criadores"); } attributes.addFlashAttribute("successMessage", "Criador cadastrado com sucesso."); return "redirect:/criadores"; }
    @GetMapping("/criadores/{id}/editar") String edit(@PathVariable UUID id, Model model) { Creator creator = creators.findOwned(id, user.id()); CreatorForm form = new CreatorForm(); form.setName(creator.getName()); form.setSortName(creator.getSortName()); model.addAttribute("creatorForm", form); return form(model, "Editar criador", "/criadores/" + id + "/editar"); }
    @PostMapping("/criadores/{id}/editar") String update(@PathVariable UUID id, @Valid @ModelAttribute CreatorForm form, BindingResult result, Model model, RedirectAttributes attributes) { if (result.hasErrors()) return form(model, "Editar criador", "/criadores/" + id + "/editar"); try { creators.update(id, user.id(), command(form)); } catch (CreatorService.DuplicateCreatorException e) { result.rejectValue("name", "duplicate", "Já existe um criador com este nome."); return form(model, "Editar criador", "/criadores/" + id + "/editar"); } attributes.addFlashAttribute("successMessage", "Criador atualizado com sucesso."); return "redirect:/criadores"; }
    @PostMapping("/criadores/{id}/excluir") String delete(@PathVariable UUID id, RedirectAttributes attributes) { creators.delete(id, user.id()); attributes.addFlashAttribute("successMessage", "Criador excluído com sucesso."); return "redirect:/criadores"; }
    private String form(Model model, String title, String action) { model.addAttribute("pageTitle", title); model.addAttribute("formAction", action); return "criadores/form"; }
    private CreatorService.Command command(CreatorForm form) { return new CreatorService.Command(form.getName(), form.getSortName()); }
}
