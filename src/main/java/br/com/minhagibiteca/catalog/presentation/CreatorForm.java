package br.com.minhagibiteca.catalog.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreatorForm {
    @NotBlank(message = "Informe o nome do criador.") @Size(max = 180) private String name;
    @Size(max = 180) private String sortName;
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getSortName() { return sortName; } public void setSortName(String sortName) { this.sortName = sortName; }
}
