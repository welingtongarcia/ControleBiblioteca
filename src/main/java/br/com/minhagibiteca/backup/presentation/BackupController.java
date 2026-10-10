package br.com.minhagibiteca.backup.presentation;

import br.com.minhagibiteca.auth.application.CurrentAuthenticatedUser;
import br.com.minhagibiteca.backup.application.*;
import br.com.minhagibiteca.backup.infrastructure.BackupArchive;
import java.io.IOException;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/backup")
public class BackupController {
    private final BackupService backups;
    private final CurrentAuthenticatedUser user;
    public BackupController(BackupService backups,CurrentAuthenticatedUser user){this.backups=backups;this.user=user;}
    @GetMapping public String index(Model model,HttpServletResponse response) {response.setHeader("Cache-Control","no-store");model.addAttribute("emptyAccount",backups.empty(user.id()));return "backup/index";}
    @PostMapping("/gerar")
    public void generate(HttpServletResponse response) throws IOException {
        try(var download=backups.generate(user.id())) {
            response.setHeader("Cache-Control","no-store");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"minha-gibiteca-backup.zip\"");
            response.setContentType("application/zip");response.setContentLengthLong(java.nio.file.Files.size(download.zip()));
            java.nio.file.Files.copy(download.zip(),response.getOutputStream());
        }
    }
    @ExceptionHandler(InvalidBackupException.class)
    public String invalid(InvalidBackupException error,RedirectAttributes flash){flash.addFlashAttribute("backupError",error.getMessage());return "redirect:/backup";}
    @PostMapping("/restaurar")
    public String restore(@RequestParam("arquivo") MultipartFile file,@RequestParam(defaultValue="false") boolean confirm,RedirectAttributes flash) {
        try {
            if (!confirm)throw new InvalidBackupException("Confirme que deseja restaurar em uma conta vazia.");
            if(file.isEmpty() || file.getSize()>BackupArchive.MAX_ZIP)throw new InvalidBackupException("Selecione um arquivo ZIP de até 250 MB.");
            try(var input=file.getInputStream()) {
                int count=backups.restore(user.id(),input);flash.addFlashAttribute("backupSuccess","Coleção restaurada com sucesso: "+count+" cadastros. Confira seus dados e capas no catálogo.");
            }
        }catch(InvalidBackupException e){flash.addFlashAttribute("backupError",e.getMessage());}
        catch(IOException e){flash.addFlashAttribute("backupError","Não foi possível ler o arquivo enviado. Tente novamente.");}
        return "redirect:/backup";
    }
}
