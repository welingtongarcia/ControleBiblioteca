package br.com.minhagibiteca.catalog.infrastructure;

import br.com.minhagibiteca.catalog.application.CatalogFilter;
import br.com.minhagibiteca.catalog.domain.Edition;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class EditionSpecifications {
    private EditionSpecifications() { }

    public static Specification<Edition> ownedCatalog(UUID owner, CatalogFilter filter) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("ownerUserId"), owner));
            if (!filter.query().isBlank()) {
                String search = "%" + escape(filter.query().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.or(cb.like(root.get("normalizedTitle"), search, '\\'),
                        cb.like(cb.lower(root.get("isbn")), search, '\\'),
                        cb.like(cb.lower(root.get("editorialNumber")), search, '\\')));
            }
            if (filter.publisherId() != null) predicates.add(cb.equal(root.get("publisherId"), filter.publisherId()));
            if (filter.seriesId() != null) predicates.add(cb.equal(root.get("seriesId"), filter.seriesId()));
            if (filter.mediaType() != null) predicates.add(cb.equal(root.get("mediaType"), filter.mediaType()));
            if (!filter.format().isBlank()) predicates.add(cb.equal(cb.lower(root.get("format")), filter.format().toLowerCase(Locale.ROOT)));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
