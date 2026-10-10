package br.com.minhagibiteca.inventory.presentation;

import br.com.minhagibiteca.auth.application.CurrentAuthenticatedUser;
import br.com.minhagibiteca.inventory.application.LocationService;
import br.com.minhagibiteca.inventory.domain.LocationLevel;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LocationController {
    private final LocationService locations;
    private final CurrentAuthenticatedUser user;
    public LocationController(LocationService locations, CurrentAuthenticatedUser user) { this.locations = locations; this.user = user; }

    @GetMapping("/localizacoes")
    String list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("locations", locations.list(user.id(), q, page)); model.addAttribute("query", q); return "localizacoes/list";
    }
    @GetMapping("/localizacoes/nova") String newForm(Model model) {
        model.addAttribute("locationForm", new LocationForm()); return form(model, null);
    }
    @PostMapping("/localizacoes")
    String create(@Valid @ModelAttribute("locationForm") LocationForm input, BindingResult result, Model model, RedirectAttributes flash) {
        if (result.hasErrors()) return form(model, null);
        try { locations.create(user.id(), command(input)); }
        catch (LocationService.RuleViolation error) { result.rejectValue(error.field(), "rule", error.getMessage()); return form(model, null); }
        flash.addFlashAttribute("successMessage", "Localização cadastrada com sucesso."); return "redirect:/localizacoes";
    }
    @GetMapping("/localizacoes/{id}/editar") String edit(@PathVariable UUID id, Model model) {
        var location = locations.find(id, user.id()); var input = new LocationForm();
        input.setName(location.name()); input.setLevel(location.level()); input.setParentId(location.parentId()); input.setNotes(location.notes());
        model.addAttribute("locationForm", input); return form(model, id);
    }
    @PostMapping("/localizacoes/{id}/editar")
    String update(@PathVariable UUID id, @Valid @ModelAttribute("locationForm") LocationForm input, BindingResult result, Model model, RedirectAttributes flash) {
        locations.find(id, user.id());
        if (result.hasErrors()) return form(model, id);
        try { locations.update(id, user.id(), command(input)); }
        catch (LocationService.RuleViolation error) { result.rejectValue(error.field(), "rule", error.getMessage()); return form(model, id); }
        flash.addFlashAttribute("successMessage", "Localização atualizada com sucesso."); return "redirect:/localizacoes";
    }
    @GetMapping("/localizacoes/{id}/excluir") String confirm(@PathVariable UUID id, Model model) {
        model.addAttribute("location", locations.find(id, user.id())); return "localizacoes/delete";
    }
    @PostMapping("/localizacoes/{id}/excluir") String delete(@PathVariable UUID id, @RequestParam(defaultValue = "false") boolean confirmed, RedirectAttributes flash) {
        locations.find(id, user.id());
        if (!confirmed) return "redirect:/localizacoes/" + id + "/excluir";
        try { locations.delete(id, user.id()); flash.addFlashAttribute("successMessage", "Localização excluída com sucesso."); }
        catch (LocationService.RuleViolation error) { flash.addFlashAttribute("errorMessage", error.getMessage()); }
        catch (org.springframework.dao.DataIntegrityViolationException error) { flash.addFlashAttribute("errorMessage", "Não foi possível excluir esta localização por um conflito com dados vinculados. Revise as localizações internas e os exemplares e tente novamente."); }
        return "redirect:/localizacoes";
    }
    private String form(Model model, UUID id) {
        model.addAttribute("pageTitle", id == null ? "Nova localização" : "Editar localização");
        model.addAttribute("formAction", id == null ? "/localizacoes" : "/localizacoes/" + id + "/editar");
        model.addAttribute("levels", LocationLevel.values()); model.addAttribute("parents", locations.options(user.id(), id)); return "localizacoes/form";
    }
    private LocationService.Command command(LocationForm input) { return new LocationService.Command(input.getParentId(), input.getLevel(), input.getName(), input.getNotes()); }
}
