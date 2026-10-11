package br.com.minhagibiteca.catalog.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Entity @Table(name = "editions")
public class Edition {
 @Id private UUID id; @Column(name="owner_user_id",nullable=false,updatable=false) private UUID ownerUserId;
 @Column(name="publisher_id") private UUID publisherId; @Column(name="series_id") private UUID seriesId;
 @Column(nullable=false,length=240) private String title; @Column(name="normalized_title",nullable=false,length=240) private String normalizedTitle;
 @Column(name="editorial_number",length=60) private String editorialNumber; @Column(length=120) private String variant;
 @Column(length=32) private String isbn; @Column(name="publication_year") private Integer publicationYear;
 @Column(name="format",length=60) private String format; @Column(length=2000) private String notes;
 @Enumerated(EnumType.STRING) @Column(name="media_type",length=20) private MediaType mediaType;
 @Column(name="cover_price",precision=12,scale=2) private BigDecimal coverPrice;
 @Column(name="reference_paid_price",precision=12,scale=2) private BigDecimal referencePaidPrice;
 @Column(name="purchase_date") private LocalDate purchaseDate;
 @Column(name="is_read",nullable=false) private boolean read;
 @Column(name="total_pages") private Integer totalPages;
 @Column(name="pages_read",nullable=false) private int pagesRead;
 @Column(name="rating") private Integer rating;
 @Enumerated(EnumType.STRING) @Column(name="publication_type",nullable=false,length=20) private PublicationType publicationType = PublicationType.COMIC;
 @Column(name="cover_file_name",length=255) private String coverFileName; @Column(name="cover_content_type",length=100) private String coverContentType; @Column(name="cover_updated_at") private Instant coverUpdatedAt;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt; @Column(name="updated_at",nullable=false) private Instant updatedAt;
 protected Edition() { }
 private Edition(UUID owner, UUID publisher, UUID series, String title, String number, String variant, String isbn, Integer year, String format, String notes) { id=UUID.randomUUID(); ownerUserId=owner; update(publisher,series,title,number,variant,isbn,year,format,notes); createdAt=Instant.now(); }
 public static Edition create(UUID owner, UUID publisher, UUID series, String title, String number, String variant, String isbn, Integer year, String format, String notes) { return new Edition(owner,publisher,series,title,number,variant,isbn,year,format,notes); }
 public void update(UUID publisher, UUID series, String title, String number, String variant, String isbn, Integer year, String format, String notes) { publisherId=publisher; seriesId=series; this.title=title.trim(); normalizedTitle=title.trim().toLowerCase(Locale.ROOT); editorialNumber=blank(number); this.variant=blank(variant); this.isbn=blank(isbn); publicationYear=year; this.format=blank(format); this.notes=blank(notes); updatedAt=Instant.now(); }
 public void updateCover(String fileName, String contentType) { coverFileName=fileName; coverContentType=contentType; coverUpdatedAt=Instant.now(); updatedAt=Instant.now(); }
 public void updatePricingAndMedia(MediaType mediaType, BigDecimal coverPrice, BigDecimal referencePaidPrice) {
     validatePrice(coverPrice); validatePrice(referencePaidPrice);
     this.mediaType = mediaType; this.coverPrice = coverPrice; this.referencePaidPrice = referencePaidPrice;
 }
 private static void validatePrice(BigDecimal price) {
     if (price != null && (price.signum() < 0 || price.scale() > 2 || price.compareTo(new BigDecimal("9999999999.99")) > 0)) {
         throw new IllegalArgumentException("Informe um valor não negativo com até duas casas decimais.");
     }
 }
 public void updatePurchaseDate(LocalDate date) {
     if(date!=null && (date.isBefore(LocalDate.of(1900,1,1)) || date.isAfter(LocalDate.now()))) throw new IllegalArgumentException("Informe a data da compra entre 1900 e hoje, ou deixe em branco.");
     purchaseDate=date;
 }
 public LocalDate getPurchaseDate(){return purchaseDate;}
 public MediaType getMediaType() { return mediaType; }
 public boolean isRead() { return read; }
 public void markRead(boolean read) { this.read = read; this.pagesRead = read && totalPages != null ? totalPages : 0; this.updatedAt = Instant.now(); }
 public void updatePages(Integer total, int completed) {
     if (completed < 0 || (total == null && completed != 0) || (total != null && (total < 1 || total > 100000 || completed > total))) {
         throw new IllegalArgumentException("Informe um total de 1 a 100.000 páginas e páginas lidas entre zero e o total. Sem total, use zero páginas lidas.");
     }
     totalPages = total; pagesRead = completed;
     if (total != null) read = completed == total;
     updatedAt = Instant.now();
 }
 public Integer getTotalPages() { return totalPages; }
 public int getPagesRead() { return pagesRead; }
 public Integer getRating() { return rating; }
 public PublicationType getPublicationType() { return publicationType; }
 public void updatePublicationType(PublicationType type) { publicationType = java.util.Objects.requireNonNull(type, "Informe o tipo de publicação."); updatedAt = Instant.now(); }
 public void rate(Integer rating) {
     if (rating != null && (rating < 0 || rating > 5)) throw new IllegalArgumentException("Informe uma nota inteira de 0 a 5 estrelas.");
     this.rating = rating;
     updatedAt = Instant.now();
 }
 public BigDecimal getCoverPrice() { return coverPrice; }
 public BigDecimal getReferencePaidPrice() { return referencePaidPrice; }
 private static String blank(String s){return s==null||s.isBlank()?null:s.trim();}
 public UUID getId(){return id;} public UUID getOwnerUserId(){return ownerUserId;} public UUID getPublisherId(){return publisherId;} public UUID getSeriesId(){return seriesId;} public String getTitle(){return title;} public String getEditorialNumber(){return editorialNumber;} public String getVariant(){return variant;} public String getIsbn(){return isbn;} public Integer getPublicationYear(){return publicationYear;} public String getFormat(){return format;} public String getNotes(){return notes;} public String getCoverFileName(){return coverFileName;} public String getCoverContentType(){return coverContentType;}
}
