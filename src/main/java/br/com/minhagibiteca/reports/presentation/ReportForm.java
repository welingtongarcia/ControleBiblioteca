package br.com.minhagibiteca.reports.presentation;

import br.com.minhagibiteca.catalog.domain.CreatorFunction;
import br.com.minhagibiteca.reports.application.ReportFilter;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

public class ReportForm {
    @Size(max=180)
    private String q = "";
    private UUID seriesId = null;
    private UUID publisherId = null;
    private UUID creatorId = null;
    private CreatorFunction creatorFunction = null;
    private UUID characterId = null;
    private String mediaType = "ALL";
    private String reading = "ALL";
    @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)
    private LocalDate start = null;
    @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)
    private LocalDate end = null;
    private String type = "SUMMARY";
    public String getQ() { return q; }
    public void setQ(String value) { this.q = value; }
    public UUID getSeriesId() { return seriesId; }
    public void setSeriesId(UUID value) { this.seriesId = value; }
    public UUID getPublisherId() { return publisherId; }
    public void setPublisherId(UUID value) { this.publisherId = value; }
    public UUID getCreatorId() { return creatorId; }
    public void setCreatorId(UUID value) { this.creatorId = value; }
    public CreatorFunction getCreatorFunction() { return creatorFunction; }
    public void setCreatorFunction(CreatorFunction value) { this.creatorFunction = value; }
    public UUID getCharacterId() { return characterId; }
    public void setCharacterId(UUID value) { this.characterId = value; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String value) { this.mediaType = value; }
    public String getReading() { return reading; }
    public void setReading(String value) { this.reading = value; }
    public LocalDate getStart() { return start; }
    public void setStart(LocalDate value) { this.start = value; }
    public LocalDate getEnd() { return end; }
    public void setEnd(LocalDate value) { this.end = value; }
    public String getType() { return type; }
    public void setType(String value) { this.type = value; }
    public ReportFilter filter() {
        if (!java.util.Set.of("SUMMARY","CATALOG").contains(type)) throw new ReportFilter.InvalidReport("Selecione um tipo de relatório válido.");
        return new ReportFilter(q,seriesId,publisherId,creatorId,creatorFunction,characterId,mediaType,reading,start,end);
    }
    public boolean catalog() { return type.equals("CATALOG"); }
}
