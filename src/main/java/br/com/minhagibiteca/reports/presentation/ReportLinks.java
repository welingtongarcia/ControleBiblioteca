package br.com.minhagibiteca.reports.presentation;

import br.com.minhagibiteca.reports.application.ReportFilter;
import org.springframework.web.util.UriComponentsBuilder;

public record ReportLinks(ReportFilter filter, String type) {
    public String page(int page) { return url("/relatorios", page); }
    public String csv() { return url("/relatorios/csv", 0); }
    public String pdf() { return url("/relatorios/pdf", 0); }
    private String url(String path, int page) {
        var b = UriComponentsBuilder.fromPath(path);
        Object[] values={filter.q().isEmpty()?null:filter.q(),filter.seriesId(),filter.publisherId(),filter.creatorId(),filter.creatorFunction(),filter.characterId(),filter.mediaType(),filter.reading(),filter.start(),filter.end(),type};
        String[] names={"q","seriesId","publisherId","creatorId","creatorFunction","characterId","mediaType","reading","start","end","type"};
        for(int i=0;i<names.length;i++) if(values[i]!=null) b.queryParam(names[i],values[i]);
        if(page>0) b.queryParam("page",page);
        return b.build().encode().toUriString();
    }
}
