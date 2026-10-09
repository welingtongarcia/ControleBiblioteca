package br.com.minhagibiteca.collection;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.minhagibiteca.collection.application.*;
import br.com.minhagibiteca.collection.infrastructure.ReadingQueries;
import br.com.minhagibiteca.catalog.application.ComicSeriesService;
import br.com.minhagibiteca.catalog.domain.Edition;
import br.com.minhagibiteca.catalog.infrastructure.EditionRepository;
import br.com.minhagibiteca.shared.application.ResourceNotFoundException;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class ReadingTest {
    @Test void calculatesEmptyPartialAndCompletePercentages() {
        assertThat(new ReadingProgress(0, 0).percentageLabel()).isEqualTo("0,0%");
        assertThat(new ReadingProgress(3, 1).percentageLabel()).isEqualTo("33,3%");
        assertThat(new ReadingProgress(3, 2).percentageLabel()).isEqualTo("66,7%");
        assertThat(new ReadingProgress(20, 20).percentageLabel()).isEqualTo("100,0%");
    }
    @Test void domainReadingIsReversibleAndMetadataPreservesIt() {
        var e = Edition.create(UUID.randomUUID(), null, null, "HQ", null, null, null, null, null, null);
        assertThat(e.isRead()).isFalse();
        e.markRead(true); e.markRead(true);
        e.update(null, null, "Outro título", null, null, null, null, null, null);
        assertThat(e.isRead()).isTrue();
        e.markRead(false); assertThat(e.isRead()).isFalse();
    }
    @Test void authorizesAndFlushesReadingState() {
        var repo = mock(EditionRepository.class); var jdbc = mock(JdbcTemplate.class);
        var owner = UUID.randomUUID();
        var e = Edition.create(owner, null, null, "HQ", null, null, null, null, null, null);
        when(jdbc.queryForList(anyString(), eq(UUID.class), eq(owner))).thenReturn(List.of(owner));
        when(repo.findByIdAndOwnerUserId(e.getId(), owner)).thenReturn(Optional.of(e));
        var service = new ReadingService(repo, mock(ReadingQueries.class), mock(ComicSeriesService.class), jdbc);
        service.mark(e.getId(), owner, true);
        assertThat(e.isRead()).isTrue(); verify(repo).flush();
    }
    @Test void rejectsForeignEditionBeforeWriting() {
        var repo = mock(EditionRepository.class); var jdbc = mock(JdbcTemplate.class); var owner = UUID.randomUUID();
        when(jdbc.queryForList(anyString(), eq(UUID.class), eq(owner))).thenReturn(List.of(owner));
        when(repo.findByIdAndOwnerUserId(any(), eq(owner))).thenReturn(Optional.empty());
        var service = new ReadingService(repo, mock(ReadingQueries.class), mock(ComicSeriesService.class), jdbc);
        assertThatThrownBy(() -> service.mark(UUID.randomUUID(), owner, true)).isInstanceOf(ResourceNotFoundException.class);
        verify(repo, never()).flush();
    }
}
