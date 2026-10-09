package br.com.minhagibiteca.finance.presentation;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
public class PurchaseForm {
 @NotNull(message="Selecione a edição comprada.") private UUID editionId;
 @NotNull(message="Informe o valor da compra.") @DecimalMin(value="0.00",message="O valor não pode ser negativo.") @Digits(integer=10,fraction=2,message="Use até 10 dígitos inteiros e duas casas decimais.") private BigDecimal amount;
 @PastOrPresent(message="A data da compra não pode ser futura.") @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) private LocalDate purchaseDate;
 @Size(max=160) private String vendor;
 @Size(max=2000) private String notes;
 public UUID getEditionId(){return editionId;}public void setEditionId(UUID x){editionId=x;}
 public BigDecimal getAmount(){return amount;}public void setAmount(BigDecimal x){amount=x;}
 public LocalDate getPurchaseDate(){return purchaseDate;}public void setPurchaseDate(LocalDate x){purchaseDate=x;}
 public String getVendor(){return vendor;}public void setVendor(String x){vendor=x;}
 public String getNotes(){return notes;}public void setNotes(String x){notes=x;}
}
