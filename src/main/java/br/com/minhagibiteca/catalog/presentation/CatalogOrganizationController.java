package br.com.minhagibiteca.catalog.presentation;

import br.com.minhagibiteca.auth.application.CurrentAuthenticatedUser;
import br.com.minhagibiteca.catalog.application.*;
import br.com.minhagibiteca.catalog.domain.MediaType;
import br.com.minhagibiteca.catalog.domain.PublicationType;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CatalogOrganizationController {
    private final CatalogOrganizationService organization;
    private final CurrentAuthenticatedUser user;

    public CatalogOrganizationController(CatalogOrganizationService organization, CurrentAuthenticatedUser user) {
        this.organization = organization;
        this.user = user;
    }

    @GetMapping("/edicoes/pendencias")
    public String pending(Model model) {
        model.addAttribute("pending", organization.pending(user.id()));
        return "edicoes/pending";
    }

    @PostMapping("/edicoes/filtros")
    public String save(@RequestParam String name, @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) UUID publisherId, @RequestParam(required = false) UUID seriesId,
            @RequestParam(defaultValue = "") String format, @RequestParam(required = false) MediaType mediaType,
            @RequestParam(required = false) PublicationType publicationType,
            @RequestParam(defaultValue = "ALL") CatalogReading reading,
            @RequestParam(defaultValue = "NONE") CatalogMissing missing,
            @RequestParam(defaultValue = "TITLE_NUMBER_ASC") CatalogOrder order, RedirectAttributes flash) {
        var filter = new CatalogFilter(q, publisherId, seriesId, format, mediaType, publicationType, reading, missing);
        try {
            organization.save(user.id(), name, filter, order);
            flash.addFlashAttribute("successMessage", "Filtro salvo. Ele acompanhará as alterações do seu catálogo.");
        } catch (IllegalArgumentException error) {
            flash.addFlashAttribute("errorMessage", error.getMessage());
        }
        return "redirect:" + CatalogLinks.url(filter, order);
    }

    @PostMapping("/edicoes/filtros/{id}/excluir")
    public String delete(@PathVariable UUID id, RedirectAttributes flash) {
        organization.delete(user.id(), id);
        flash.addFlashAttribute("successMessage", "Filtro removido.");
        return "redirect:/edicoes";
    }
}
