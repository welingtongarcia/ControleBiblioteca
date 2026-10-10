package br.com.minhagibiteca.catalog.application;

import br.com.minhagibiteca.catalog.domain.CreatorFunction;
import br.com.minhagibiteca.catalog.infrastructure.AssociationOptionsRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AssociationOptionsService {
    public enum Kind { WORK, CREATOR, CHARACTER }
    public record Option(UUID id, String label, String description) { }
    public record Results(List<Option> items, int page, boolean hasNext) { }
    private final AssociationOptionsRepository repository;
    public AssociationOptionsService(AssociationOptionsRepository repository) { this.repository = repository; }
    public Results search(UUID owner, Kind kind, String q, CreatorFunction function, int page) {
        if (q == null || q.length() > 180 || page < 0 || page > 500 || (function != null && kind != Kind.CREATOR))
            throw new IllegalArgumentException("Parâmetros de pesquisa inválidos.");
        var rows = repository.search(owner, kind, q.strip(), function, page);
        return new Results(rows.stream().limit(20).toList(), page, rows.size() > 20);
    }
    public List<Option> forForm(UUID owner, Kind kind, List<UUID> selected) {
        var options = new LinkedHashMap<UUID, Option>();
        if (selected != null) repository.selected(owner, kind, selected.stream().filter(Objects::nonNull).distinct().toList())
                .forEach(o -> options.put(o.id(), o));
        search(owner, kind, "", null, 0).items().forEach(o -> options.putIfAbsent(o.id(), o));
        return List.copyOf(options.values());
    }
}
