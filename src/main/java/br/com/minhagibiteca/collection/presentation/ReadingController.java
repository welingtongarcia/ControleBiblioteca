package br.com.minhagibiteca.collection.presentation;

import br.com.minhagibiteca.auth.application.CurrentAuthenticatedUser;
import br.com.minhagibiteca.collection.application.ReadingService;
import br.com.minhagibiteca.shared.presentation.NavigationContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReadingController {
    private final ReadingService reading;
    private final CurrentAuthenticatedUser user;
    public ReadingController(ReadingService reading, CurrentAuthenticatedUser user) { this.reading = reading; this.user = user; }
    @org.springframework.web.bind.annotation.InitBinder("pageReadingForm")
    void bindPages(org.springframework.web.bind.WebDataBinder binder) { binder.setAllowedFields("pagesRead"); }
    @PostMapping("/edicoes/{id}/paginas")
    String pages(@PathVariable UUID id, @jakarta.validation.Valid @ModelAttribute("pageReadingForm") PageReadingForm form,
                 org.springframework.validation.BindingResult errors, RedirectAttributes flash) {
        // Autorizar mesmo quando a entrada é inválida, antes de redirecionar/expor erro.
        reading.assertOwned(id, user.id());
        if (errors.hasErrors()) flash.addFlashAttribute("errorMessage", "Informe páginas lidas entre zero e o total cadastrado, usando um número inteiro.");
        else try {
            reading.updatePages(id, user.id(), form.getPagesRead());
            flash.addFlashAttribute("successMessage", "Progresso de páginas atualizado.");
        } catch (IllegalArgumentException error) { flash.addFlashAttribute("errorMessage", error.getMessage()); }
        return "redirect:/edicoes/" + id;
    }
    @PostMapping("/edicoes/{id}/leitura")
    String mark(@PathVariable UUID id, @RequestParam boolean read, @RequestParam(defaultValue = "false") boolean fromCatalog,
                HttpServletRequest request, RedirectAttributes flash) {
        reading.mark(id, user.id(), read);
        flash.addFlashAttribute("successMessage", read ? "Edição marcada como já lida." : "Edição marcada como não lida.");
        return "redirect:" + (fromCatalog ? NavigationContext.from(request).getListReturnUrl() : "/edicoes/" + id);
    }
}
