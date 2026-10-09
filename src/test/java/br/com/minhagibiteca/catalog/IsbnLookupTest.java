package br.com.minhagibiteca.catalog;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.minhagibiteca.catalog.application.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class IsbnLookupTest {
    @Test void normalizesAndValidatesChecksums() {
        assertThat(IsbnLookupService.normalize("978-0-140-32872-1")).isEqualTo("9780140328721");
        assertThat(IsbnLookupService.normalize("0-8044-2957-x")).isEqualTo("080442957X");
        for (String invalid : List.of("", "123", "9780140328722", "0804429570", "https://example.com", "9780140328721<script>"))
            assertThatThrownBy(() -> IsbnLookupService.normalize(invalid)).isInstanceOf(IsbnLookupService.LookupException.class);
    }
    @Test void rejectsInvalidBeforeProviderAndCachesFoundData() {
        var provider = mock(IsbnProvider.class); var clock = mock(Clock.class);
        when(clock.instant()).thenReturn(Instant.EPOCH);
        var service = new IsbnLookupService(provider, clock);
        assertThatThrownBy(() -> service.lookup("123")).isInstanceOf(IsbnLookupService.LookupException.class);
        verifyNoInteractions(provider);
        var result = Optional.of(new IsbnMetadata("9780140328721", "Livro", 2000, List.of(), List.of(), false));
        when(provider.lookup("9780140328721")).thenReturn(result);
        assertThat(service.lookup("9780140328721")).isEqualTo(result);
        assertThat(service.lookup("978-0-140-32872-1")).isEqualTo(result);
        verify(provider, times(1)).lookup("9780140328721");
    }
    @Test void cachesMissingAndExpiresWithoutSleeping() {
        var provider = mock(IsbnProvider.class); var clock = mock(Clock.class);
        when(clock.instant()).thenReturn(Instant.EPOCH);
        when(provider.lookup(anyString())).thenReturn(Optional.empty());
        var service = new IsbnLookupService(provider, clock);
        assertThat(service.lookup("9780140328721")).isEmpty();
        assertThat(service.lookup("9780140328721")).isEmpty();
        verify(provider).lookup("9780140328721");
        when(clock.instant()).thenReturn(Instant.EPOCH.plusSeconds(601));
        assertThat(service.lookup("9780140328721")).isEmpty();
        verify(provider, times(2)).lookup("9780140328721");
    }
    @Test void rateLimitsUncachedLookupsAndDoesNotCacheFailure() {
        var provider = mock(IsbnProvider.class); var clock = mock(Clock.class);
        when(clock.instant()).thenReturn(Instant.EPOCH);
        when(provider.lookup("9780140328721")).thenThrow(new IsbnLookupService.LookupException("Indisponível"));
        var service = new IsbnLookupService(provider, clock);
        assertThatThrownBy(() -> service.lookup("9780140328721")).hasMessage("Indisponível");
        assertThatThrownBy(() -> service.lookup("9780132350884")).hasMessageContaining("Aguarde");
        verify(provider, never()).lookup("9780132350884");
        when(clock.instant()).thenReturn(Instant.EPOCH.plusSeconds(1));
        assertThatThrownBy(() -> service.lookup("9780140328721")).hasMessage("Indisponível");
        verify(provider, times(2)).lookup("9780140328721");
    }
}
