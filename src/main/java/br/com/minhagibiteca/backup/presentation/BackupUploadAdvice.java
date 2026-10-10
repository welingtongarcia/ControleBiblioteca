package br.com.minhagibiteca.backup.presentation;

import br.com.minhagibiteca.shared.presentation.NavigationContext;
import jakarta.servlet.http.*;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** A resolução multipart pode falhar antes de existir um handler MVC selecionado. */
@ControllerAdvice
public class BackupUploadAdvice {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String tooLarge(HttpServletRequest request,HttpServletResponse response,Model model,RedirectAttributes flash) {
        if (request.getRequestURI().equals(request.getContextPath()+"/backup/restaurar")) {
            flash.addFlashAttribute("backupError","O ZIP excede o limite de 250 MB.");return "redirect:/backup";
        }
        response.setStatus(413);model.addAttribute("navigation",NavigationContext.from(request));return "errors/upload";
    }
}
