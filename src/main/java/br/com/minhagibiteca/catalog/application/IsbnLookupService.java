package br.com.minhagibiteca.catalog.application;

import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IsbnLookupService {
    private final IsbnProvider provider;
    private final Clock clock;
    private final Map<String, Cached> cache = new LinkedHashMap<>();
    private Instant nextAllowed = Instant.MIN;
    @Autowired public IsbnLookupService(IsbnProvider provider) { this(provider, Clock.systemUTC()); }
    public IsbnLookupService(IsbnProvider provider, Clock clock) { this.provider = provider; this.clock = clock; }
    public Optional<IsbnMetadata> lookup(String input) {
        String isbn = normalize(input);
        Instant now = clock.instant();
        synchronized (cache) {
            cache.entrySet().removeIf(e -> !e.getValue().expires().isAfter(now));
            var cached = cache.get(isbn);
            if (cached != null) return cached.result();
            if (now.isBefore(nextAllowed)) throw new LookupException("Aguarde um instante antes de consultar outro ISBN. Você pode continuar o cadastro manualmente.");
            nextAllowed = now.plusSeconds(1);
        }
        var result = provider.lookup(isbn);
        synchronized (cache) {
            if (cache.size() >= 200) cache.remove(cache.keySet().iterator().next());
            cache.put(isbn, new Cached(result, clock.instant().plusSeconds(600)));
        }
        return result;
    }
    public static String normalize(String input) {
        if (input == null || input.length() > 32) throw invalid();
        String isbn = input.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        if (isbn.matches("[0-9]{9}[0-9X]")) {
            int sum = 0;
            for (int i = 0; i < 10; i++) sum += (10 - i) * (isbn.charAt(i) == 'X' ? 10 : isbn.charAt(i) - '0');
            if (sum % 11 == 0) return isbn;
        } else if (isbn.matches("97[89][0-9]{10}")) {
            int sum = 0;
            for (int i = 0; i < 13; i++) sum += (i % 2 == 0 ? 1 : 3) * (isbn.charAt(i) - '0');
            if (sum % 10 == 0) return isbn;
        }
        throw invalid();
    }
    private static LookupException invalid() { return new LookupException("Informe um ISBN-10 ou ISBN-13 válido para buscar. O cadastro manual continua disponível."); }
    public static String canonical(String input) {
        String isbn = normalize(input);
        if (isbn.length() == 13) return isbn;
        String prefix = "978" + isbn.substring(0, 9);
        int sum = 0;
        for (int i = 0; i < 12; i++) sum += (i % 2 == 0 ? 1 : 3) * (prefix.charAt(i) - '0');
        return prefix + ((10 - sum % 10) % 10);
    }
    private record Cached(Optional<IsbnMetadata> result, Instant expires) { }
    public static class LookupException extends RuntimeException {
        public LookupException(String message) { super(message); }
    }
}
