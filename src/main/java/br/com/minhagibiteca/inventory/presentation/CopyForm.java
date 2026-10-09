package br.com.minhagibiteca.inventory.presentation;

import br.com.minhagibiteca.inventory.domain.CopyCondition;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

public class CopyForm {
    private UUID storageLocationId;
    @NotNull(message = "Selecione a conservação.")
    private CopyCondition condition = CopyCondition.UNKNOWN;
    @Size(max = 2000, message = "Informe até 2000 caracteres.")
    private String conditionNotes;
    @Size(max = 160, message = "Informe até 160 caracteres.")
    private String acquisitionSource;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate acquisitionDate;
    @DecimalMin(value = "0", message = "O valor não pode ser negativo.")
    @Digits(integer = 10, fraction = 2, message = "Informe até 10 dígitos inteiros e duas casas decimais.")
    private BigDecimal acquisitionPrice;
    @Size(max = 2000, message = "Informe até 2000 caracteres.")
    private String notes;
    public UUID getStorageLocationId() { return storageLocationId; }
    public void setStorageLocationId(UUID value) { storageLocationId = value; }
    public CopyCondition getCondition() { return condition; }
    public void setCondition(CopyCondition value) { condition = value; }
    public String getConditionNotes() { return conditionNotes; }
    public void setConditionNotes(String value) { conditionNotes = value; }
    public String getAcquisitionSource() { return acquisitionSource; }
    public void setAcquisitionSource(String value) { acquisitionSource = value; }
    public LocalDate getAcquisitionDate() { return acquisitionDate; }
    public void setAcquisitionDate(LocalDate value) { acquisitionDate = value; }
    public BigDecimal getAcquisitionPrice() { return acquisitionPrice; }
    public void setAcquisitionPrice(BigDecimal value) { acquisitionPrice = value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = value; }
}
