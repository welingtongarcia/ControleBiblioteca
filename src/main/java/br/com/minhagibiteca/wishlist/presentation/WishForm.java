package br.com.minhagibiteca.wishlist.presentation;

import br.com.minhagibiteca.catalog.domain.PublicationType;
import br.com.minhagibiteca.wishlist.domain.WishPriority;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class WishForm {
    @NotBlank(message="Informe o título.") @Size(max=240,message="Use até 240 caracteres.")
    private String title;
    @NotNull(message="Informe o tipo de publicação.") private PublicationType publicationType=PublicationType.COMIC;
    @Size(max=180,message="Use até 180 caracteres.") private String author;
    @Size(max=180,message="Use até 180 caracteres.") private String publisher;
    @Size(max=32,message="Use até 32 caracteres.") private String isbn;
    @DecimalMin(value="0",message="O preço não pode ser negativo.") @Digits(integer=10,fraction=2,message="Use até duas casas decimais e dez dígitos inteiros.")
    private BigDecimal estimatedPrice;
    @NotNull(message="Informe a prioridade.") private WishPriority priority=WishPriority.NORMAL;
    @Size(max=2000,message="Use até 2.000 caracteres.") private String notes;
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public PublicationType getPublicationType(){return publicationType;} public void setPublicationType(PublicationType v){publicationType=v;}
    public String getAuthor(){return author;} public void setAuthor(String v){author=v;}
    public String getPublisher(){return publisher;} public void setPublisher(String v){publisher=v;}
    public String getIsbn(){return isbn;} public void setIsbn(String v){isbn=v;}
    public BigDecimal getEstimatedPrice(){return estimatedPrice;} public void setEstimatedPrice(BigDecimal v){estimatedPrice=v;}
    public WishPriority getPriority(){return priority;} public void setPriority(WishPriority v){priority=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
}
