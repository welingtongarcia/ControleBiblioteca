package br.com.minhagibiteca.catalog;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.minhagibiteca.catalog.application.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class MetadataSearchTest {
    private final UUID owner = UUID.randomUUID();
    private final MetadataCandidate candidate = new MetadataCandidate("OL123M", "9780140328721", "Livro", 2000, List.of(), List.of(), MetadataCandidate.Source.OPEN_LIBRARY);
    @Test void cachesNormalizesAndSelectsWithoutAnotherRequest() {
        var provider = mock(MetadataSearchProvider.class); var clock = mock(Clock.class);
        when(clock.instant()).thenReturn(Instant.EPOCH);
        when(provider.search(MetadataCandidate.Source.OPEN_LIBRARY, "Batman Ano Um")).thenReturn(List.of(candidate));
        var service = new MetadataSearchService(provider, clock);
        var result = service.search(owner, MetadataCandidate.Source.OPEN_LIBRARY, "  Batman  Ano Um  ");
        assertThat(service.search(owner, MetadataCandidate.Source.OPEN_LIBRARY, "Batman Ano Um")).isEqualTo(result);
        assertThat(service.select(owner, result.token().toString(), 0)).isEqualTo(candidate);
        verify(provider).search(MetadataCandidate.Source.OPEN_LIBRARY, "Batman Ano Um"); verifyNoMoreInteractions(provider);
    }
    @Test void rejectsOtherOwnerBadTokenBadIndexAndExpiredSelection() {
        var provider = mock(MetadataSearchProvider.class); var clock = mock(Clock.class);
        when(clock.instant()).thenReturn(Instant.EPOCH); when(provider.search(any(), any())).thenReturn(List.of(candidate));
        var service = new MetadataSearchService(provider, clock);
        var result = service.search(owner, MetadataCandidate.Source.OPEN_LIBRARY, "Batman");
        assertThatThrownBy(() -> service.select(UUID.randomUUID(), result.token().toString(), 0)).hasMessageContaining("inválida");
        for (int index : List.of(-1, 1, Integer.MAX_VALUE)) assertThatThrownBy(() -> service.select(owner, result.token().toString(), index)).hasMessageContaining("inválida");
        assertThatThrownBy(() -> service.select(owner, "bad", 0)).hasMessageContaining("inválida");
        when(clock.instant()).thenReturn(Instant.EPOCH.plusSeconds(600));
        assertThatThrownBy(() -> service.select(owner, result.token().toString(), 0)).hasMessageContaining("expirada");
    }
    @Test void validatesBeforeNetworkLimitsRequestsAndDoesNotCacheErrors() {
        var provider = mock(MetadataSearchProvider.class); var clock = mock(Clock.class);
        when(clock.instant()).thenReturn(Instant.EPOCH);
        var service = new MetadataSearchService(provider, clock);
        for (String query : List.of("", "ab", "a".repeat(241), "abc\n")) assertThatThrownBy(() -> service.search(owner, MetadataCandidate.Source.OPEN_LIBRARY, query)).isInstanceOf(MetadataSearchService.SearchException.class);
        verifyNoInteractions(provider);
        when(provider.search(any(), any())).thenThrow(new MetadataSearchService.SearchException("Indisponível"));
        assertThatThrownBy(() -> service.search(owner, MetadataCandidate.Source.OPEN_LIBRARY, "Batman")).hasMessage("Indisponível");
        assertThatThrownBy(() -> service.search(owner, MetadataCandidate.Source.OPEN_LIBRARY, "Batman")).hasMessageContaining("Aguarde");
        when(clock.instant()).thenReturn(Instant.EPOCH.plusSeconds(1));
        assertThatThrownBy(() -> service.search(owner, MetadataCandidate.Source.OPEN_LIBRARY, "Batman")).hasMessage("Indisponível");
        verify(provider, times(2)).search(any(), any());
    }
    @Test void boundsResultsAndRejectsUnsafeIds() {
        var provider = mock(MetadataSearchProvider.class);
        when(provider.search(any(), any())).thenReturn(Collections.nCopies(20, candidate));
        assertThat(new MetadataSearchService(provider).search(owner, MetadataCandidate.Source.OPEN_LIBRARY, "Batman").candidates()).hasSize(5);
        assertThatThrownBy(() -> new MetadataCandidate("https://evil", null, "HQ", null, List.of(), List.of(), MetadataCandidate.Source.GOOGLE_BOOKS)).isInstanceOf(IllegalArgumentException.class);
    }
}
