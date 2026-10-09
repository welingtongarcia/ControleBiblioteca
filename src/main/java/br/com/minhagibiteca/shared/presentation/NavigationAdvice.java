package br.com.minhagibiteca.shared.presentation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice(annotations = Controller.class)
public class NavigationAdvice {
    @ModelAttribute("navigation") NavigationContext navigation(HttpServletRequest request) { return NavigationContext.from(request); }
}
