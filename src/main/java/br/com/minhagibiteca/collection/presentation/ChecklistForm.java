package br.com.minhagibiteca.collection.presentation;
import jakarta.validation.constraints.*;
public class ChecklistForm {
    @NotBlank(message = "Informe o número editorial.") @Size(max = 40, message = "Informe até 40 caracteres.")
    private String editorialNumber;
    @Size(max = 180, message = "Informe até 180 caracteres.") private String label;
    private boolean owned;
    @Size(max = 2000, message = "Informe até 2000 caracteres.") private String notes;
    public String getEditorialNumber() { return editorialNumber; }
    public void setEditorialNumber(String value) { editorialNumber = value; }
    public String getLabel() { return label; }
    public void setLabel(String value) { label = value; }
    public boolean isOwned() { return owned; }
    public void setOwned(boolean value) { owned = value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = value; }
}
