package br.com.minhagibiteca.catalog.application;

public enum CatalogReading {
    ALL("Todas as situações"), UNREAD("Não lidas"), IN_PROGRESS("Em leitura"), READ("Lidas");

    private final String label;
    CatalogReading(String label) { this.label = label; }
    public String getLabel() { return label; }
}
