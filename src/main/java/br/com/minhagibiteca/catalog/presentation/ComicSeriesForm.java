package br.com.minhagibiteca.catalog.presentation;

import br.com.minhagibiteca.catalog.domain.SeriesStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public class ComicSeriesForm {
    private UUID publisherId;
    @NotBlank(message = "Informe o nome da série.") @Size(max = 180) private String name;
    @Min(value = 1800, message = "Informe um ano válido.") @Max(value = 2100, message = "Informe um ano válido.") private Integer startYear;
    @Min(value = 1800, message = "Informe um ano válido.") @Max(value = 2100, message = "Informe um ano válido.") private Integer endYear;
    private SeriesStatus status;
    @Size(max = 2000) private String notes;

    @AssertTrue(message = "O ano final não pode ser anterior ao ano inicial.")
    public boolean isPublicationPeriodValid() { return startYear == null || endYear == null || endYear >= startYear; }
    public UUID getPublisherId() { return publisherId; } public void setPublisherId(UUID publisherId) { this.publisherId = publisherId; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public Integer getStartYear() { return startYear; } public void setStartYear(Integer startYear) { this.startYear = startYear; }
    public Integer getEndYear() { return endYear; } public void setEndYear(Integer endYear) { this.endYear = endYear; }
    public SeriesStatus getStatus() { return status; } public void setStatus(SeriesStatus status) { this.status = status; }
    public String getNotes() { return notes; } public void setNotes(String notes) { this.notes = notes; }
}
