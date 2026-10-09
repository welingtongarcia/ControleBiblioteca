package br.com.minhagibiteca.catalog.infrastructure;

import br.com.minhagibiteca.catalog.application.*;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class CompositeIsbnProvider implements IsbnProvider {
    private final IsbnProvider primary;
    private final IsbnProvider secondary;
    public CompositeIsbnProvider(OpenLibraryIsbnProvider primary, GoogleBooksIsbnProvider secondary) {
        this.primary = primary; this.secondary = secondary;
    }
    @Override public Optional<IsbnMetadata> lookup(String input) {
        String isbn = IsbnLookupService.normalize(input);
        var found = primary.lookup(isbn);
        return found.isPresent() ? found : secondary.lookup(isbn);
    }
}
