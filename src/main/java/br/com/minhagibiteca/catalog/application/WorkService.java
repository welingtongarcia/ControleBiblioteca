package br.com.minhagibiteca.catalog.application;

import br.com.minhagibiteca.catalog.domain.Work;
import br.com.minhagibiteca.catalog.infrastructure.WorkRepository;
import br.com.minhagibiteca.shared.application.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkService {
    private final WorkRepository works;
    public WorkService(WorkRepository works) { this.works = works; }

    @Transactional(readOnly = true)
    public Page<Work> list(UUID ownerId, String query, Pageable pageable) {
        return works.findByOwnerUserIdAndNormalizedTitleContaining(ownerId, normalized(query), pageable);
    }
    @Transactional(readOnly = true)
    public List<Work> options(UUID ownerId) { return works.findByOwnerUserIdOrderByNormalizedTitleAsc(ownerId); }
    @Transactional(readOnly = true)
    public Work findOwned(UUID id, UUID ownerId) { return works.findByIdAndOwnerUserId(id, ownerId).orElseThrow(ResourceNotFoundException::new); }
    @Transactional
    public Work create(UUID ownerId, Command command) {
        ensureUnique(ownerId, command.title());
        return works.save(Work.create(ownerId, command.title(), command.originalYear(), command.summary()));
    }
    @Transactional
    public void update(UUID id, UUID ownerId, Command command) {
        Work work = findOwned(id, ownerId);
        if (!work.getTitle().equalsIgnoreCase(command.title())
                && works.existsByOwnerUserIdAndNormalizedTitle(ownerId, Work.normalizeTitle(command.title()))) throw new DuplicateWorkException();
        work.update(command.title(), command.originalYear(), command.summary());
    }
    @Transactional
    public void delete(UUID id, UUID ownerId) {
        var work = findOwned(id, ownerId);
        if (works.hasReferences(id, ownerId)) throw new ReferencedCatalogException("Esta obra está vinculada a edições. Edite as edições para desvincular a obra antes de excluí-la. Nenhuma HQ foi apagada.");
        works.delete(work); works.flush();
    }
    private void ensureUnique(UUID ownerId, String title) {
        if (works.existsByOwnerUserIdAndNormalizedTitle(ownerId, Work.normalizeTitle(title))) throw new DuplicateWorkException();
    }
    private String normalized(String text) { return text == null ? "" : text.trim().toLowerCase(); }
    public record Command(String title, Integer originalYear, String summary) { }
    public static class DuplicateWorkException extends RuntimeException { }
}
