package br.com.minhagibiteca.dashboard.application;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public record CollectionSummary(long editions, long copies, long ownedEditions, long pricedCopies, BigDecimal totalPaid,
        long physicalEditions, long digitalEditions, long pricedEditions, BigDecimal registeredPaid) {
    public long unpricedCopies() { return copies - pricedCopies; }
    public long unpricedEditions() { return editions - pricedEditions; }
    public long unknownMediaEditions() { return editions - physicalEditions - digitalEditions; }
    public String registeredPaidLabel() { return money(registeredPaid, editions); }
    public String totalPaidLabel() {
        return money(totalPaid, copies);
    }
    private String money(BigDecimal value, long count) {
        if (value == null && count > 0) return "Não informado";
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
                .format(value == null ? BigDecimal.ZERO : value);
    }
}
