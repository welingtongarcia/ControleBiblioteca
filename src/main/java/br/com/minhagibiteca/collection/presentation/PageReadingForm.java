package br.com.minhagibiteca.collection.presentation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class PageReadingForm {
    @NotNull(message = "Informe quantas páginas você já leu.")
    @Min(value = 0, message = "As páginas lidas não podem ser negativas.")
    @Max(value = 100000, message = "O máximo é 100.000 páginas.")
    private Integer pagesRead;
    public Integer getPagesRead() { return pagesRead; }
    public void setPagesRead(Integer value) { pagesRead = value; }
}
