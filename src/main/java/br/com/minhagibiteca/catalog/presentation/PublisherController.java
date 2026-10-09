package br.com.minhagibiteca.catalog.presentation;

import br.com.minhagibiteca.auth.application.CurrentAuthenticatedUser;
import br.com.minhagibiteca.catalog.application.PublisherService;
import br.com.minhagibiteca.catalog.application.PublisherService.DuplicatePublisherException;
import br.com.minhagibiteca.catalog.domain.Publisher;
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
public class PublisherController {

    private final PublisherService publisherService;
    private final CurrentAuthenticatedUser currentAuthenticatedUser;

    public PublisherController(PublisherService publisherService, CurrentAuthenticatedUser currentAuthenticatedUser) {
        this.publisherService = publisherService;
        this.currentAuthenticatedUser = currentAuthenticatedUser;
    }

    @GetMapping("/editoras")
    String list(@RequestParam(defaultValue = "") String q,
                @RequestParam(defaultValue = "0") int page,
                Model model) {
        int safePage = Math.max(page, 0);
        model.addAttribute("publishers", publisherService.list(currentAuthenticatedUser.id(), q,
                PageRequest.of(safePage, 20, Sort.by("normalizedName").ascending())));
        model.addAttribute("query", q);
        return "editoras/list";
    }

    @GetMapping("/editoras/nova")
    String newForm(Model model) {
        model.addAttribute("publisherForm", new PublisherForm());
        model.addAttribute("pageTitle", "Nova editora");
        model.addAttribute("formAction", "/editoras");
        return "editoras/form";
    }

    @PostMapping("/editoras")
    String create(@Valid @ModelAttribute PublisherForm publisherForm, BindingResult bindingResult,
                  Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return formWithMetadata(model, "Nova editora", "/editoras");
        }
        try {
            publisherService.create(currentAuthenticatedUser.id(), command(publisherForm));
        } catch (DuplicatePublisherException exception) {
            bindingResult.rejectValue("name", "duplicate", "Já existe uma editora com este nome na sua coleção.");
            return formWithMetadata(model, "Nova editora", "/editoras");
        }
        redirectAttributes.addFlashAttribute("successMessage", "Editora cadastrada com sucesso.");
        return "redirect:/editoras";
    }

    @GetMapping("/editoras/{id}/editar")
    String editForm(@PathVariable UUID id, Model model) {
        Publisher publisher = publisherService.findOwned(id, currentAuthenticatedUser.id());
        PublisherForm form = new PublisherForm();
        form.setName(publisher.getName());
        form.setCountryCode(publisher.getCountryCode());
        form.setNotes(publisher.getNotes());
        model.addAttribute("publisherForm", form);
        model.addAttribute("pageTitle", "Editar editora");
        model.addAttribute("formAction", "/editoras/" + id + "/editar");
        return "editoras/form";
    }

    @PostMapping("/editoras/{id}/editar")
    String update(@PathVariable UUID id, @Valid @ModelAttribute PublisherForm publisherForm, BindingResult bindingResult,
                  Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return formWithMetadata(model, "Editar editora", "/editoras/" + id + "/editar");
        }
        try {
            publisherService.update(id, currentAuthenticatedUser.id(), command(publisherForm));
        } catch (DuplicatePublisherException exception) {
            bindingResult.rejectValue("name", "duplicate", "Já existe uma editora com este nome na sua coleção.");
            return formWithMetadata(model, "Editar editora", "/editoras/" + id + "/editar");
        }
        redirectAttributes.addFlashAttribute("successMessage", "Editora atualizada com sucesso.");
        return "redirect:/editoras";
    }

    @PostMapping("/editoras/{id}/excluir")
    String delete(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        publisherService.delete(id, currentAuthenticatedUser.id());
        redirectAttributes.addFlashAttribute("successMessage", "Editora excluída com sucesso.");
        return "redirect:/editoras";
    }

    private String formWithMetadata(Model model, String title, String action) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("formAction", action);
        return "editoras/form";
    }

    private PublisherService.PublisherCommand command(PublisherForm form) {
        return new PublisherService.PublisherCommand(form.getName(), form.getCountryCode(), form.getNotes());
    }
}
