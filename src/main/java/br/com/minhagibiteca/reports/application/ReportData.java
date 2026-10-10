package br.com.minhagibiteca.reports.application;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;

public record ReportData(Summary summary, Page<Entry> entries, List<String> filters, ZonedDateTime emittedAt) {
    public String emissionLabel() { return emittedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss 'BRT'")); }
    public record Summary(long editions, long physical, long digital, long unknownMedia, long copies,
            BigDecimal paid, long priced, long read, long totalPages, long pagesRead, long paged) {
        public long unpriced() { return editions - priced; }
        public long unpaged() { return editions - paged; }
        public long pagesUnread() { return totalPages - pagesRead; }
        public String paidLabel() { return editions > 0 && priced == 0 ? "Não informado" : money(paid); }
    }
    public record Entry(UUID id, String title, String number, String isbn, String series, String publisher,
            String creators, String characters, String media, boolean read, Integer totalPages, int pagesRead,
            BigDecimal paid, LocalDate purchaseDate, long copies, String locations) {
        public String paidLabel() { return paid == null ? "Não informado" : money(paid); }
        public String readingLabel() { return read ? "Lida" : "Não lida"; }
        public String dateLabel() { return purchaseDate == null ? "Não informada" : purchaseDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
        public String pagesLabel() { return totalPages == null ? "Não informadas" : pagesRead + " de " + totalPages + " lidas"; }
        public Integer pagesUnread() { return totalPages == null ? null : totalPages - pagesRead; }
    }
    public record Option(UUID id, String name) { }
    public static String money(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(value == null ? BigDecimal.ZERO : value);
    }
}
