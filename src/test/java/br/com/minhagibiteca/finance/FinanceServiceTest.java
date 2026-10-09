package br.com.minhagibiteca.finance;
import br.com.minhagibiteca.finance.application.*;
import br.com.minhagibiteca.finance.infrastructure.FinanceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class FinanceServiceTest {
 @Test void invalidAmountNeverWrites(){var repo=mock(FinanceRepository.class);var service=new FinanceService(repo);var owner=UUID.randomUUID();assertThatThrownBy(()->service.save(owner,null,new FinanceService.Command(UUID.randomUUID(),new BigDecimal("-1"),null,null,null))).isInstanceOf(FinanceService.InvalidPurchase.class);verify(repo,never()).save(any(),any(),any(),any(),any(),any(),any(),anyBoolean());}
 @Test void futureAndAncientDatesNeverWrite(){var repo=mock(FinanceRepository.class);var service=new FinanceService(repo);var owner=UUID.randomUUID();for(LocalDate date:java.util.List.of(LocalDate.now().plusDays(1),LocalDate.of(1899,1,1)))assertThatThrownBy(()->service.save(owner,null,new FinanceService.Command(UUID.randomUUID(),BigDecimal.ZERO,date,null,null))).isInstanceOf(FinanceService.InvalidPurchase.class);verify(repo,never()).save(any(),any(),any(),any(),any(),any(),any(),anyBoolean());}
 @Test void preservesMissingDateAndZeroAndNormalizesOptionalText(){var repo=mock(FinanceRepository.class);var service=new FinanceService(repo);var owner=UUID.randomUUID();var edition=UUID.randomUUID();UUID id=service.save(owner,null,new FinanceService.Command(edition,BigDecimal.ZERO,null," "," Minha nota "));verify(repo).save(owner,id,edition,BigDecimal.ZERO,null,null,"Minha nota",false);}
}
