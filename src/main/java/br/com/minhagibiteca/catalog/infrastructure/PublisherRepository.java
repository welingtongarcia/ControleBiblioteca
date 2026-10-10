package br.com.minhagibiteca.catalog.infrastructure;

import br.com.minhagibiteca.catalog.domain.Publisher;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublisherRepository extends JpaRepository<Publisher, UUID> {

    @org.springframework.data.jpa.repository.Query(value = """
        select exists(select 1 from editions where publisher_id = :id and owner_user_id = :owner)
            or exists(select 1 from series where publisher_id = :id and owner_user_id = :owner)
        """, nativeQuery = true)
    boolean hasReferences(@org.springframework.data.repository.query.Param("id") UUID id,
            @org.springframework.data.repository.query.Param("owner") UUID owner);

    Page<Publisher> findByOwnerUserIdAndNormalizedNameContaining(UUID ownerUserId, String normalizedName, Pageable pageable);

    Optional<Publisher> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    boolean existsByOwnerUserIdAndNormalizedName(UUID ownerUserId, String normalizedName);

    List<Publisher> findByOwnerUserIdOrderByNormalizedNameAsc(UUID ownerUserId);
}
