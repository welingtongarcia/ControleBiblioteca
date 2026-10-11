package br.com.minhagibiteca.catalog.presentation;

import br.com.minhagibiteca.auth.application.CurrentAuthenticatedUser;
import br.com.minhagibiteca.catalog.application.EditionRatingService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EditionRatingController {
    private final EditionRatingService ratings;
    private final CurrentAuthenticatedUser user;
    public EditionRatingController(EditionRatingService ratings, CurrentAuthenticatedUser user) {
        this.ratings = ratings; this.user = user;
    }
    @InitBinder("editionRatingForm")
    void bind(WebDataBinder binder) { binder.setAllowedFields("rating"); }

    @PostMapping("/edicoes/{id}/avaliacao")
    String rate(@PathVariable UUID id, @Valid @ModelAttribute("editionRatingForm") EditionRatingForm form,
                BindingResult errors, RedirectAttributes flash) {
        ratings.assertOwned(id, user.id());
        if (errors.hasErrors()) flash.addFlashAttribute("errorMessage", "Informe uma nota inteira de 0 a 5 estrelas.");
        else {
            ratings.rate(id, user.id(), form.getRating());
            flash.addFlashAttribute("successMessage", "Avaliação salva: " + form.getRating() + " de 5 estrelas.");
        }
        return "redirect:/edicoes/" + id;
    }

    @PostMapping("/edicoes/{id}/avaliacao/remover")
    String clear(@PathVariable UUID id, RedirectAttributes flash) {
        ratings.rate(id, user.id(), null);
        flash.addFlashAttribute("successMessage", "Avaliação removida. A edição ficou sem avaliação.");
        return "redirect:/edicoes/" + id;
    }
}
