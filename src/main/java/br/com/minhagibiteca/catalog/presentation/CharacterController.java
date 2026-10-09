package br.com.minhagibiteca.catalog.presentation;
import br.com.minhagibiteca.auth.application.CurrentAuthenticatedUser;
import br.com.minhagibiteca.catalog.application.CharacterService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
public class CharacterController {
 private final CharacterService service;private final CurrentAuthenticatedUser user;
 public CharacterController(CharacterService service,CurrentAuthenticatedUser user){this.service=service;this.user=user;}
 @InitBinder("characterForm")void bind(WebDataBinder b){b.setAllowedFields("name");}
 @GetMapping("/personagens")String list(@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="0")int page,Model m){m.addAttribute("characters",service.list(user.id(),q,page));m.addAttribute("query",q);return "personagens/list";}
 @GetMapping("/personagens/novo")String newForm(Model m){m.addAttribute("characterForm",new CharacterForm());return form(m,"Novo personagem","/personagens");}
 @PostMapping("/personagens")String create(@Valid @ModelAttribute("characterForm")CharacterForm f,BindingResult r,Model m,RedirectAttributes a){if(r.hasErrors())return form(m,"Novo personagem","/personagens");try{service.save(user.id(),null,f.getName());}catch(CharacterService.CharacterRule e){r.rejectValue("name","rule",e.getMessage());return form(m,"Novo personagem","/personagens");}a.addFlashAttribute("successMessage","Personagem cadastrado.");return "redirect:/personagens";}
 @GetMapping("/personagens/{id}/editar")String edit(@PathVariable UUID id,Model m){var p=service.find(user.id(),id);var f=new CharacterForm();f.setName(p.name());m.addAttribute("characterForm",f);return form(m,"Editar personagem","/personagens/"+id+"/editar");}
 @PostMapping("/personagens/{id}/editar")String update(@PathVariable UUID id,@Valid @ModelAttribute("characterForm")CharacterForm f,BindingResult r,Model m,RedirectAttributes a){service.find(user.id(),id);if(r.hasErrors())return form(m,"Editar personagem","/personagens/"+id+"/editar");try{service.save(user.id(),id,f.getName());}catch(CharacterService.CharacterRule e){r.rejectValue("name","rule",e.getMessage());return form(m,"Editar personagem","/personagens/"+id+"/editar");}a.addFlashAttribute("successMessage","Personagem atualizado.");return "redirect:/personagens";}
 @GetMapping("/personagens/{id}/excluir")String deleteForm(@PathVariable UUID id,Model m){m.addAttribute("character",service.find(user.id(),id));return "personagens/delete";}
 @PostMapping("/personagens/{id}/excluir")String delete(@PathVariable UUID id,@RequestParam(defaultValue="false")boolean confirmed,RedirectAttributes a){
  service.find(user.id(),id);if(!confirmed){a.addFlashAttribute("errorMessage","Confirme a exclusão do personagem.");return "redirect:/personagens/"+id+"/excluir";}
  try{service.delete(user.id(),id);a.addFlashAttribute("successMessage","Personagem excluído.");}catch(CharacterService.CharacterRule e){a.addFlashAttribute("errorMessage",e.getMessage());}return "redirect:/personagens";
 }
 private String form(Model m,String title,String action){m.addAttribute("pageTitle",title);m.addAttribute("formAction",action);return "personagens/form";}
}
