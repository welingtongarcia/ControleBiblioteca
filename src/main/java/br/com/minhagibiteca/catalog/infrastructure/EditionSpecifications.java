package br.com.minhagibiteca.catalog.infrastructure;

import br.com.minhagibiteca.catalog.application.CatalogFilter;
import br.com.minhagibiteca.catalog.application.CatalogOrder;
import br.com.minhagibiteca.catalog.domain.Edition;
import java.math.BigDecimal;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class EditionSpecifications {
    private EditionSpecifications() { }

    public static Specification<Edition> orderedCatalog(UUID owner, CatalogFilter filter, CatalogOrder order) {
        return (root, query, cb) -> {
            var predicate = ownedCatalog(owner, filter).toPredicate(root, query, cb);
            // A consulta de contagem não precisa de ordenação. O Pageable fica sem Sort
            // apenas nestas opções, para não substituir a ordem natural da Specification.
            if (order.isNumberOrder() && query != null && query.getResultType() != Long.class) {
                var number = root.<String>get("editorialNumber");
                var prefix = cb.function("regexp_substr", String.class, number, cb.literal("^[0-9]+"));
                var group = cb.<Integer>selectCase().when(cb.isNull(number), 2)
                        .when(cb.isNull(prefix), 1).otherwise(0);
                var numeric = cb.function("to_number", BigDecimal.class, prefix, cb.literal("9".repeat(60)));
                var suffix = cb.lower(cb.function("regexp_replace", String.class, number,
                        cb.literal("^[0-9]+"), cb.literal("")));
                var ordering = new ArrayList<jakarta.persistence.criteria.Order>();
                if (order.groupsByTitle()) ordering.add(cb.asc(root.get("normalizedTitle")));
                ordering.add(cb.asc(group));
                ordering.add(order.isDescending() ? cb.desc(numeric) : cb.asc(numeric));
                ordering.add(order.isDescending() ? cb.desc(suffix) : cb.asc(suffix));
                if (!order.groupsByTitle()) ordering.add(cb.asc(root.get("normalizedTitle")));
                ordering.add(cb.asc(root.get("id")));
                query.orderBy(ordering);
            }
            return predicate;
        };
    }

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
