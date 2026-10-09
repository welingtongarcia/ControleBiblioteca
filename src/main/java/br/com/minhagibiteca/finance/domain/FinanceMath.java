package br.com.minhagibiteca.finance.domain;
import java.math.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
public final class FinanceMath {
 private FinanceMath() {}
 public static String money(BigDecimal amount) { return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(amount); }
 public static BigDecimal ratio(BigDecimal part, BigDecimal total) { return total.signum()==0 ? BigDecimal.ZERO : part.multiply(BigDecimal.valueOf(100)).divide(total,2,RoundingMode.HALF_UP); }
 public static String percentage(BigDecimal value) { return value.setScale(1,RoundingMode.HALF_UP).toPlainString().replace('.',',') + "%"; }
 public static LocalDate previousStart(LocalDate start,LocalDate end) { return start.minusDays(ChronoUnit.DAYS.between(start,end)+1); }
 public static String change(BigDecimal current,BigDecimal previous) { return previous.signum()==0 ? "Sem base para percentual" : percentage(ratio(current.subtract(previous),previous)); }
}
