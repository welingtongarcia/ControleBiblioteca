package br.com.minhagibiteca.catalog.application;

public enum CatalogMissing {
    NONE("Todos os cadastros"), COVER("Sem capa"), PAGES("Sem total de páginas"),
    PUBLISHER("Sem editora"), SERIES("Sem série"), PAID("Sem valor pago");

    private final String label;
    CatalogMissing(String label) { this.label = label; }
    public String getLabel() { return label; }
}
