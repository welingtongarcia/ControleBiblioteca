package br.com.minhagibiteca.catalog.presentation;

import br.com.minhagibiteca.auth.application.CurrentAuthenticatedUser;
import br.com.minhagibiteca.catalog.application.*;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/edicoes/amazon")
public class AmazonImportController {
    private final CurrentAuthenticatedUser user;
    private final AmazonImportService imports;
    private final EditionCatalogViewService catalog;
    public AmazonImportController(CurrentAuthenticatedUser user,AmazonImportService imports,EditionCatalogViewService catalog) {
        this.user=user;this.imports=imports;this.catalog=catalog;
    }

    @PostMapping("/buscar") String search(@ModelAttribute("editionForm") EditionForm form,BindingResult errors,
            @RequestParam(defaultValue="") String amazonLink,RedirectAttributes flash) {
        preserve(form,errors,flash);
        if(errors.hasErrors()) { flash.addFlashAttribute("amazonMessage","Corrija os campos com valores inválidos antes de importar. Seus dados foram preservados.");return "redirect:/edicoes/nova"; }
        try {
            var preview=imports.search(user.id(),amazonLink);
            flash.addFlashAttribute("amazonPreview",preview);
            flash.addFlashAttribute("amazonLink",preview.productUrl());
            flash.addFlashAttribute("amazonMessage",preview.message());
        } catch(AmazonImportService.ImportException e) { flash.addFlashAttribute("amazonMessage",e.getMessage()); }
        return "redirect:/edicoes/nova";
    }

    @PostMapping("/aplicar") String apply(@ModelAttribute("editionForm") EditionForm form,BindingResult errors,
            @RequestParam(defaultValue="") String amazonToken,RedirectAttributes flash) {
        preserve(form,errors,flash);
        if(errors.hasErrors()) { flash.addFlashAttribute("amazonMessage","Corrija os campos com valores inválidos e consulte o link novamente. Seus dados foram preservados.");return "redirect:/edicoes/nova"; }
        try {
            var preview=imports.select(user.id(),amazonToken);var found=preview.metadata();
            if(blank(form.getTitle())) form.setTitle(found.title());
            if(blank(form.getIsbn())) form.setIsbn(found.isbn());
            if(form.getPublicationYear()==null) form.setPublicationYear(found.year());
            if(form.getTotalPages()==null) form.setTotalPages(found.pages());
            if(blank(form.getFormat())) form.setFormat(found.format());
            if(blank(form.getNotes())) form.setNotes(found.summary());
            if(form.getPublisherId()==null && found.publishers().size()==1) form.setPublisherId(unique(catalog.publishers(user.id()),found.publishers().getFirst()));
            if(form.getCreatorIds()==null || form.getCreatorIds().isEmpty()) {
                var options=catalog.creators(user.id());var ids=new LinkedHashSet<UUID>();
                for(String author:found.authors()) { UUID id=unique(options,author);if(id!=null) ids.add(id); }
                form.setCreatorIds(List.copyOf(ids));
            }
            flash.addFlashAttribute("amazonApplied",preview);
            flash.addFlashAttribute("amazonLink",preview.productUrl());
            flash.addFlashAttribute("amazonMessage","Sugestões aplicadas somente nos campos vazios. Revise os dados e salve a edição para concluir. Valores, compra e leitura não foram alterados.");
        } catch(AmazonImportService.ImportException e) { flash.addFlashAttribute("amazonMessage",e.getMessage()); }
        return "redirect:/edicoes/nova";
    }
    private static void preserve(EditionForm form,BindingResult errors,RedirectAttributes flash) {
        flash.addFlashAttribute("editionForm",form);
        if(errors.hasErrors()) flash.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX+"editionForm",errors);
    }
    private static boolean blank(String value) { return value==null || value.isBlank(); }
    private static UUID unique(List<EditionCatalogViewService.NamedOption> options,String name) {
        var matches=options.stream().filter(o->o.name().strip().equalsIgnoreCase(name.strip())).toList();
        return matches.size()==1?matches.getFirst().id():null;
    }
}
