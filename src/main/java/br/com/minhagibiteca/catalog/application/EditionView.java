package br.com.minhagibiteca.catalog.application;

import br.com.minhagibiteca.catalog.domain.Edition;
import br.com.minhagibiteca.catalog.domain.MediaType;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;

public record EditionView(UUID id, UUID publisherId, UUID seriesId, String title,
        String editorialNumber, String variant, String isbn, Integer publicationYear,
        String format, String notes, String coverFileName, MediaType mediaType,
        BigDecimal coverPrice, BigDecimal referencePaidPrice, boolean read) {
    static EditionView from(Edition edition) {
        return new EditionView(edition.getId(), edition.getPublisherId(), edition.getSeriesId(),
                edition.getTitle(), edition.getEditorialNumber(), edition.getVariant(), edition.getIsbn(),
                edition.getPublicationYear(), edition.getFormat(), edition.getNotes(), edition.getCoverFileName(),
                edition.getMediaType(), edition.getCoverPrice(), edition.getReferencePaidPrice(), edition.isRead());
    }
    public String getMediaLabel() { return mediaType == null ? "Não informado" : mediaType.getLabel(); }
    public String getFormattedCoverPrice() { return money(coverPrice); }
    public String getFormattedReferencePaidPrice() { return money(referencePaidPrice); }
    private String money(BigDecimal amount) {
        return amount == null ? "Não informado" : NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(amount);
    }
}
