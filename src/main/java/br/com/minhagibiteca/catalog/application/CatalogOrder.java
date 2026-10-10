package br.com.minhagibiteca.catalog.application;

import org.springframework.data.domain.Sort;

public enum CatalogOrder {
    TITLE_ASC("Título: A–Z", "normalizedTitle", Sort.Direction.ASC),
    TITLE_DESC("Título: Z–A", "normalizedTitle", Sort.Direction.DESC),
    NUMBER_ASC("Número da edição: crescente", null, Sort.Direction.ASC),
    NUMBER_DESC("Número da edição: decrescente", null, Sort.Direction.DESC),
    TITLE_NUMBER_ASC("Título A–Z + número crescente", null, Sort.Direction.ASC),
    TITLE_NUMBER_DESC("Título A–Z + número decrescente", null, Sort.Direction.DESC),
    NEWEST("Cadastros mais recentes", "createdAt", Sort.Direction.DESC),
    OLDEST("Cadastros mais antigos", "createdAt", Sort.Direction.ASC);

    private final String label;
    private final String property;
    private final Sort.Direction direction;

    CatalogOrder(String label, String property, Sort.Direction direction) {
        this.label = label; this.property = property; this.direction = direction;
    }
    public String getLabel() { return label; }
    public boolean groupsByTitle() { return this == TITLE_NUMBER_ASC || this == TITLE_NUMBER_DESC; }
    public boolean isNumberOrder() { return this == NUMBER_ASC || this == NUMBER_DESC || groupsByTitle(); }
    public boolean isDescending() { return direction == Sort.Direction.DESC; }
    public Sort sort() { return isNumberOrder() ? Sort.unsorted() : Sort.by(direction, property).and(Sort.by("id")); }
    public static CatalogOrder parse(String value) {
        if (value == null) return TITLE_NUMBER_ASC;
        try { return valueOf(value); } catch (IllegalArgumentException exception) { return TITLE_NUMBER_ASC; }
    }
}
