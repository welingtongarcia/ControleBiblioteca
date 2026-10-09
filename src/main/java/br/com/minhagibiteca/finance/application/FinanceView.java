package br.com.minhagibiteca.finance.application;
import br.com.minhagibiteca.finance.domain.FinanceMath;
import java.math.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.data.domain.Page;
public final class FinanceView {
 private FinanceView(){}
 public record Option(UUID id,String name){}
 public record Purchase(UUID id,UUID editionId,String title,BigDecimal amount,LocalDate purchaseDate,String vendor,String notes) {
  public String getMoney(){return FinanceMath.money(amount);}
  public String getDateLabel(){return purchaseDate==null?"Não informada":purchaseDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));}
 }
 public record Summary(long count,BigDecimal total,long undatedCount,BigDecimal undatedAmount,BigDecimal unreadAmount) {
  public String getMoney(){return FinanceMath.money(total);}
  public String getAverage(){return count==0?"Não informado":FinanceMath.money(total.divide(BigDecimal.valueOf(count),2,RoundingMode.HALF_UP));}
  public String getUndatedMoney(){return FinanceMath.money(undatedAmount);}
  public String getUnreadMoney(){return FinanceMath.money(unreadAmount);}
 }
 public record Bucket(String key,String label,long count,BigDecimal amount) { public String getMoney(){return FinanceMath.money(amount);} }
 public record ChartItem(Bucket bucket,BigDecimal width,BigDecimal share,double offset) {
  public String getDash(){return share.doubleValue()*4.398229715 + " 439.8229715";}
  public String getOffset(){return Double.toString(-offset*4.398229715);}
 }
 public record Chart(String title,String parameter,List<ChartItem> items,boolean shared,boolean ring,boolean truncated,boolean quantity) {}
 public record Monthly(List<Bucket> items,String points,String range,String peak) {
  public String getMaximumMoney(){return FinanceMath.money(items.stream().map(Bucket::amount).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO));}
 }
 public record Comparison(Summary previous,String period,BigDecimal difference,String change,BigDecimal currentWidth,BigDecimal previousWidth) {
  public String getDifferenceMoney(){return FinanceMath.money(difference);}
 }
 public record Report(Summary summary,Summary coverage,Page<Purchase> purchases,List<Chart> charts,Monthly monthly,Comparison comparison){}
}
