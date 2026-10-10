package br.com.minhagibiteca.catalog;

import br.com.minhagibiteca.catalog.application.AssociationOptionsService;
import br.com.minhagibiteca.catalog.application.AssociationOptionsService.*;
import br.com.minhagibiteca.catalog.domain.CreatorFunction;
import br.com.minhagibiteca.catalog.infrastructure.AssociationOptionsRepository;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AssociationOptionsServiceTest {
    private final AssociationOptionsRepository repository = mock(AssociationOptionsRepository.class);
    private final AssociationOptionsService service = new AssociationOptionsService(repository);
    private final UUID owner = UUID.randomUUID();
    @Test void rejectsUnboundedAndInapplicableParametersBeforeQuery() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.search(owner, Kind.WORK, "x".repeat(181), null, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> service.search(owner, Kind.WORK, "", null, -1));
        assertThatIllegalArgumentException().isThrownBy(() -> service.search(owner, Kind.WORK, "", CreatorFunction.CAPISTA, 0));
        verifyNoInteractions(repository);
    }
    @Test void returnsAtMostTwentyOptionsWithNextPageIndicator() {
        var rows = java.util.stream.IntStream.range(0, 21).mapToObj(i -> new Option(UUID.randomUUID(), "Obra " + i, "")).toList();
        when(repository.search(owner, Kind.WORK, "obra", null, 0)).thenReturn(rows);
        var result = service.search(owner, Kind.WORK, " obra ", null, 0);
        assertThat(result.items()).hasSize(20); assertThat(result.hasNext()).isTrue(); assertThat(result.page()).isZero();
    }
    @Test void mergesAuthorizedSelectionsWithFirstPageWithoutDuplicates() {
        var selected = new Option(UUID.randomUUID(), "Escolhida", "");
        var another = new Option(UUID.randomUUID(), "Outra", "");
        when(repository.selected(owner, Kind.WORK, List.of(selected.id()))).thenReturn(List.of(selected));
        when(repository.search(owner, Kind.WORK, "", null, 0)).thenReturn(List.of(selected, another));
        assertThat(service.forForm(owner, Kind.WORK, List.of(selected.id(), selected.id()))).containsExactly(selected, another);
    }
}
