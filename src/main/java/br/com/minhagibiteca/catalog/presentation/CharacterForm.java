package br.com.minhagibiteca.catalog.presentation;
import jakarta.validation.constraints.*;
public class CharacterForm {
 @NotBlank(message="Informe o nome do personagem.") @Size(max=180) private String name;
 public String getName(){return name;}public void setName(String name){this.name=name;}
}
