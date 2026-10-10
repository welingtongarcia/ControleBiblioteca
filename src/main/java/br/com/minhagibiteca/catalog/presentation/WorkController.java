package br.com.minhagibiteca.catalog.presentation;

import br.com.minhagibiteca.auth.application.CurrentAuthenticatedUser;
import br.com.minhagibiteca.catalog.application.WorkService;
import br.com.minhagibiteca.catalog.domain.Work;
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
public class WorkController {
    private final WorkService works; private final CurrentAuthenticatedUser user;
    public WorkController(WorkService works, CurrentAuthenticatedUser user) { this.works = works; this.user = user; }
    @GetMapping("/obras") String list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("works", works.list(user.id(), q, PageRequest.of(Math.max(0, page), 20, Sort.by("normalizedTitle")))); model.addAttribute("query", q); return "obras/list";
    }
    @GetMapping("/obras/nova") String newForm(Model model) { model.addAttribute("workForm", new WorkForm()); return form(model, "Nova obra", "/obras"); }
    @PostMapping("/obras") String create(@Valid @ModelAttribute WorkForm form, BindingResult result, Model model, RedirectAttributes attributes) {
        if (result.hasErrors()) return form(model, "Nova obra", "/obras");
        try { works.create(user.id(), command(form)); } catch (WorkService.DuplicateWorkException e) { result.rejectValue("title", "duplicate", "Já existe uma obra com este título."); return form(model, "Nova obra", "/obras"); }
        attributes.addFlashAttribute("successMessage", "Obra cadastrada com sucesso."); return "redirect:/obras";
    }
    @GetMapping("/obras/{id}/editar") String edit(@PathVariable UUID id, Model model) { Work work = works.findOwned(id, user.id()); WorkForm form = new WorkForm(); form.setTitle(work.getTitle()); form.setOriginalYear(work.getOriginalYear()); form.setSummary(work.getSummary()); model.addAttribute("workForm", form); return form(model, "Editar obra", "/obras/" + id + "/editar"); }
    @PostMapping("/obras/{id}/editar") String update(@PathVariable UUID id, @Valid @ModelAttribute WorkForm form, BindingResult result, Model model, RedirectAttributes attributes) {
        if (result.hasErrors()) return form(model, "Editar obra", "/obras/" + id + "/editar");
        try { works.update(id, user.id(), command(form)); } catch (WorkService.DuplicateWorkException e) { result.rejectValue("title", "duplicate", "Já existe uma obra com este título."); return form(model, "Editar obra", "/obras/" + id + "/editar"); }
        attributes.addFlashAttribute("successMessage", "Obra atualizada com sucesso."); return "redirect:/obras";
    }
    @PostMapping("/obras/{id}/excluir") String delete(@PathVariable UUID id, RedirectAttributes attributes) {
        try { works.delete(id, user.id()); attributes.addFlashAttribute("successMessage", "Obra excluída com sucesso."); }
        catch (br.com.minhagibiteca.catalog.application.ReferencedCatalogException error) { attributes.addFlashAttribute("errorMessage", error.getMessage()); }
        catch (org.springframework.dao.DataIntegrityViolationException error) { attributes.addFlashAttribute("errorMessage", "Não foi possível excluir esta obra por um conflito com dados vinculados. Revise os vínculos nas edições e tente novamente. Nenhuma HQ foi apagada."); }
        return "redirect:/obras";
    }
    private String form(Model model, String title, String action) { model.addAttribute("pageTitle", title); model.addAttribute("formAction", action); return "obras/form"; }
    private WorkService.Command command(WorkForm form) { return new WorkService.Command(form.getTitle(), form.getOriginalYear(), form.getSummary()); }
}
