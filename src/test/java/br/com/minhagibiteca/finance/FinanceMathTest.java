package br.com.minhagibiteca.finance;
import br.com.minhagibiteca.finance.domain.FinanceMath;
import br.com.minhagibiteca.finance.application.FinanceFilter;
import br.com.minhagibiteca.finance.presentation.FinanceLinks;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class FinanceMathTest {
 @Test void comparesEqualLengthAcrossLeapYear(){assertThat(FinanceMath.previousStart(LocalDate.of(2024,3,1),LocalDate.of(2024,3,31))).isEqualTo(LocalDate.of(2024,1,30));assertThat(FinanceMath.change(new BigDecimal("150"),new BigDecimal("100"))).isEqualTo("50,0%");assertThat(FinanceMath.change(BigDecimal.ZERO,new BigDecimal("100"))).isEqualTo("-100,0%");}
 @Test void doesNotInventPercentageWhenBaseIsZero(){assertThat(FinanceMath.change(new BigDecimal("50"),BigDecimal.ZERO)).isEqualTo("Sem base para percentual");assertThat(FinanceMath.ratio(BigDecimal.ZERO,BigDecimal.ZERO)).isEqualByComparingTo("0");}
 @Test void rejectsPartialReversedAndExcessivePeriods(){assertThatThrownBy(()->new FinanceFilter(LocalDate.of(2024,1,1),null,null,null,null,null,null)).isInstanceOf(FinanceFilter.InvalidFilter.class);assertThatThrownBy(()->new FinanceFilter(LocalDate.of(2024,2,1),LocalDate.of(2024,1,1),null,null,null,null,null)).isInstanceOf(FinanceFilter.InvalidFilter.class);assertThatThrownBy(()->new FinanceFilter(LocalDate.of(1900,1,1),LocalDate.of(2024,1,1),null,null,null,null,null)).isInstanceOf(FinanceFilter.InvalidFilter.class);}
 @Test void keepsFiltersInRankAndPageLinks(){var f=new FinanceFilter(LocalDate.of(2024,1,1),LocalDate.of(2024,1,31),null,null,null,null,null);var links=new FinanceLinks(f,true);assertThat(links.page(2)).contains("start=2024-01-01","end=2024-01-31","ranking=QUANTITY","page=2");assertThatThrownBy(()->links.refine("returnTo","https://invalid.example")).isInstanceOf(IllegalArgumentException.class);}
}
