package br.com.minhagibiteca.finance.presentation;
import br.com.minhagibiteca.auth.application.CurrentAuthenticatedUser;
import br.com.minhagibiteca.finance.application.*;
import br.com.minhagibiteca.catalog.domain.MediaType;
import br.com.minhagibiteca.shared.presentation.NavigationContext;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
public class FinanceController {
 private final FinanceService service;private final CurrentAuthenticatedUser user;
 public FinanceController(FinanceService service,CurrentAuthenticatedUser user){this.service=service;this.user=user;}
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
}
