package br.com.minhagibiteca.collection.application;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record ReadingProgress(long total, long read, long totalPages, long pagesRead, long knownPageEditions) {
    public ReadingProgress(long total, long read) { this(total, read, 0, 0, 0); }
    public long pagesUnread() { return totalPages - pagesRead; }
    public BigDecimal pagePercentage() {
        return totalPages == 0 ? BigDecimal.ZERO.setScale(1) : BigDecimal.valueOf(pagesRead).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalPages), 1, RoundingMode.HALF_UP);
    }
    public String pagePercentageLabel() { return pagePercentage().toPlainString().replace('.', ',') + "%"; }
    public BigDecimal percentage() {
        return total == 0 ? BigDecimal.ZERO.setScale(1) : BigDecimal.valueOf(read).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }
    public String percentageLabel() { return percentage().toPlainString().replace('.', ',') + "%"; }
    public String label() { return percentageLabel() + " — " + read + " de " + total + " edições lidas"; }
}
