package br.com.minhagibiteca.catalog.domain;

public enum PublicationType {
    COMIC("Quadrinho"), BOOK("Livro");
    private final String label;
    PublicationType(String label) { this.label = label; }
    public String getLabel() { return label; }
}
