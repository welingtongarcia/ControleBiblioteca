package br.com.minhagibiteca.reports.application;

import br.com.minhagibiteca.reports.infrastructure.ReportQueries;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {
    public static final int EXPORT_LIMIT = 10000;
    private final ReportQueries queries;
    public ReportService(ReportQueries queries) { this.queries = queries; }

    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
    public ReportData preview(UUID owner, ReportFilter filter, int page) { return load(owner,filter,Math.max(0,page),false,false); }
    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
    public ReportData export(UUID owner, ReportFilter filter, boolean catalog) { return load(owner,filter,0,true,catalog); }
    @Transactional(readOnly=true)
    public List<ReportData.Option> options(UUID owner, String kind) { return queries.options(owner,kind); }

    private ReportData load(UUID owner, ReportFilter f, int page, boolean export, boolean catalog) {
        var filters = descriptions(owner,f);
        var summary = queries.summary(owner,f);
        if (export && catalog && summary.editions() > EXPORT_LIMIT)
            throw new ReportFilter.InvalidReport("O catálogo possui mais de 10.000 edições. Refine os filtros para exportar sem truncar os dados.");
        int size = export ? EXPORT_LIMIT : 20;
        if(!export) page=(int)Math.min(page,Math.max(0,(summary.editions()-1)/size));
        var entries = export && !catalog ? List.<ReportData.Entry>of() : queries.entries(owner,f,(long)page*size,size);
        return new ReportData(summary,new PageImpl<>(entries,PageRequest.of(page,size),summary.editions()),filters,ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")));
    }
    private List<String> descriptions(UUID owner, ReportFilter f) {
        var labels = new ArrayList<String>();
        if (!f.q().isEmpty()) labels.add("Busca: " + f.q());
        if (f.seriesId()!=null) labels.add("Coleção: " + queries.requireName(owner,"series",f.seriesId()));
        if (f.publisherId()!=null) labels.add("Editora: " + queries.requireName(owner,"publisher",f.publisherId()));
        if (f.creatorId()!=null) labels.add("Criador: " + queries.requireName(owner,"creator",f.creatorId()));
        if (f.creatorFunction()!=null) labels.add("Função geral: " + f.creatorFunction().getLabel());
        if (f.characterId()!=null) labels.add("Personagem: " + queries.requireName(owner,"character",f.characterId()));
        if (!f.mediaType().equals("ALL")) labels.add("Suporte: " + switch(f.mediaType()) {case "PHYSICAL" -> "Físico"; case "DIGITAL" -> "Digital"; default -> "Não informado";});
        if (!f.reading().equals("ALL")) labels.add("Leitura: " + (f.reading().equals("READ") ? "Lida" : "Não lida"));
        if (f.start()!=null) {var format=DateTimeFormatter.ofPattern("dd/MM/yyyy"); labels.add("Compra: " + f.start().format(format) + " a " + f.end().format(format));}
        if (labels.isEmpty()) labels.add("Toda a coleção (sem filtros)");
        return List.copyOf(labels);
    }
}
