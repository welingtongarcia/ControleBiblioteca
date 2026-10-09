package br.com.minhagibiteca.catalog.application;

import org.springframework.data.domain.Sort;

public enum CatalogOrder {
    TITLE_ASC("Título: A–Z", "normalizedTitle", Sort.Direction.ASC),
    TITLE_DESC("Título: Z–A", "normalizedTitle", Sort.Direction.DESC),
    NEWEST("Cadastros mais recentes", "createdAt", Sort.Direction.DESC),
    OLDEST("Cadastros mais antigos", "createdAt", Sort.Direction.ASC);

    private final String label;
    private final String property;
    private final Sort.Direction direction;

    CatalogOrder(String label, String property, Sort.Direction direction) {
        this.label = label; this.property = property; this.direction = direction;
    }
    public String getLabel() { return label; }
    public Sort sort() { return Sort.by(direction, property).and(Sort.by("id")); }
    public static CatalogOrder parse(String value) {
        if (value == null) return TITLE_ASC;
        try { return valueOf(value); } catch (IllegalArgumentException exception) { return TITLE_ASC; }
    }
}
