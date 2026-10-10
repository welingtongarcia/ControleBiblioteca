package br.com.minhagibiteca.reports.presentation;

import br.com.minhagibiteca.auth.application.CurrentAuthenticatedUser;
import br.com.minhagibiteca.catalog.domain.CreatorFunction;
import br.com.minhagibiteca.reports.application.ReportFilter;
import br.com.minhagibiteca.reports.application.ReportService;
import br.com.minhagibiteca.reports.infrastructure.ReportCsvExporter;
import br.com.minhagibiteca.reports.infrastructure.ReportPdfExporter;
import br.com.minhagibiteca.shared.presentation.NavigationContext;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/relatorios")
public class ReportController {
    private final ReportService reports;
    private final CurrentAuthenticatedUser user;
    private final ReportCsvExporter csv;
    private final ReportPdfExporter pdf;
    public ReportController(ReportService reports, CurrentAuthenticatedUser user, ReportCsvExporter csv, ReportPdfExporter pdf) {
        this.reports=reports; this.user=user; this.csv=csv; this.pdf=pdf;
    }
    private void validate(BindingResult result) {
        if(result.hasErrors()) throw new ReportFilter.InvalidReport("Revise os filtros: use datas, identificadores e opções válidos; a busca deve ter até 180 caracteres.");
    }
    @GetMapping
    public String index(@Valid @ModelAttribute("reportForm") ReportForm form, BindingResult result,
            @RequestParam(defaultValue="0") int page, Model model, jakarta.servlet.http.HttpServletResponse response) {
        validate(result);
        var filter=form.filter();
        model.addAttribute("report",reports.preview(user.id(),filter,page));
        model.addAttribute("links",new ReportLinks(filter,form.getType()));
        for(var kind:java.util.List.of("series","publisher","creator","character")) model.addAttribute(kind+"Options",reports.options(user.id(),kind));
        model.addAttribute("creatorFunctions",CreatorFunction.values());
        response.setHeader("Cache-Control","no-store");
        return "relatorios/index";
    }
    @GetMapping("/csv")
    public ResponseEntity<byte[]> csv(@Valid @ModelAttribute("reportForm") ReportForm form, BindingResult result) {
        validate(result);
        var data=reports.export(user.id(),form.filter(),form.catalog());
        return download(csv.export(data,form.catalog()),"text/csv;charset=UTF-8",form.catalog()?"catalogo-acervo.csv":"resumo-colecao.csv");
    }
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> pdf(@Valid @ModelAttribute("reportForm") ReportForm form, BindingResult result) throws java.io.IOException {
        validate(result);
        var data=reports.export(user.id(),form.filter(),form.catalog());
        return download(pdf.export(data,form.catalog()),"application/pdf",form.catalog()?"catalogo-acervo.pdf":"resumo-colecao.pdf");
    }
    private ResponseEntity<byte[]> download(byte[] bytes,String contentType,String filename) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+filename+"\"").body(bytes);
    }
    @ExceptionHandler(ReportFilter.InvalidReport.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String invalid(ReportFilter.InvalidReport error,Model model,jakarta.servlet.http.HttpServletRequest request) {
        model.addAttribute("message",error.getMessage()); model.addAttribute("navigation",NavigationContext.from(request));
        return "relatorios/invalid";
    }
}
