package br.com.minhagibiteca.wishlist.domain;

public enum WishPriority {
    HIGH("Alta"), NORMAL("Normal"), LOW("Baixa");
    private final String label;
    WishPriority(String label) { this.label=label; }
    public String getLabel() { return label; }
}
