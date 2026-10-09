package br.com.minhagibiteca.finance.presentation;
import br.com.minhagibiteca.auth.application.CurrentAuthenticatedUser;
import br.com.minhagibiteca.finance.application.*;
import br.com.minhagibiteca.catalog.domain.MediaType;
import br.com.minhagibiteca.shared.presentation.NavigationContext;
import jakarta.validation.Valid;
import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
public class FinanceController {
 private final FinanceService service;private final CurrentAuthenticatedUser user;
 public FinanceController(FinanceService service,CurrentAuthenticatedUser user){this.service=service;this.user=user;}
 @InitBinder("purchaseForm")void bind(WebDataBinder binder){
  binder.setAllowedFields("editionId","amount","purchaseDate","vendor","notes");
  binder.registerCustomEditor(BigDecimal.class,new PropertyEditorSupport(){
   @Override public void setAsText(String text){if(text==null||text.isBlank()){setValue(null);return;}String value=text.strip();if(!value.matches("-?[0-9]+([.,][0-9]+)?"))throw new IllegalArgumentException();setValue(new BigDecimal(value.replace(',','.')));}
   @Override public String getAsText(){return getValue()==null?"":((BigDecimal)getValue()).toPlainString().replace('.',',');}
  });
 }
 @GetMapping("/financeiro")String index(@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate start,
  @RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate end,@RequestParam(required=false)UUID seriesId,
  @RequestParam(required=false)UUID creatorId,@RequestParam(required=false)UUID characterId,@RequestParam(required=false)UUID publisherId,
  @RequestParam(required=false)String mediaType,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="AMOUNT")String ranking,Model m){
  if(!java.util.Set.of("AMOUNT","QUANTITY").contains(ranking))throw new FinanceFilter.InvalidFilter("Selecione uma ordenação de ranking válida.");
  boolean unknown="UNSPECIFIED".equals(mediaType);MediaType media=null;
  if(mediaType!=null&&!mediaType.isBlank()&&!unknown){try{media=MediaType.valueOf(mediaType);}catch(IllegalArgumentException error){throw new FinanceFilter.InvalidFilter("Selecione um suporte válido.");}}
  var f=new FinanceFilter(start,end,seriesId,creatorId,characterId,publisherId,media,unknown);
  m.addAttribute("filter",f);m.addAttribute("ranking",ranking);m.addAttribute("report",service.report(user.id(),f,page,ranking.equals("QUANTITY")));
  m.addAttribute("links",new FinanceLinks(f,ranking.equals("QUANTITY")));options(m);return "financeiro/index";
 }
 private void options(Model m){for(String kind:java.util.List.of("series","publisher","creator","character"))m.addAttribute(kind+"Options",service.options(user.id(),kind));m.addAttribute("mediaTypes",MediaType.values());}
 @ExceptionHandler(FinanceFilter.InvalidFilter.class)@ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
 String invalid(FinanceFilter.InvalidFilter error,Model m,jakarta.servlet.http.HttpServletRequest request){m.addAttribute("message",error.getMessage());m.addAttribute("navigation",NavigationContext.from(request));return "financeiro/invalid";}
 @GetMapping("/financeiro/compras/nova")String newForm(@RequestParam(required=false)UUID editionId,Model m){
  var f=new PurchaseForm();if(editionId!=null){service.options(user.id(),"edition").stream().filter(x->x.id().equals(editionId)).findFirst().orElseThrow(br.com.minhagibiteca.shared.application.ResourceNotFoundException::new);f.setEditionId(editionId);}
  m.addAttribute("purchaseForm",f);return form(m,"Registrar compra","/financeiro/compras");
 }
 @PostMapping("/financeiro/compras")String create(@Valid @ModelAttribute("purchaseForm")PurchaseForm f,BindingResult errors,Model m,RedirectAttributes a){
  if(errors.hasErrors())return form(m,"Registrar compra","/financeiro/compras");
  try{service.save(user.id(),null,command(f));}catch(FinanceService.InvalidPurchase e){errors.reject("rule",e.getMessage());return form(m,"Registrar compra","/financeiro/compras");}
  a.addFlashAttribute("successMessage","Compra registrada com sucesso.");return "redirect:/financeiro";
 }
 @GetMapping("/financeiro/compras/{id}/editar")String edit(@PathVariable UUID id,Model m){
  var p=service.find(user.id(),id);var f=new PurchaseForm();f.setEditionId(p.editionId());f.setAmount(p.amount());f.setPurchaseDate(p.purchaseDate());f.setVendor(p.vendor());f.setNotes(p.notes());m.addAttribute("purchaseForm",f);
  return form(m,"Editar compra","/financeiro/compras/"+id+"/editar");
 }
 @PostMapping("/financeiro/compras/{id}/editar")String update(@PathVariable UUID id,@Valid @ModelAttribute("purchaseForm")PurchaseForm f,BindingResult errors,Model m,RedirectAttributes a){
  service.find(user.id(),id);if(errors.hasErrors())return form(m,"Editar compra","/financeiro/compras/"+id+"/editar");
  try{service.save(user.id(),id,command(f));}catch(FinanceService.InvalidPurchase e){errors.reject("rule",e.getMessage());return form(m,"Editar compra","/financeiro/compras/"+id+"/editar");}
  a.addFlashAttribute("successMessage","Compra atualizada com sucesso.");return "redirect:/financeiro";
 }
 @GetMapping("/financeiro/compras/{id}/excluir")String deleteForm(@PathVariable UUID id,Model m){m.addAttribute("purchase",service.find(user.id(),id));return "financeiro/delete";}
 @PostMapping("/financeiro/compras/{id}/excluir")String delete(@PathVariable UUID id,@RequestParam(defaultValue="false")boolean confirmed,RedirectAttributes a){
  service.find(user.id(),id);if(!confirmed){a.addFlashAttribute("errorMessage","Confirme a exclusão da compra.");return "redirect:/financeiro/compras/"+id+"/excluir";}
  service.delete(user.id(),id);a.addFlashAttribute("successMessage","Compra excluída.");return "redirect:/financeiro";
 }
 private String form(Model m,String title,String action){m.addAttribute("pageTitle",title);m.addAttribute("formAction",action);m.addAttribute("editionOptions",service.options(user.id(),"edition"));return "financeiro/form";}
 private FinanceService.Command command(PurchaseForm f){return new FinanceService.Command(f.getEditionId(),f.getAmount(),f.getPurchaseDate(),f.getVendor(),f.getNotes());}
}
