package br.com.minhagibiteca.dashboard.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import br.com.minhagibiteca.dashboard.infrastructure.DashboardQueries;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CollectionSummaryTest {
    @Test void distinguishesEmptyUnknownAndFree() {
        assertThat(new CollectionSummary(0, 0, 0, 0, null).totalPaidLabel()).contains("R$", "0,00");
        var unknown = new CollectionSummary(1, 2, 1, 0, null);
        assertThat(unknown.totalPaidLabel()).isEqualTo("Não informado");
        assertThat(unknown.unpricedCopies()).isEqualTo(2);
        assertThat(new CollectionSummary(1, 1, 1, 1, BigDecimal.ZERO).totalPaidLabel()).contains("R$", "0,00");
    }
    @Test void formatsLargeDecimalTotalsWithoutIndividualPriceLimit() {
        assertThat(new CollectionSummary(1, 2, 1, 2, new BigDecimal("19999999999.98")).totalPaidLabel())
                .contains("19.999.999.999,98");
    }
    @Test void delegatesWithExactOwner() {
        var queries = mock(DashboardQueries.class);
        var owner = UUID.randomUUID();
        var summary = new CollectionSummary(0, 0, 0, 0, null);
        when(queries.summary(owner)).thenReturn(summary);
        assertThat(new DashboardService(queries).summary(owner)).isSameAs(summary);
        verify(queries).summary(owner);
        verifyNoMoreInteractions(queries);
    }
}
