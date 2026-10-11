package br.com.minhagibiteca.catalog.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import br.com.minhagibiteca.catalog.domain.CreatorFunction;
import java.util.Set;
import java.util.HashSet;

public class CreatorForm {
    @NotBlank(message = "Informe o nome do criador.") @Size(max = 180) private String name;
    @Size(max = 180) private String sortName;
    @Size(max = 10) private Set<@NotNull CreatorFunction> functions = new HashSet<>();
    public Set<CreatorFunction> getFunctions() { return functions; }
    public void setFunctions(Set<CreatorFunction> functions) { this.functions = functions == null ? new HashSet<>() : new HashSet<>(functions); }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getSortName() { return sortName; } public void setSortName(String sortName) { this.sortName = sortName; }
}
