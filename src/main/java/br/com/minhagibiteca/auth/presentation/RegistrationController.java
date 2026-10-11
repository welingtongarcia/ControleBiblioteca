package br.com.minhagibiteca.auth.presentation;

import br.com.minhagibiteca.auth.application.UserPrincipal;
import br.com.minhagibiteca.auth.application.UserRegistrationService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegistrationController {
    private final UserRegistrationService registrations;
    public RegistrationController(UserRegistrationService registrations) { this.registrations = registrations; }
    @InitBinder("registrationForm")
    void bind(WebDataBinder binder) { binder.setAllowedFields("email", "password", "passwordConfirmation"); }

    @GetMapping("/cadastro")
    String form(@AuthenticationPrincipal UserPrincipal current, Model model) {
        if (current != null) return "redirect:/dashboard";
        model.addAttribute("registrationForm", new RegistrationForm());
        return "auth/register";
    }

    @PostMapping("/cadastro")
    String register(@AuthenticationPrincipal UserPrincipal current,
                    @Valid @ModelAttribute("registrationForm") RegistrationForm form, BindingResult errors,
                    Model model, RedirectAttributes flash) {
        if (current != null) { form.clearPasswords(); return "redirect:/dashboard"; }
        if (!errors.hasErrors()) {
            try {
                registrations.register(new UserRegistrationService.Command(form.getEmail(), form.getPassword()));
                form.clearPasswords();
                flash.addFlashAttribute("registrationSuccess", "Conta criada com sucesso! Entre com seu e-mail e senha para começar sua coleção.");
                return "redirect:/login";
            } catch (UserRegistrationService.RegistrationUnavailableException conflict) {
                errors.rejectValue("email", "unavailable", conflict.getMessage());
            }
        }
        form.clearPasswords();
        // Não devolver senhas rejeitadas no BindingResult do modelo.
        var safeErrors = new BeanPropertyBindingResult(form, "registrationForm");
        for (var error : errors.getAllErrors()) {
            if (error instanceof FieldError field) safeErrors.addError(new FieldError("registrationForm", field.getField(), field.getDefaultMessage()));
            else safeErrors.addError(new ObjectError("registrationForm", error.getDefaultMessage()));
        }
        model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "registrationForm", safeErrors);
        return "auth/register";
    }
}
