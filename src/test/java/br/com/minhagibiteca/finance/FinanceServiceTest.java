package br.com.minhagibiteca.finance;
import br.com.minhagibiteca.finance.application.*;
import br.com.minhagibiteca.finance.application.FinanceView.*;
import br.com.minhagibiteca.finance.infrastructure.FinanceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class FinanceServiceTest {
 @Test void editionDateCanBeUnknownAndRejectsInvalidRange(){
  var e=br.com.minhagibiteca.catalog.domain.Edition.create(UUID.randomUUID(),null,null,"HQ",null,null,null,null,null,null);
  e.updatePurchaseDate(LocalDate.of(2026,6,1));assertThat(e.getPurchaseDate()).isEqualTo(LocalDate.of(2026,6,1));
  for(LocalDate date:java.util.List.of(LocalDate.of(1899,1,1),LocalDate.now().plusDays(1)))assertThatThrownBy(()->e.updatePurchaseDate(date)).isInstanceOf(IllegalArgumentException.class);
  e.updatePurchaseDate(null);assertThat(e.getPurchaseDate()).isNull();
 }
 @Test void averageExcludesMissingPriceButIncludesFreeEdition(){var s=new Summary(3,new BigDecimal("20"),0,BigDecimal.ZERO,BigDecimal.ZERO,2);assertThat(s.getAverage()).contains("10,00");assertThat(s.getUnknownCount()).isEqualTo(1);}
 @Test void allUnknownIsNotAFreeCollection(){var s=new Summary(2,BigDecimal.ZERO,2,BigDecimal.ZERO,BigDecimal.ZERO,0);assertThat(s.getAverage()).isEqualTo("Não informado");assertThat(new Entry(UUID.randomUUID(),"HQ",null,null).getMoney()).isEqualTo("Não informado");}
 @Test void invalidPeriodNeverQueriesAggregates(){var repo=mock(FinanceRepository.class);var service=new FinanceService(repo);assertThatThrownBy(()->service.report(UUID.randomUUID(),new FinanceFilter(LocalDate.of(1899,1,1),LocalDate.of(1899,2,1),null,null,null,null,null),0)).isInstanceOf(FinanceFilter.InvalidFilter.class);verifyNoInteractions(repo);}
}
