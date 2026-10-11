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
        BigDecimal coverPrice, BigDecimal referencePaidPrice, boolean read, java.time.LocalDate purchaseDate,
        Integer totalPages, int pagesRead, Integer rating, br.com.minhagibiteca.catalog.domain.PublicationType publicationType) {
    static EditionView from(Edition edition) {
        return new EditionView(edition.getId(), edition.getPublisherId(), edition.getSeriesId(),
                edition.getTitle(), edition.getEditorialNumber(), edition.getVariant(), edition.getIsbn(),
                edition.getPublicationYear(), edition.getFormat(), edition.getNotes(), edition.getCoverFileName(),
                edition.getMediaType(), edition.getCoverPrice(), edition.getReferencePaidPrice(), edition.isRead(), edition.getPurchaseDate(), edition.getTotalPages(), edition.getPagesRead(), edition.getRating(), edition.getPublicationType());
    }
    public String getPurchaseDateLabel(){return purchaseDate==null?"Não informada":purchaseDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));}
    public String getMediaLabel() { return mediaType == null ? "Não informado" : mediaType.getLabel(); }
    public String getPublicationTypeLabel() { return publicationType.getLabel(); }
    public String getRatingLabel() { return rating == null ? "Sem avaliação" : rating + " de 5 estrelas"; }
    public String getRatingStars() { return "★".repeat(rating == null ? 0 : rating) + "☆".repeat(5 - (rating == null ? 0 : rating)); }
    public String getFormattedCoverPrice() { return money(coverPrice); }
    public String getFormattedReferencePaidPrice() { return money(referencePaidPrice); }
    private String money(BigDecimal amount) {
        return amount == null ? "Não informado" : NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(amount);
    }
}
