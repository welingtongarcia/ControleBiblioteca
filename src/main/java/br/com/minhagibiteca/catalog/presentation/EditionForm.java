package br.com.minhagibiteca.catalog.presentation;
import jakarta.validation.constraints.*; import java.util.List; import java.util.UUID;
import br.com.minhagibiteca.catalog.domain.MediaType;
import java.math.BigDecimal;
public class EditionForm { private UUID publisherId,seriesId; private List<UUID> workIds = List.of(), creatorIds = List.of(), characterIds = List.of(); @NotBlank(message="Informe o título da edição.") @Size(max=240) private String title; @Size(max=60) private String editorialNumber; @Size(max=120) private String variant; @Size(max=32) private String isbn; @Min(1800) @Max(2100) private Integer publicationYear; @Size(max=60) private String format; @Size(max=2000) private String notes;
 @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
 @PastOrPresent(message="A data da compra não pode estar no futuro.")
 private java.time.LocalDate purchaseDate;
 @AssertTrue(message="A data da compra deve ser a partir de 1900.")
 public boolean isPurchaseDateValid(){return purchaseDate==null || !purchaseDate.isBefore(java.time.LocalDate.of(1900,1,1));}
 public java.time.LocalDate getPurchaseDate(){return purchaseDate;} public void setPurchaseDate(java.time.LocalDate value){purchaseDate=value;}
 private MediaType mediaType = MediaType.PHYSICAL;
 @Min(value=1,message="Informe pelo menos uma página.") @Max(value=100000,message="O total máximo é 100.000 páginas.") private Integer totalPages;
 @NotNull(message="Informe as páginas lidas, ou zero.") @Min(value=0,message="As páginas lidas não podem ser negativas.") @Max(value=100000,message="O máximo é 100.000 páginas.") private Integer pagesRead = 0;
 @AssertTrue(message="As páginas lidas não podem ultrapassar o total. Sem total informado, use zero páginas lidas.")
 public boolean isPageReadingValid(){return pagesRead==null || (totalPages==null ? pagesRead==0 : pagesRead<=totalPages);}
 public Integer getTotalPages(){return totalPages;} public void setTotalPages(Integer value){totalPages=value;}
 public Integer getPagesRead(){return pagesRead;} public void setPagesRead(Integer value){pagesRead=value;}
 @DecimalMin(value="0.00",message="O preço de capa não pode ser negativo.") @Digits(integer=10,fraction=2,message="Informe até 10 dígitos inteiros e 2 casas decimais.") private BigDecimal coverPrice;
 @DecimalMin(value="0.00",message="O valor pago não pode ser negativo.") @Digits(integer=10,fraction=2,message="Informe até 10 dígitos inteiros e 2 casas decimais.") private BigDecimal referencePaidPrice;
 public MediaType getMediaType(){return mediaType;} public void setMediaType(MediaType value){mediaType=value;}
 public BigDecimal getCoverPrice(){return coverPrice;} public void setCoverPrice(BigDecimal value){coverPrice=value;}
 public BigDecimal getReferencePaidPrice(){return referencePaidPrice;} public void setReferencePaidPrice(BigDecimal value){referencePaidPrice=value;}
 public UUID getPublisherId(){return publisherId;} public void setPublisherId(UUID v){publisherId=v;} public UUID getSeriesId(){return seriesId;} public void setSeriesId(UUID v){seriesId=v;} public List<UUID> getCharacterIds(){return characterIds;} public void setCharacterIds(List<UUID> v){characterIds=v;} public List<UUID> getWorkIds(){return workIds;} public void setWorkIds(List<UUID> v){workIds=v;} public List<UUID> getCreatorIds(){return creatorIds;} public void setCreatorIds(List<UUID> v){creatorIds=v;} public String getTitle(){return title;} public void setTitle(String v){title=v;} public String getEditorialNumber(){return editorialNumber;} public void setEditorialNumber(String v){editorialNumber=v;} public String getVariant(){return variant;} public void setVariant(String v){variant=v;} public String getIsbn(){return isbn;} public void setIsbn(String v){isbn=v;} public Integer getPublicationYear(){return publicationYear;} public void setPublicationYear(Integer v){publicationYear=v;} public String getFormat(){return format;} public void setFormat(String v){format=v;} public String getNotes(){return notes;} public void setNotes(String v){notes=v;} }
