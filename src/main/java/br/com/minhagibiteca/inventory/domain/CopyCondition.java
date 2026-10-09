package br.com.minhagibiteca.inventory.domain;

public enum CopyCondition {
    UNKNOWN("Não informada"), NEW("Novo"), VERY_GOOD("Muito bom"), GOOD("Bom"), FAIR("Regular"), POOR("Ruim");
    private final String label;
    CopyCondition(String label) { this.label = label; }
    public String getLabel() { return label; }
}
