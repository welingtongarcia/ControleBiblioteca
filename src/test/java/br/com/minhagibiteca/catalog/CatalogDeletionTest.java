package br.com.minhagibiteca.catalog;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.minhagibiteca.auth.application.CurrentAuthenticatedUser;
import br.com.minhagibiteca.catalog.application.*;
import br.com.minhagibiteca.catalog.domain.Publisher;
import br.com.minhagibiteca.catalog.infrastructure.PublisherRepository;
import br.com.minhagibiteca.catalog.presentation.*;
import br.com.minhagibiteca.shared.application.ResourceNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CatalogDeletionTest {
    @Test
    void checksOwnerBeforeReferencesAndFlushesOnlyUnreferencedDeletion() {
        var repository = mock(PublisherRepository.class);
        var service = new PublisherService(repository);
        var owner = UUID.randomUUID();
        var publisher = Publisher.create(owner, "Editora", null, null);
        var id = publisher.getId();
        when(repository.findByIdAndOwnerUserId(id, owner)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(id, owner)).isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).hasReferences(any(), any());
        when(repository.findByIdAndOwnerUserId(id, owner)).thenReturn(Optional.of(publisher));
        when(repository.hasReferences(id, owner)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(id, owner)).isInstanceOf(ReferencedCatalogException.class);
        verify(repository, never()).delete(publisher);
        verify(repository, never()).flush();
        when(repository.hasReferences(id, owner)).thenReturn(false);
        service.delete(id, owner);
        var order = inOrder(repository);
        order.verify(repository).delete(publisher);
        order.verify(repository).flush();
    }

    @Test
    void convertsIntegrityConflictOutsideServiceWithoutExposingDatabaseDetails() throws Exception {
        var user = mock(CurrentAuthenticatedUser.class);
        var owner = UUID.randomUUID();
        var id = UUID.randomUUID();
        when(user.id()).thenReturn(owner);
        var publishers = mock(PublisherService.class);
        var works = mock(WorkService.class);
        var creators = mock(CreatorService.class);
        doThrow(new DataIntegrityViolationException("SECRET SQL editions_publisher_id_fkey")).when(publishers).delete(id, owner);
        doThrow(new DataIntegrityViolationException("SECRET SQL edition_works")).when(works).delete(id, owner);
        doThrow(new DataIntegrityViolationException("SECRET SQL edition_creators")).when(creators).delete(id, owner);
        var mvc = MockMvcBuilders.standaloneSetup(new PublisherController(publishers, user),
                new WorkController(works, user), new CreatorController(creators, user)).build();
        for (var path : new String[]{"/editoras", "/obras", "/criadores"}) {
            var result = mvc.perform(post(path + "/{id}/excluir", id))
                    .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(path)).andReturn();
            assertThat(result.getFlashMap().containsKey("successMessage")).isFalse();
            assertThat(result.getFlashMap().get("errorMessage").toString())
                    .contains("conflito", "Nenhuma HQ foi apagada").doesNotContain("SECRET", "SQL", "fkey", id.toString());
        }
    }
}
