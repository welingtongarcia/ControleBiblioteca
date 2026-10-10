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
    @Test void controlsPartialPagesCompletionAndReset() {
        var e = Edition.create(UUID.randomUUID(), null, null, "HQ", null, null, null, null, null, null);
        e.updatePages(120, 30); assertThat(e.isRead()).isFalse();
        e.updatePages(120, 120); assertThat(e.isRead()).isTrue();
        e.markRead(false); assertThat(e.getPagesRead()).isZero();
        e.markRead(true); assertThat(e.getPagesRead()).isEqualTo(120);
        e.updatePages(150, 120); assertThat(e.isRead()).isFalse();
        assertThatThrownBy(() -> e.updatePages(10, 11)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> e.updatePages(null, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> e.updatePages(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> e.updatePages(100001, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> e.updatePages(10, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(e.getTotalPages()).isEqualTo(150);
    }
    @Test void unknownPagesPreserveLegacyReadingAndPagePercentageIsSeparate() {
        var e = Edition.create(UUID.randomUUID(), null, null, "HQ", null, null, null, null, null, null);
        e.markRead(true); e.updatePages(null, 0);
        assertThat(e.isRead()).isTrue(); assertThat(e.getTotalPages()).isNull();
        var progress = new ReadingProgress(3, 1, 300, 75, 2);
        assertThat(progress.percentageLabel()).isEqualTo("33,3%");
        assertThat(progress.pagePercentageLabel()).isEqualTo("25,0%");
        assertThat(progress.pagesUnread()).isEqualTo(225);
        assertThat(new ReadingProgress(0, 0).pagesUnread()).isZero();
        assertThat(new ReadingProgress(1, 0, 120, 0, 1).pagesUnread()).isEqualTo(120);
        assertThat(new ReadingProgress(1, 1, 120, 120, 1).pagesUnread()).isZero();
        assertThat(new ReadingProgress(3, 1).pagePercentageLabel()).isEqualTo("0,0%");
    }
    @Test void pageUpdateAuthorizesFlushesAndRejectsUnknownTotal() {
        var repo = mock(EditionRepository.class); var jdbc = mock(JdbcTemplate.class); var owner = UUID.randomUUID();
        var e = Edition.create(owner, null, null, "HQ", null, null, null, null, null, null);
        when(jdbc.queryForList(anyString(), eq(UUID.class), eq(owner))).thenReturn(List.of(owner));
        when(repo.findByIdAndOwnerUserId(e.getId(), owner)).thenReturn(Optional.of(e));
        var service = new ReadingService(repo, mock(ReadingQueries.class), mock(ComicSeriesService.class), jdbc);
        assertThatThrownBy(() -> service.updatePages(e.getId(), owner, 0)).isInstanceOf(IllegalArgumentException.class);
        e.updatePages(100, 0); service.updatePages(e.getId(), owner, 35);
        assertThat(e.getPagesRead()).isEqualTo(35); verify(repo).flush();
        assertThatThrownBy(() -> service.updatePages(UUID.randomUUID(), owner, 1)).isInstanceOf(ResourceNotFoundException.class);
    }
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
