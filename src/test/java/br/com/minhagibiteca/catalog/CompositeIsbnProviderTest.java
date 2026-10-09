package br.com.minhagibiteca.catalog;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.minhagibiteca.catalog.application.*;
import br.com.minhagibiteca.catalog.infrastructure.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CompositeIsbnProviderTest {
    private final OpenLibraryIsbnProvider primary = mock(OpenLibraryIsbnProvider.class);
    private final GoogleBooksIsbnProvider secondary = mock(GoogleBooksIsbnProvider.class);
    private final CompositeIsbnProvider provider = new CompositeIsbnProvider(primary, secondary);
    private final IsbnMetadata found = new IsbnMetadata("9786559601325", "Quadrinho", 2021, List.of(), List.of(), false, IsbnMetadata.Source.GOOGLE_BOOKS);
    @Test void primarySuccessDoesNotCallSecondary() {
        when(primary.lookup("9786559601325")).thenReturn(Optional.of(found));
        assertThat(provider.lookup("978-6559601325")).contains(found); verifyNoInteractions(secondary);
    }
    @Test void primaryFailureDoesNotCallSecondary() {
        when(primary.lookup(anyString())).thenThrow(new IsbnLookupService.LookupException("Indisponível"));
        assertThatThrownBy(() -> provider.lookup("9786559601325")).hasMessage("Indisponível"); verifyNoInteractions(secondary);
    }
    @Test void fallsBackOnlyForAbsenceAndCachesSecondaryResult() {
        when(primary.lookup(anyString())).thenReturn(Optional.empty()); when(secondary.lookup(anyString())).thenReturn(Optional.of(found));
        var clock = mock(Clock.class); when(clock.instant()).thenReturn(Instant.EPOCH);
        var service = new IsbnLookupService(provider, clock);
        assertThat(service.lookup("9786559601325")).contains(found); assertThat(service.lookup("978-6559601325")).contains(found);
        var order = inOrder(primary, secondary); order.verify(primary).lookup("9786559601325"); order.verify(secondary).lookup("9786559601325");
        verifyNoMoreInteractions(primary, secondary);
    }
    @Test void cachesOnlyCompleteAbsenceAndNotSecondaryFailure() {
        when(primary.lookup(anyString())).thenReturn(Optional.empty()); when(secondary.lookup(anyString())).thenReturn(Optional.empty());
        var clock = mock(Clock.class); when(clock.instant()).thenReturn(Instant.EPOCH);
        var service = new IsbnLookupService(provider, clock);
        assertThat(service.lookup("9786559601325")).isEmpty(); assertThat(service.lookup("9786559601325")).isEmpty();
        verify(secondary).lookup("9786559601325");
        when(clock.instant()).thenReturn(Instant.EPOCH.plusSeconds(601));
        when(secondary.lookup(anyString())).thenThrow(new IsbnLookupService.LookupException("Cota"));
        assertThatThrownBy(() -> service.lookup("9786559601325")).hasMessage("Cota");
        when(clock.instant()).thenReturn(Instant.EPOCH.plusSeconds(602));
        assertThatThrownBy(() -> service.lookup("9786559601325")).hasMessage("Cota");
        verify(secondary, times(3)).lookup("9786559601325");
    }
    @Test void canonicalConversionAndInvalidLookupWithoutNetwork() {
        assertThat(IsbnLookupService.canonical("0140328726")).isEqualTo("9780140328721");
        assertThat(IsbnLookupService.canonical("080442957X")).isEqualTo("9780804429573");
        assertThat(IsbnLookupService.canonical("9791090636071")).isEqualTo("9791090636071");
        assertThatThrownBy(() -> provider.lookup("123")).isInstanceOf(IsbnLookupService.LookupException.class); verifyNoInteractions(primary, secondary);
    }
}
