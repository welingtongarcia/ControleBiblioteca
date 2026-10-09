package br.com.minhagibiteca.catalog.presentation;

import br.com.minhagibiteca.auth.application.CurrentAuthenticatedUser;
import br.com.minhagibiteca.catalog.application.ComicSeriesService;
import br.com.minhagibiteca.catalog.application.ComicSeriesService.DuplicateSeriesException;
import br.com.minhagibiteca.catalog.domain.ComicSeries;
import br.com.minhagibiteca.catalog.domain.SeriesStatus;
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
public class ComicSeriesController {
    private final ComicSeriesService seriesService;
    private final CurrentAuthenticatedUser currentUser;
    private final br.com.minhagibiteca.catalog.application.PublisherService publisherService;
    private final br.com.minhagibiteca.collection.application.ReadingService reading;

    public ComicSeriesController(ComicSeriesService seriesService, CurrentAuthenticatedUser currentUser,
                                 br.com.minhagibiteca.catalog.application.PublisherService publisherService,
                                 br.com.minhagibiteca.collection.application.ReadingService reading) {
        this.seriesService = seriesService; this.currentUser = currentUser; this.publisherService = publisherService; this.reading = reading;
    }

    @GetMapping("/series")
    String list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page, Model model) {
        var result = seriesService.list(currentUser.id(), q, PageRequest.of(Math.max(0, page), 20, Sort.by("normalizedName")));
        model.addAttribute("seriesPage", result);
        model.addAttribute("readingBySeries", reading.seriesPage(currentUser.id(), result.getContent().stream().map(ComicSeries::getId).toList()));
        model.addAttribute("query", q);
        return "series/list";
    }

    @GetMapping("/series/nova") String newForm(Model model) {
        model.addAttribute("seriesForm", new ComicSeriesForm()); return populateForm(model, "Nova série", "/series");
    }

    @PostMapping("/series") String create(@Valid @ModelAttribute("seriesForm") ComicSeriesForm form, BindingResult result,
                                             Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) return populateForm(model, "Nova série", "/series");
        try { seriesService.create(currentUser.id(), command(form)); }
        catch (DuplicateSeriesException exception) { result.rejectValue("name", "duplicate", "Já existe uma série com este nome."); return populateForm(model, "Nova série", "/series"); }
        redirect.addFlashAttribute("successMessage", "Série cadastrada com sucesso."); return "redirect:/series";
    }

    @GetMapping("/series/{id}/editar") String editForm(@PathVariable UUID id, Model model) {
        ComicSeries series = seriesService.findOwned(id, currentUser.id()); ComicSeriesForm form = new ComicSeriesForm();
        form.setPublisherId(series.getPublisherId()); form.setName(series.getName()); form.setStartYear(series.getStartYear());
        form.setEndYear(series.getEndYear()); form.setStatus(series.getStatus()); form.setNotes(series.getNotes());
        model.addAttribute("seriesForm", form); return populateForm(model, "Editar série", "/series/" + id + "/editar");
    }

    @PostMapping("/series/{id}/editar") String update(@PathVariable UUID id, @Valid @ModelAttribute("seriesForm") ComicSeriesForm form,
                                                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) return populateForm(model, "Editar série", "/series/" + id + "/editar");
        try { seriesService.update(id, currentUser.id(), command(form)); }
        catch (DuplicateSeriesException exception) { result.rejectValue("name", "duplicate", "Já existe uma série com este nome."); return populateForm(model, "Editar série", "/series/" + id + "/editar"); }
        redirect.addFlashAttribute("successMessage", "Série atualizada com sucesso."); return "redirect:/series";
    }

    @PostMapping("/series/{id}/excluir") String delete(@PathVariable UUID id, RedirectAttributes redirect) {
        try { seriesService.delete(id, currentUser.id()); redirect.addFlashAttribute("successMessage", "Série excluída com sucesso."); }
        catch (ComicSeriesService.ReferencedSeriesException error) { redirect.addFlashAttribute("errorMessage", error.getMessage()); }
        return "redirect:/series";
    }

    private String populateForm(Model model, String title, String action) {
        model.addAttribute("pageTitle", title); model.addAttribute("formAction", action);
        model.addAttribute("publishers", publisherService.options(currentUser.id())); model.addAttribute("statuses", SeriesStatus.values());
        return "series/form";
    }
    private ComicSeriesService.Command command(ComicSeriesForm form) {
        return new ComicSeriesService.Command(form.getPublisherId(), form.getName(), form.getStartYear(), form.getEndYear(), form.getStatus(), form.getNotes());
    }
}
