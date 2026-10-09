package br.com.minhagibiteca.inventory.presentation;

import br.com.minhagibiteca.inventory.domain.LocationLevel;
import jakarta.validation.constraints.*;
import java.util.UUID;

public class LocationForm {
    @NotBlank(message = "Informe o nome da localização.") @Size(max = 120) private String name;
    @NotNull(message = "Selecione o tipo de localização.") private LocationLevel level = LocationLevel.ROOM;
    private UUID parentId;
    @Size(max = 2000) private String notes;
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public LocationLevel getLevel() { return level; } public void setLevel(LocationLevel level) { this.level = level; }
    public UUID getParentId() { return parentId; } public void setParentId(UUID parentId) { this.parentId = parentId; }
    public String getNotes() { return notes; } public void setNotes(String notes) { this.notes = notes; }
}
