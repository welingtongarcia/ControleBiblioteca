package br.com.minhagibiteca.catalog.infrastructure;

import br.com.minhagibiteca.catalog.domain.Creator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreatorRepository extends JpaRepository<Creator, UUID> {
    Page<Creator> findByOwnerUserIdAndNormalizedNameContaining(UUID ownerUserId, String name, Pageable pageable);
    List<Creator> findByOwnerUserIdOrderByNormalizedNameAsc(UUID ownerUserId);
    Optional<Creator> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);
    boolean existsByOwnerUserIdAndNormalizedName(UUID ownerUserId, String normalizedName);
}
