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
    @PostMapping("/edicoes/{id}/leitura")
    String mark(@PathVariable UUID id, @RequestParam boolean read, @RequestParam(defaultValue = "false") boolean fromCatalog,
                HttpServletRequest request, RedirectAttributes flash) {
        reading.mark(id, user.id(), read);
        flash.addFlashAttribute("successMessage", read ? "Edição marcada como já lida." : "Edição marcada como não lida.");
        return "redirect:" + (fromCatalog ? NavigationContext.from(request).getListReturnUrl() : "/edicoes/" + id);
    }
}
