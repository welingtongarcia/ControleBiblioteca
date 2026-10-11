package br.com.minhagibiteca.catalog;

import br.com.minhagibiteca.catalog.application.EditionRatingService;
import br.com.minhagibiteca.catalog.domain.Edition;
import br.com.minhagibiteca.catalog.infrastructure.EditionRepository;
import br.com.minhagibiteca.shared.application.ResourceNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EditionRatingTest {
    @Test void acceptsEveryWholeScoreAndDistinguishesZeroFromAbsence() {
        var edition = Edition.create(UUID.randomUUID(), null, null, "HQ", null, null, null, null, null, null);
        assertThat(edition.getRating()).isNull();
        for (int rating = 0; rating <= 5; rating++) {
            edition.rate(rating);
            assertThat(edition.getRating()).isEqualTo(rating);
        }
        for (int invalid : new int[]{-1, 6, Integer.MAX_VALUE}) {
            assertThatThrownBy(() -> edition.rate(invalid)).isInstanceOf(IllegalArgumentException.class);
            assertThat(edition.getRating()).isEqualTo(5);
        }
        edition.rate(0);
        edition.markRead(true);
        assertThat(edition.getRating()).isZero();
        edition.rate(null);
        assertThat(edition.getRating()).isNull();
    }

    @Test void serviceAuthorizesUpdatesAndRemovalBeforeChangingAnything() {
        var repository = mock(EditionRepository.class);
        var service = new EditionRatingService(repository);
        var owner = UUID.randomUUID();
        var edition = Edition.create(owner, null, null, "HQ", null, null, null, null, null, null);
        when(repository.findByIdAndOwnerUserId(edition.getId(), owner)).thenReturn(Optional.of(edition));
        service.rate(edition.getId(), owner, 4);
        assertThat(edition.getRating()).isEqualTo(4);
        var other = UUID.randomUUID();
        assertThatThrownBy(() -> service.rate(edition.getId(), other, null)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.rate(edition.getId(), other, 6)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.assertOwned(edition.getId(), other)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(edition.getRating()).isEqualTo(4);
        service.rate(edition.getId(), owner, null);
        assertThat(edition.getRating()).isNull();
        verify(repository, times(2)).flush();
    }
}
