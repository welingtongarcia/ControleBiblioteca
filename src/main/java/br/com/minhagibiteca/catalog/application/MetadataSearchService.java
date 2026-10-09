package br.com.minhagibiteca.catalog.application;

import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MetadataSearchService {
    public record Result(UUID token, List<MetadataCandidate> candidates) { }
    private record Entry(UUID owner, MetadataCandidate.Source source, String query, Result result, Instant expires) { }
    private final MetadataSearchProvider provider;
    private final Clock clock;
    private final Map<UUID, Entry> cache = new LinkedHashMap<>();
    private Instant nextAllowed = Instant.MIN;

    @Autowired public MetadataSearchService(MetadataSearchProvider provider) { this(provider, Clock.systemUTC()); }
    public MetadataSearchService(MetadataSearchProvider provider, Clock clock) { this.provider = provider; this.clock = clock; }

    public Result search(UUID owner, MetadataCandidate.Source source, String title) {
        String query = normalize(title);
        if (source == null || owner == null) throw new SearchException("Escolha uma fonte para pesquisar.");
        synchronized (cache) {
            purge();
            for (Entry entry : cache.values()) if (entry.owner().equals(owner) && entry.source() == source && entry.query().equals(query)) return entry.result();
            if (clock.instant().isBefore(nextAllowed)) throw new SearchException("Aguarde um instante antes de pesquisar novamente. O cadastro manual continua disponível.");
            nextAllowed = clock.instant().plusSeconds(1);
        }
        List<MetadataCandidate> candidates = List.copyOf(provider.search(source, query).stream().limit(5).toList());
        Result result = new Result(UUID.randomUUID(), candidates);
        synchronized (cache) {
            purge();
            if (cache.size() >= 200) cache.remove(cache.keySet().iterator().next());
            cache.put(result.token(), new Entry(owner, source, query, result, clock.instant().plusSeconds(600)));
        }
        return result;
    }
    public MetadataCandidate select(UUID owner, String token, int index) {
        synchronized (cache) {
            purge();
            UUID id;
            try { id = UUID.fromString(token); } catch (RuntimeException error) { throw expired(); }
            Entry entry = cache.get(id);
            if (entry == null || !entry.owner().equals(owner) || index < 0 || index >= entry.result().candidates().size()) throw expired();
            return entry.result().candidates().get(index);
        }
    }
    private void purge() { cache.entrySet().removeIf(e -> !e.getValue().expires().isAfter(clock.instant())); }
    private SearchException expired() { return new SearchException("Seleção inválida ou expirada. Pesquise novamente; seus dados foram preservados."); }
    public static String normalize(String title) {
        if (title == null || title.length() > 240 || title.matches("(?s).*\\p{Cntrl}.*")) throw new SearchException("Informe um título de 3 a 240 caracteres para pesquisar.");
        String query = title.strip().replaceAll("\\s+", " ");
        if (query.length() < 3) throw new SearchException("Informe um título de 3 a 240 caracteres para pesquisar.");
        return query;
    }
    public static class SearchException extends RuntimeException {
        public SearchException(String message) { super(message); }
    }
}
