package br.com.minhagibiteca.dashboard.application;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public record CollectionSummary(long editions, long copies, long ownedEditions, long pricedCopies, BigDecimal totalPaid) {
    public long unpricedCopies() { return copies - pricedCopies; }
    public String totalPaidLabel() {
        if (totalPaid == null && copies > 0) return "Não informado";
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
                .format(totalPaid == null ? BigDecimal.ZERO : totalPaid);
    }
}
