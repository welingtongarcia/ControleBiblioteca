package br.com.minhagibiteca.digital.presentation;

import br.com.minhagibiteca.auth.application.CurrentAuthenticatedUser;
import br.com.minhagibiteca.catalog.application.*;
import br.com.minhagibiteca.digital.application.OneDriveService;
import br.com.minhagibiteca.digital.domain.OneDriveException;
import br.com.minhagibiteca.digital.infrastructure.OneDriveRepository;
import jakarta.servlet.http.*;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/biblioteca-digital")
public class OneDriveController {
    static final String PENDING="onedrive.pending";
    private final CurrentAuthenticatedUser user;private final OneDriveService drive;private final OneDriveRepository repo;private final EditionService editions;
    public OneDriveController(CurrentAuthenticatedUser user,OneDriveService drive,OneDriveRepository repo,EditionService editions){this.user=user;this.drive=drive;this.repo=repo;this.editions=editions;}
    @ModelAttribute("extensions") public java.util.List<String> extensions(){return OneDriveService.EXTENSIONS;}
    @GetMapping String index(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String extension,@RequestParam(defaultValue="0") int page,Model m,HttpServletResponse response){
        response.setHeader("Cache-Control","no-store");m.addAttribute("driveStatus",drive.status(user.id()));m.addAttribute("rcloneEnabled",drive.rcloneEnabled());m.addAttribute("oauthEnabled",drive.oauthEnabled());m.addAttribute("rcloneAccountId",user.id());m.addAttribute("folders",repo.folders(user.id()));m.addAttribute("files",repo.files(user.id(),q,extension,page));m.addAttribute("query",q);m.addAttribute("selectedExtension",extension);return "digital/index";
    }
    @PostMapping("/rclone/conectar") String connectRclone(RedirectAttributes a){drive.connectRclone(user.id());a.addFlashAttribute("successMessage","OneDrive via rclone ativado. Escolha as pastas e sincronize.");return "redirect:/biblioteca-digital";}
    @PostMapping("/onedrive/conectar") String connect(HttpSession session){var pending=drive.begin(user.id());session.setAttribute(PENDING,pending);return "redirect:"+pending.request().getAuthorizationRequestUri();}
    @GetMapping("/onedrive/retorno") String callback(@RequestParam(required=false) String state,@RequestParam(required=false) String code,@RequestParam(required=false) String error,HttpServletRequest request,RedirectAttributes a,HttpServletResponse response){
        response.setHeader("Cache-Control","no-store");response.setHeader("Referrer-Policy","no-referrer");var session=request.getSession(false);
        if(session==null)throw new OneDriveException("Autorização inválida ou expirada. Use Conectar OneDrive novamente.");
        synchronized(session){
            var pending=(OneDriveService.Pending)session.getAttribute(PENDING);
            if(!drive.valid(pending,user.id(),state))throw new OneDriveException("Autorização inválida ou expirada. Use Conectar OneDrive novamente.");
            session.removeAttribute(PENDING);
            if(error!=null)throw new OneDriveException("A autorização Microsoft não foi concluída. Nenhum arquivo foi acessado.");
            drive.finish(user.id(),pending,code);
        }
        a.addFlashAttribute("successMessage","OneDrive conectado. Escolha as pastas para sua biblioteca digital.");return "redirect:/biblioteca-digital";
    }
    @PostMapping("/onedrive/desconectar") String disconnect(HttpSession session,RedirectAttributes a){synchronized(session){repo.disconnect(user.id());session.removeAttribute(PENDING);}a.addFlashAttribute("successMessage","Conexão e índice local removidos. Arquivos e edições não foram alterados. A autorização externa do rclone, se utilizada, permanece no computador até ser revogada na Microsoft ou removida pelo responsável pela instalação.");return "redirect:/biblioteca-digital";}
    @GetMapping("/pastas") String folders(@RequestParam(defaultValue="root") String folder,@RequestParam(required=false) String next,Model m,HttpServletResponse response){response.setHeader("Cache-Control","no-store");var page=drive.browse(user.id(),folder,next);m.addAttribute("folderId",folder);m.addAttribute("folderName",drive.folderName(user.id(),folder));m.addAttribute("remoteItems",page.items());m.addAttribute("next",page.next());return "digital/folders";}
    @PostMapping("/pastas") String select(@RequestParam String remoteId,RedirectAttributes a){drive.select(user.id(),remoteId);a.addFlashAttribute("successMessage","Pasta selecionada. Clique em Sincronizar para atualizar a listagem.");return "redirect:/biblioteca-digital";}
    @PostMapping("/pastas/{id}/remover") String remove(@PathVariable UUID id,RedirectAttributes a){repo.removeFolder(id,user.id());a.addFlashAttribute("successMessage","Pasta removida da seleção. Sincronize para atualizar a disponibilidade dos arquivos.");return "redirect:/biblioteca-digital";}
    @PostMapping("/sincronizar") String sync(RedirectAttributes a){int count=drive.synchronize(user.id());a.addFlashAttribute("successMessage",count+" arquivos digitais encontrados. Nenhum arquivo remoto foi alterado.");return "redirect:/biblioteca-digital";}
    @GetMapping("/arquivos/{id}/vincular") String linkForm(@PathVariable UUID id,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,Model m){m.addAttribute("file",repo.file(id,user.id()));m.addAttribute("query",q);m.addAttribute("editions",editions.search(user.id(),new CatalogFilter(q,null,null,"",null),CatalogOrder.TITLE_NUMBER_ASC,page));return "digital/link";}
    @PostMapping("/arquivos/{id}/vincular") String link(@PathVariable UUID id,@RequestParam(required=false) UUID editionId,RedirectAttributes a){repo.link(id,user.id(),editionId);a.addFlashAttribute("successMessage",editionId==null?"Vínculo removido.":"Arquivo vinculado à edição. Valores, suporte e leitura não foram alterados.");return "redirect:/biblioteca-digital";}
    @ExceptionHandler(OneDriveException.class) String failure(OneDriveException error,RedirectAttributes a){a.addFlashAttribute("errorMessage",error.getMessage());return "redirect:/biblioteca-digital";}
}
