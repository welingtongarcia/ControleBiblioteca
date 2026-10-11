package br.com.minhagibiteca.wishlist.presentation;

import br.com.minhagibiteca.auth.application.CurrentAuthenticatedUser;
import br.com.minhagibiteca.catalog.domain.PublicationType;
import br.com.minhagibiteca.wishlist.application.WishService;
import br.com.minhagibiteca.wishlist.domain.WishPriority;
import jakarta.validation.Valid;
import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WishController {
    private final WishService wishes; private final CurrentAuthenticatedUser user;
    public WishController(WishService wishes,CurrentAuthenticatedUser user){this.wishes=wishes;this.user=user;}
    @InitBinder("wishForm") void bind(WebDataBinder binder){
        binder.setAllowedFields("title","publicationType","author","publisher","isbn","estimatedPrice","priority","notes");
        binder.registerCustomEditor(BigDecimal.class,new PropertyEditorSupport(){
            @Override public void setAsText(String text){if(text==null||text.isBlank()){setValue(null);return;}String v=text.trim();if(!v.matches("-?[0-9]+([.,][0-9]+)?"))throw new IllegalArgumentException("Valor inválido.");setValue(new BigDecimal(v.replace(',','.')));}
            @Override public String getAsText(){return getValue()==null?"":((BigDecimal)getValue()).toPlainString().replace('.',',');}
        });
    }
    @GetMapping("/desejos") String list(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="PENDING") String status,@RequestParam(defaultValue="0") int page,Model m){
        if(!java.util.Set.of("PENDING","ACQUIRED","ALL").contains(status))status="PENDING";
        m.addAttribute("wishes",wishes.list(user.id(),q,status.equals("ALL")?null:status.equals("ACQUIRED"),page));m.addAttribute("query",q);m.addAttribute("selectedStatus",status);return "desejos/list";
    }
    @GetMapping("/desejos/novo") String newForm(Model m){m.addAttribute("wishForm",new WishForm());return form(m,"Novo desejo","/desejos");}
    @PostMapping("/desejos") String create(@Valid @ModelAttribute("wishForm") WishForm f,BindingResult r,Model m,RedirectAttributes a){if(r.hasErrors())return form(m,"Novo desejo","/desejos");wishes.create(user.id(),command(f));a.addFlashAttribute("successMessage","Desejo cadastrado com sucesso.");return "redirect:/desejos";}
    @GetMapping("/desejos/{id}/editar") String edit(@PathVariable UUID id,Model m){var w=wishes.find(id,user.id());var f=new WishForm();f.setTitle(w.title());f.setPublicationType(w.publicationType());f.setAuthor(w.author());f.setPublisher(w.publisher());f.setIsbn(w.isbn());f.setEstimatedPrice(w.estimatedPrice());f.setPriority(w.priority());f.setNotes(w.notes());m.addAttribute("wishForm",f);return form(m,"Editar desejo","/desejos/"+id+"/editar");}
    @PostMapping("/desejos/{id}/editar") String update(@PathVariable UUID id,@Valid @ModelAttribute("wishForm") WishForm f,BindingResult r,Model m,RedirectAttributes a){wishes.find(id,user.id());if(r.hasErrors())return form(m,"Editar desejo","/desejos/"+id+"/editar");wishes.update(id,user.id(),command(f));a.addFlashAttribute("successMessage","Desejo atualizado com sucesso.");return "redirect:/desejos";}
    @PostMapping("/desejos/{id}/situacao") String acquired(@PathVariable UUID id,@RequestParam boolean acquired,RedirectAttributes a){wishes.acquired(id,user.id(),acquired);a.addFlashAttribute("successMessage",acquired?"Desejo marcado como adquirido. Cadastre a edição no catálogo para registrar a compra e a leitura.":"Desejo devolvido à lista de pendentes.");return "redirect:/desejos";}
    @PostMapping("/desejos/{id}/excluir") String delete(@PathVariable UUID id,RedirectAttributes a){wishes.delete(id,user.id());a.addFlashAttribute("successMessage","Desejo excluído com sucesso.");return "redirect:/desejos";}
    private String form(Model m,String title,String action){m.addAttribute("pageTitle",title);m.addAttribute("formAction",action);m.addAttribute("publicationTypes",PublicationType.values());m.addAttribute("priorities",WishPriority.values());return "desejos/form";}
    private WishService.Command command(WishForm f){return new WishService.Command(f.getTitle(),f.getPublicationType(),f.getAuthor(),f.getPublisher(),f.getIsbn(),f.getEstimatedPrice(),f.getPriority(),f.getNotes());}
}
