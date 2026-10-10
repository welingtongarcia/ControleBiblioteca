package br.com.minhagibiteca.catalog.application;

import br.com.minhagibiteca.catalog.domain.Creator;
import br.com.minhagibiteca.catalog.domain.CreatorFunction;
import java.util.Set;
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
        Creator creator = Creator.create(ownerId, command.name(), command.sortName());
        creator.replaceFunctions(command.functions());
        return creators.save(creator);
    }
    @Transactional
    public void update(UUID id, UUID ownerId, Command command) {
        Creator creator = findOwned(id, ownerId);
        if (!creator.getName().equalsIgnoreCase(command.name())
                && creators.existsByOwnerUserIdAndNormalizedName(ownerId, Creator.normalizeName(command.name()))) throw new DuplicateCreatorException();
        creator.update(command.name(), command.sortName());
        creator.replaceFunctions(command.functions());
    }
    @Transactional
    public void delete(UUID id, UUID ownerId) {
        var creator = findOwned(id, ownerId);
        if (creators.hasReferences(id, ownerId)) throw new ReferencedCatalogException("Este criador está vinculado a edições. Edite as edições para desvincular o criador antes de excluí-lo. Nenhuma HQ foi apagada.");
        creators.delete(creator); creators.flush();
    }
    private void ensureUnique(UUID ownerId, String name) {
        if (creators.existsByOwnerUserIdAndNormalizedName(ownerId, Creator.normalizeName(name))) throw new DuplicateCreatorException();
    }
    private String normalized(String text) { return text == null ? "" : text.trim().toLowerCase(); }
    public record Command(String name, String sortName, Set<CreatorFunction> functions) {
        public Command {
            functions = functions == null ? Set.of() : Set.copyOf(functions);
        }
        public Command(String name, String sortName) { this(name, sortName, Set.of()); }
    }
    public static class DuplicateCreatorException extends RuntimeException { }
}
