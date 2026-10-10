package br.com.minhagibiteca.catalog;

import br.com.minhagibiteca.catalog.application.CreatorService;
import br.com.minhagibiteca.catalog.domain.Creator;
import br.com.minhagibiteca.catalog.domain.CreatorFunction;
import br.com.minhagibiteca.catalog.infrastructure.CreatorRepository;
import br.com.minhagibiteca.shared.application.ResourceNotFoundException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreatorFunctionsTest {
    @Test void allowsMultipleOptionalFunctionsAndDefensiveCopies() {
        var functions = new HashSet<>(Set.of(CreatorFunction.DESENHISTA, CreatorFunction.ROTEIRISTA));
        var command = new CreatorService.Command("Artista", null, functions);
        functions.clear();
        var creator = Creator.create(UUID.randomUUID(), "Artista", null);
        creator.replaceFunctions(command.functions());
        assertThat(creator.getFunctionsDescription()).isEqualTo("Roteirista, Desenhista");
        assertThatThrownBy(() -> creator.getFunctions().clear()).isInstanceOf(UnsupportedOperationException.class);
        creator.replaceFunctions(null);
        assertThat(creator.getFunctions()).isEmpty();
        assertThat(creator.getFunctionsDescription()).isEqualTo("Funções não informadas");
    }

    @Test void serviceSavesAndClearsFunctionsWithoutChangingOwnership() {
        var repository = mock(CreatorRepository.class);
        var service = new CreatorService(repository);
        var owner = UUID.randomUUID();
        when(repository.save(any(Creator.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var creator = service.create(owner, new CreatorService.Command("Artista", null, Set.of(CreatorFunction.CAPISTA)));
        assertThat(creator.getFunctions()).containsExactly(CreatorFunction.CAPISTA);
        assertThat(creator.getOwnerUserId()).isEqualTo(owner);
        when(repository.findByIdAndOwnerUserId(creator.getId(), owner)).thenReturn(Optional.of(creator));
        service.update(creator.getId(), owner, new CreatorService.Command("Artista", null, Set.of()));
        assertThat(creator.getFunctions()).isEmpty();
        assertThatThrownBy(() -> service.update(creator.getId(), UUID.randomUUID(),
                new CreatorService.Command("Artista", null, Set.of(CreatorFunction.EDITOR))))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(creator.getFunctions()).isEmpty();
    }
}
