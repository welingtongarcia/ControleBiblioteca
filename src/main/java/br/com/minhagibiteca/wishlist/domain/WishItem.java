package br.com.minhagibiteca.wishlist.domain;

import br.com.minhagibiteca.catalog.domain.PublicationType;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;

public record WishItem(UUID id, String title, PublicationType publicationType, String author,
        String publisher, String isbn, BigDecimal estimatedPrice, WishPriority priority, boolean acquired, String notes) {
    public String getPriceLabel() {
        return estimatedPrice==null ? "Não informado" : NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(estimatedPrice);
    }
}
