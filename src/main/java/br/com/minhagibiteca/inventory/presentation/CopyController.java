package br.com.minhagibiteca.inventory.presentation;

import br.com.minhagibiteca.auth.application.CurrentAuthenticatedUser;
import br.com.minhagibiteca.catalog.application.EditionService;
import br.com.minhagibiteca.inventory.application.*;
import br.com.minhagibiteca.inventory.domain.CopyCondition;
import jakarta.validation.Valid;
import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CopyController {
    private final CopyService copies;
    private final EditionService editions;
    private final LocationService locations;
    private final CurrentAuthenticatedUser user;
    public CopyController(CopyService copies, EditionService editions, LocationService locations, CurrentAuthenticatedUser user) {
        this.copies = copies; this.editions = editions; this.locations = locations; this.user = user;
    }
    @InitBinder("copyForm") void bind(WebDataBinder binder) {
        binder.setAllowedFields("storageLocationId", "condition", "conditionNotes", "acquisitionSource", "acquisitionDate", "acquisitionPrice", "notes");
        binder.registerCustomEditor(BigDecimal.class, new PropertyEditorSupport() {
            @Override public void setAsText(String text) {
                if (text == null || text.isBlank()) { setValue(null); return; }
                String value = text.trim();
                if (!value.matches("-?[0-9]+([.,][0-9]+)?")) throw new IllegalArgumentException("Valor monetário inválido.");
                setValue(new BigDecimal(value.replace(',', '.')));
            }
            @Override public String getAsText() { return getValue() == null ? "" : ((BigDecimal) getValue()).toPlainString().replace('.', ','); }
        });
    }
    @GetMapping("/edicoes/{editionId}/exemplares/novo")
    String newForm(@PathVariable UUID editionId, Model model, RedirectAttributes flash) {
        try { copies.validateEdition(editionId, user.id()); }
        catch (CopyService.RuleViolation error) { flash.addFlashAttribute("errorMessage", error.getMessage()); return back(editionId); }
        model.addAttribute("copyForm", new CopyForm()); return form(model, editionId, null);
    }
    @PostMapping("/edicoes/{editionId}/exemplares")
    String create(@PathVariable UUID editionId, @Valid @ModelAttribute("copyForm") CopyForm input, BindingResult result, Model model, RedirectAttributes flash) {
        editions.viewOwned(editionId, user.id());
        if (result.hasErrors()) return form(model, editionId, null);
        try { copies.create(editionId, user.id(), command(input)); }
        catch (CopyService.RuleViolation error) {
            if (error.field().equals("edition")) { flash.addFlashAttribute("errorMessage", error.getMessage()); return back(editionId); }
            result.rejectValue(error.field(), "rule", error.getMessage()); return form(model, editionId, null);
        }
        flash.addFlashAttribute("successMessage", "Exemplar cadastrado com sucesso."); return back(editionId);
    }
    @GetMapping("/exemplares/{id}/editar") String edit(@PathVariable UUID id, Model model) {
        var copy = copies.find(id, user.id()); var input = new CopyForm();
        input.setStorageLocationId(copy.storageLocationId()); input.setCondition(copy.condition()); input.setConditionNotes(copy.conditionNotes());
        input.setAcquisitionSource(copy.acquisitionSource()); input.setAcquisitionDate(copy.acquisitionDate()); input.setAcquisitionPrice(copy.acquisitionPrice()); input.setNotes(copy.notes());
        model.addAttribute("copyForm", input); return form(model, copy.editionId(), id);
    }
    @PostMapping("/exemplares/{id}/editar")
    String update(@PathVariable UUID id, @Valid @ModelAttribute("copyForm") CopyForm input, BindingResult result, Model model, RedirectAttributes flash) {
        var copy = copies.find(id, user.id());
        if (result.hasErrors()) return form(model, copy.editionId(), id);
        try { copies.update(id, user.id(), command(input)); }
        catch (CopyService.RuleViolation error) { result.rejectValue(error.field(), "rule", error.getMessage()); return form(model, copy.editionId(), id); }
        flash.addFlashAttribute("successMessage", "Exemplar atualizado com sucesso."); return back(copy.editionId());
    }
    @GetMapping("/exemplares/{id}/excluir") String confirm(@PathVariable UUID id, Model model) {
        var copy = copies.find(id, user.id()); model.addAttribute("copy", copy);
        model.addAttribute("edition", editions.viewOwned(copy.editionId(), user.id())); return "exemplares/delete";
    }
    @PostMapping("/exemplares/{id}/excluir") String delete(@PathVariable UUID id, @RequestParam(defaultValue = "false") boolean confirmed, RedirectAttributes flash) {
        copies.find(id, user.id());
        if (!confirmed) return "redirect:/exemplares/" + id + "/excluir";
        UUID edition = copies.delete(id, user.id()); flash.addFlashAttribute("successMessage", "Exemplar excluído com sucesso."); return back(edition);
    }
    private String form(Model model, UUID edition, UUID copy) {
        model.addAttribute("edition", editions.viewOwned(edition, user.id()));
        model.addAttribute("pageTitle", copy == null ? "Novo exemplar" : "Editar exemplar");
        model.addAttribute("formAction", copy == null ? "/edicoes/" + edition + "/exemplares" : "/exemplares/" + copy + "/editar");
        model.addAttribute("locations", locations.options(user.id(), null)); model.addAttribute("conditions", CopyCondition.values()); return "exemplares/form";
    }
    private CopyService.Command command(CopyForm input) { return new CopyService.Command(input.getStorageLocationId(), input.getCondition(), input.getConditionNotes(), input.getAcquisitionSource(), input.getAcquisitionDate(), input.getAcquisitionPrice(), input.getNotes()); }
    private String back(UUID edition) { return "redirect:/edicoes/" + edition; }
}
