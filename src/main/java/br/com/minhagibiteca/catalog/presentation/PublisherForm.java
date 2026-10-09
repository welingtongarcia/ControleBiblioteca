package br.com.minhagibiteca.catalog.presentation;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public class PublisherForm {

    @NotBlank(message = "Informe o nome da editora.")
    @Size(max = 160, message = "O nome deve ter no máximo 160 caracteres.")
    private String name;

    @Pattern(regexp = "^$|^[A-Za-z]{2}$", message = "Use a sigla ISO de duas letras.")
    private String countryCode;

    @Size(max = 2000, message = "As observações devem ter no máximo 2000 caracteres.")
    private String notes;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
