package br.com.minhagibiteca.dashboard.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.minhagibiteca.dashboard.infrastructure.HomeQueries;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HomeServiceTest {
    private final HomeQueries queries = mock(HomeQueries.class);
    private final HomeService service = new HomeService(queries);
    private final UUID owner = UUID.randomUUID();

    @Test void omitsEmptyShelvesAndUsesEmptyState() {
        var result = service.showcase(owner);
        assertThat(result.featured()).isNull();
        assertThat(result.shelves()).isEmpty();
        for (var category : HomeShelf.values()) verify(queries).shelf(owner, category);
        verifyNoMoreInteractions(queries);
    }
    @Test void prefersUnreadFeatureAndRetainsCategoryOrdering() {
        var recent = edition("Recentíssima", true);
        var unread = edition("Próxima leitura", false);
        when(queries.shelf(owner, HomeShelf.RECENT)).thenReturn(List.of(recent, unread));
        when(queries.shelf(owner, HomeShelf.UNREAD)).thenReturn(List.of(unread));
        var result = service.showcase(owner);
        assertThat(result.featured()).isEqualTo(unread);
        assertThat(result.shelves()).extracting(s -> s.category()).containsExactly(HomeShelf.RECENT, HomeShelf.UNREAD);
        assertThatThrownBy(() -> result.shelves().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.shelves().getFirst().editions().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
    @Test void fallsBackToRecentWhenEverythingIsReadAndLabelsUnknownMediaHonestly() {
        var recent = edition("Releitura", true);
        when(queries.shelf(owner, HomeShelf.RECENT)).thenReturn(List.of(recent));
        var result = service.showcase(owner);
        assertThat(result.featured()).isEqualTo(recent);
        assertThat(recent.mediaLabel()).isEqualTo("Suporte não informado");
        assertThat(recent.readingLabel()).isEqualTo("Já li");
    }
    private HomeEdition edition(String title, boolean read) {
        return new HomeEdition(UUID.randomUUID(), title, null, null, null, null, false, null, read);
    }
}
