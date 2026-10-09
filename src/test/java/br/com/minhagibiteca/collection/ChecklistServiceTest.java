package br.com.minhagibiteca.collection;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import br.com.minhagibiteca.catalog.application.ComicSeriesService;
import br.com.minhagibiteca.collection.application.ChecklistService;
import br.com.minhagibiteca.collection.domain.ChecklistItem;
import br.com.minhagibiteca.collection.infrastructure.*;
import br.com.minhagibiteca.shared.application.ResourceNotFoundException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class ChecklistServiceTest {
    @Mock ChecklistRepository repository;
    @Mock ChecklistQueries queries;
    @Mock ComicSeriesService series;
    @Mock JdbcTemplate jdbc;
    private ChecklistService service;
    private final UUID owner = UUID.randomUUID();
    private final UUID seriesId = UUID.randomUUID();
    @BeforeEach void setup() { service = new ChecklistService(repository, queries, series, jdbc); }
    private void lock() { when(jdbc.queryForList("select id from users where id = ? for update", UUID.class, owner)).thenReturn(List.of(owner)); }
    private ChecklistService.Command command(String number) { return new ChecklistService.Command(number, null, false, null); }
    @ParameterizedTest @ValueSource(strings = {"0", "00", "1A", "1/2", "Especial"})
    void preservesIrregularEditorialNumbers(String number) {
        var item = ChecklistItem.create(owner, seriesId, " " + number + " ", null, false, null);
        assertThat(item.getEditorialNumber()).isEqualTo(number);
        assertThat(item.getNormalizedNumber()).isEqualTo(number.toLowerCase(Locale.ROOT));
        assertThat(item.isOwned()).isFalse();
        item.update(number, null, true, null); assertThat(item.isOwned()).isTrue();
        assertThat(item.getSeriesId()).isEqualTo(seriesId);
    }
    @Test void rejectsInvalidDomainTexts() {
        for (String number : List.of("", " ", "x".repeat(41))) assertThatThrownBy(() -> ChecklistItem.create(owner, seriesId, number, null, false, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChecklistItem.create(owner, seriesId, "1", "x".repeat(181), false, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChecklistItem.create(owner, seriesId, "1", null, false, "x".repeat(2001))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejectsNormalizedDuplicateBeforeWriting() {
        lock(); when(repository.existsByOwnerUserIdAndSeriesIdAndNormalizedNumber(owner, seriesId, "1a")).thenReturn(true);
        assertThatThrownBy(() -> service.create(seriesId, owner, command(" 1A "))).isInstanceOf(ChecklistService.RuleViolation.class).hasMessageContaining("Já existe");
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void authorizesSeriesBeforeCreatingAndDoesNotInferItems() {
        lock(); when(series.viewOwned(seriesId, owner)).thenThrow(new ResourceNotFoundException());
        assertThatThrownBy(() -> service.create(seriesId, owner, command("1"))).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(repository, queries);
    }
    @Test void allowsChangingManualStateOfSameNumber() {
        lock(); var item = ChecklistItem.create(owner, seriesId, "1A", null, false, null);
        when(repository.findByIdAndOwnerUserId(item.getId(), owner)).thenReturn(Optional.of(item));
        assertThat(service.update(item.getId(), owner, new ChecklistService.Command(" 1a ", null, true, null))).isEqualTo(seriesId);
        assertThat(item.isOwned()).isTrue();
        verify(repository, never()).existsByOwnerUserIdAndSeriesIdAndNormalizedNumber(any(), any(), any());
        verify(repository).flush();
    }
    @Test void refusesForeignItemMutations() {
        lock(); var id = UUID.randomUUID(); when(repository.findByIdAndOwnerUserId(id, owner)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(id, owner, command("1"))).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(id, owner)).isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).delete(any()); verify(repository, never()).flush();
    }
}
