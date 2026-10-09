package br.com.minhagibiteca.catalog.presentation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class WorkForm {
    @NotBlank(message = "Informe o título da obra.") @Size(max = 240) private String title;
    @Min(1) @Max(2100) private Integer originalYear;
    @Size(max = 4000) private String summary;
    public String getTitle() { return title; } public void setTitle(String title) { this.title = title; }
    public Integer getOriginalYear() { return originalYear; } public void setOriginalYear(Integer originalYear) { this.originalYear = originalYear; }
    public String getSummary() { return summary; } public void setSummary(String summary) { this.summary = summary; }
}
