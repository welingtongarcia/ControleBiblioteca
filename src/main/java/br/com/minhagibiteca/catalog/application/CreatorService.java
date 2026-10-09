package br.com.minhagibiteca.catalog.application;

import br.com.minhagibiteca.catalog.domain.Creator;
import br.com.minhagibiteca.catalog.infrastructure.CreatorRepository;
import br.com.minhagibiteca.shared.application.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreatorService {
    private final CreatorRepository creators;
    public CreatorService(CreatorRepository creators) { this.creators = creators; }

    @Transactional(readOnly = true)
    public Page<Creator> list(UUID ownerId, String query, Pageable pageable) {
        return creators.findByOwnerUserIdAndNormalizedNameContaining(ownerId, normalized(query), pageable);
    }
    @Transactional(readOnly = true)
    public List<Creator> options(UUID ownerId) { return creators.findByOwnerUserIdOrderByNormalizedNameAsc(ownerId); }
    @Transactional(readOnly = true)
    public Creator findOwned(UUID id, UUID ownerId) { return creators.findByIdAndOwnerUserId(id, ownerId).orElseThrow(ResourceNotFoundException::new); }
    @Transactional
    public Creator create(UUID ownerId, Command command) {
        ensureUnique(ownerId, command.name());
        return creators.save(Creator.create(ownerId, command.name(), command.sortName()));
    }
    @Transactional
    public void update(UUID id, UUID ownerId, Command command) {
        Creator creator = findOwned(id, ownerId);
        if (!creator.getName().equalsIgnoreCase(command.name())
                && creators.existsByOwnerUserIdAndNormalizedName(ownerId, Creator.normalizeName(command.name()))) throw new DuplicateCreatorException();
        creator.update(command.name(), command.sortName());
    }
    @Transactional
    public void delete(UUID id, UUID ownerId) { creators.delete(findOwned(id, ownerId)); }
    private void ensureUnique(UUID ownerId, String name) {
        if (creators.existsByOwnerUserIdAndNormalizedName(ownerId, Creator.normalizeName(name))) throw new DuplicateCreatorException();
    }
    private String normalized(String text) { return text == null ? "" : text.trim().toLowerCase(); }
    public record Command(String name, String sortName) { }
    public static class DuplicateCreatorException extends RuntimeException { }
}
