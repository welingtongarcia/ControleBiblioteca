package br.com.minhagibiteca.catalog.domain;

public enum MediaType {
    PHYSICAL("Físico"), DIGITAL("Digital");

    private final String label;
    MediaType(String label) { this.label = label; }
    public String getLabel() { return label; }
}
