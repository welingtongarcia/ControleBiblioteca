package br.com.minhagibiteca.catalog.domain;

public enum CreatorFunction {
    ROTEIRISTA("Roteirista"),
    DESENHISTA("Desenhista"),
    ARTE_FINALISTA("Arte-finalista"),
    COLORISTA("Colorista"),
    LETRISTA("Letrista"),
    CAPISTA("Capista"),
    EDITOR("Editor"),
    TRADUTOR("Tradutor"),
    OUTRO("Outra função");

    private final String label;

    CreatorFunction(String label) { this.label = label; }
    public String getLabel() { return label; }
}
