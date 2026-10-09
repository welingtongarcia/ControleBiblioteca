package br.com.minhagibiteca.inventory.domain;

public enum LocationLevel {
    ROOM("Cômodo"), SHELF_UNIT("Estante"), SHELF("Prateleira"), BOX("Caixa");

    private final String label;
    LocationLevel(String label) { this.label = label; }
    public String getLabel() { return label; }
    public boolean canContain(LocationLevel child) { return child != null && ordinal() < child.ordinal(); }
}
