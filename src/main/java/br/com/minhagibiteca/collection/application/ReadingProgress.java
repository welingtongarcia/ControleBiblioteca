package br.com.minhagibiteca.collection.application;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record ReadingProgress(long total, long read) {
    public BigDecimal percentage() {
        return total == 0 ? BigDecimal.ZERO.setScale(1) : BigDecimal.valueOf(read).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }
    public String percentageLabel() { return percentage().toPlainString().replace('.', ',') + "%"; }
    public String label() { return percentageLabel() + " — " + read + " de " + total + " edições lidas"; }
}
