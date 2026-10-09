package br.com.minhagibiteca.inventory;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.minhagibiteca.catalog.application.EditionService;
import br.com.minhagibiteca.catalog.application.EditionView;
import br.com.minhagibiteca.catalog.domain.MediaType;
import br.com.minhagibiteca.inventory.application.*;
import br.com.minhagibiteca.inventory.domain.*;
import br.com.minhagibiteca.inventory.infrastructure.*;
import br.com.minhagibiteca.shared.application.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class CopyServiceTest {
    @Mock PhysicalCopyRepository repository;
    @Mock CopyQueries queries;
    @Mock EditionService editions;
    @Mock LocationService locations;
    @Mock JdbcTemplate jdbc;
    private CopyService service;
    private final UUID owner = UUID.randomUUID();
    private final UUID edition = UUID.randomUUID();
    @BeforeEach void setup() { service = new CopyService(repository, queries, editions, locations, jdbc); }
    private EditionView authorize() {
        when(jdbc.queryForList("select id from users where id = ? for update", UUID.class, owner)).thenReturn(List.of(owner));
        var view = mock(EditionView.class); when(editions.viewOwned(edition, owner)).thenReturn(view); return view;
    }
    private CopyService.Command command(BigDecimal price, LocalDate date) { return new CopyService.Command(null, CopyCondition.UNKNOWN, null, null, date, price, null); }

    @ParameterizedTest @ValueSource(strings = {"-0.01", "1.234", "10000000000"})
    void rejectsInvalidDecimalBeforeWriting(String value) {
        authorize();
        assertThatThrownBy(() -> service.create(edition, owner, command(new BigDecimal(value), null)))
                .isInstanceOf(CopyService.RuleViolation.class).hasMessageContaining("duas casas");
        verifyNoInteractions(repository);
    }
    @Test void rejectsFutureDateUsingBrazilianCalendar() {
        authorize();
        assertThatThrownBy(() -> service.create(edition, owner, command(null, LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(1))))
                .isInstanceOf(CopyService.RuleViolation.class).hasMessageContaining("futura");
        verifyNoInteractions(repository);
    }
    @Test void rejectsDigitalEditionBeforeWriting() {
        when(authorize().mediaType()).thenReturn(MediaType.DIGITAL);
        assertThatThrownBy(() -> service.create(edition, owner, command(null, null))).hasMessageContaining("digital");
        verifyNoInteractions(repository, locations);
    }
    @Test void acceptsZeroAndKeepsUnknownPriceNull() {
        authorize(); when(repository.saveAndFlush(any(PhysicalCopy.class))).thenAnswer(call -> call.getArgument(0));
        assertThat(service.create(edition, owner, command(BigDecimal.ZERO, null))).isNotNull();
        assertThat(service.create(edition, owner, command(null, null))).isNotNull();
        verify(repository, times(2)).saveAndFlush(any(PhysicalCopy.class));
    }
    @Test void rejectsForeignLocationBeforeWriting() {
        authorize(); var location = UUID.randomUUID(); when(locations.find(location, owner)).thenThrow(new ResourceNotFoundException());
        assertThatThrownBy(() -> service.create(edition, owner, new CopyService.Command(location, CopyCondition.GOOD, null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(repository);
    }
    @Test void refusesToMutateCopyWithoutOwnership() {
        when(jdbc.queryForList("select id from users where id = ? for update", UUID.class, owner)).thenReturn(List.of(owner));
        var copy = UUID.randomUUID(); when(repository.findByIdAndOwnerUserId(copy, owner)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(copy, owner, command(null, null))).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(copy, owner)).isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).flush(); verify(repository, never()).delete(any(PhysicalCopy.class));
        verifyNoInteractions(editions, locations);
    }
}
