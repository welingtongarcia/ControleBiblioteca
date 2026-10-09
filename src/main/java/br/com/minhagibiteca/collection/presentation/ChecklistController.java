package br.com.minhagibiteca.collection.presentation;
import br.com.minhagibiteca.auth.application.CurrentAuthenticatedUser;
import br.com.minhagibiteca.catalog.application.ComicSeriesService;
import br.com.minhagibiteca.collection.application.ChecklistService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ChecklistController {
    private final ChecklistService checklist;
    private final ComicSeriesService series;
    private final CurrentAuthenticatedUser user;
    private final br.com.minhagibiteca.collection.application.ReadingService reading;
    public ChecklistController(ChecklistService checklist, ComicSeriesService series, CurrentAuthenticatedUser user, br.com.minhagibiteca.collection.application.ReadingService reading) {
        this.checklist = checklist; this.series = series; this.user = user; this.reading = reading;
    }
    @InitBinder("checklistForm") void bind(WebDataBinder binder) { binder.setAllowedFields("editorialNumber", "label", "owned", "notes"); }
    @GetMapping("/series/{id}/checklist") String list(@PathVariable UUID id, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("checklistForm", new ChecklistForm()); return list(model, id, false, page);
    }
    @GetMapping("/series/{id}/faltantes") String missing(@PathVariable UUID id, @RequestParam(defaultValue = "0") int page, Model model) { return list(model, id, true, page); }
    @PostMapping("/series/{id}/checklist") String create(@PathVariable UUID id, @Valid @ModelAttribute("checklistForm") ChecklistForm input,
                                                       BindingResult result, Model model, RedirectAttributes flash) {
        series.viewOwned(id, user.id());
        if (result.hasErrors()) return list(model, id, false, 0);
        try { checklist.create(id, user.id(), command(input)); }
        catch (ChecklistService.RuleViolation error) { result.rejectValue(error.field(), "rule", error.getMessage()); return list(model, id, false, 0); }
        flash.addFlashAttribute("successMessage", "Item adicionado ao checklist."); return back(id);
    }
    @GetMapping("/checklist/itens/{id}/editar") String edit(@PathVariable UUID id, Model model) {
        var item = checklist.find(id, user.id()); var input = new ChecklistForm();
        input.setEditorialNumber(item.editorialNumber()); input.setLabel(item.label()); input.setOwned(item.owned()); input.setNotes(item.notes());
        model.addAttribute("checklistForm", input); return form(model, item.seriesId(), id);
    }
    @PostMapping("/checklist/itens/{id}/editar") String update(@PathVariable UUID id, @Valid @ModelAttribute("checklistForm") ChecklistForm input,
                                                             BindingResult result, Model model, RedirectAttributes flash) {
        var item = checklist.find(id, user.id());
        if (result.hasErrors()) return form(model, item.seriesId(), id);
        try { checklist.update(id, user.id(), command(input)); }
        catch (ChecklistService.RuleViolation error) { result.rejectValue(error.field(), "rule", error.getMessage()); return form(model, item.seriesId(), id); }
        flash.addFlashAttribute("successMessage", "Item do checklist atualizado."); return back(item.seriesId());
    }
    @GetMapping("/checklist/itens/{id}/excluir") String confirm(@PathVariable UUID id, Model model) {
        var item = checklist.find(id, user.id()); model.addAttribute("item", item); model.addAttribute("series", series.viewOwned(item.seriesId(), user.id())); return "checklist/delete";
    }
    @PostMapping("/checklist/itens/{id}/excluir") String delete(@PathVariable UUID id, @RequestParam(defaultValue = "false") boolean confirmed, RedirectAttributes flash) {
        checklist.find(id, user.id());
        if (!confirmed) return "redirect:/checklist/itens/" + id + "/excluir";
        UUID seriesId = checklist.delete(id, user.id()); flash.addFlashAttribute("successMessage", "Item removido do checklist."); return back(seriesId);
    }
    private String list(Model model, UUID id, boolean missing, int page) {
        model.addAttribute("series", series.viewOwned(id, user.id())); model.addAttribute("items", checklist.list(id, user.id(), missing, page));
        model.addAttribute("readingProgress", reading.series(id, user.id()));
        model.addAttribute("missingOnly", missing); model.addAttribute("pageTitle", missing ? "Quadrinhos faltantes" : "Checklist da série");
        model.addAttribute("listPath", "/series/" + id + (missing ? "/faltantes" : "/checklist"));
        model.addAttribute("formAction", "/series/" + id + "/checklist"); return "checklist/list";
    }
    private String form(Model model, UUID seriesId, UUID id) {
        model.addAttribute("series", series.viewOwned(seriesId, user.id())); model.addAttribute("pageTitle", "Editar item do checklist");
        model.addAttribute("formAction", "/checklist/itens/" + id + "/editar"); return "checklist/form";
    }
    private ChecklistService.Command command(ChecklistForm input) { return new ChecklistService.Command(input.getEditorialNumber(), input.getLabel(), input.isOwned(), input.getNotes()); }
    private String back(UUID id) { return "redirect:/series/" + id + "/checklist"; }
}
