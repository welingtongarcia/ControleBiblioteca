package br.com.minhagibiteca.catalog.infrastructure;

import br.com.minhagibiteca.catalog.domain.Work;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkRepository extends JpaRepository<Work, UUID> {
    Page<Work> findByOwnerUserIdAndNormalizedTitleContaining(UUID ownerUserId, String title, Pageable pageable);
    List<Work> findByOwnerUserIdOrderByNormalizedTitleAsc(UUID ownerUserId);
    Optional<Work> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);
    boolean existsByOwnerUserIdAndNormalizedTitle(UUID ownerUserId, String normalizedTitle);
}
