package br.com.minhagibiteca.inventory.application;

import br.com.minhagibiteca.inventory.domain.CopyCondition;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

public record CopyView(UUID id, UUID editionId, UUID storageLocationId, CopyCondition condition, String conditionNotes,
        String acquisitionSource, LocalDate acquisitionDate, BigDecimal acquisitionPrice, String notes, String locationPath) {
    public String getFormattedPrice() { return acquisitionPrice == null ? "Não informado" : NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(acquisitionPrice); }
    public String getFormattedDate() { return acquisitionDate == null ? "Não informada" : acquisitionDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
}
