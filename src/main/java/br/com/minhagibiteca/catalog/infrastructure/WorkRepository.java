package br.com.minhagibiteca.catalog.infrastructure;

import br.com.minhagibiteca.catalog.domain.Work;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkRepository extends JpaRepository<Work, UUID> {
    @org.springframework.data.jpa.repository.Query(value = """
        select exists(select 1 from edition_works ew join editions e on e.id = ew.edition_id
            where ew.work_id = :id and e.owner_user_id = :owner)
        """, nativeQuery = true)
    boolean hasReferences(@org.springframework.data.repository.query.Param("id") UUID id,
            @org.springframework.data.repository.query.Param("owner") UUID owner);
    Page<Work> findByOwnerUserIdAndNormalizedTitleContaining(UUID ownerUserId, String title, Pageable pageable);
    List<Work> findByOwnerUserIdOrderByNormalizedTitleAsc(UUID ownerUserId);
    Optional<Work> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);
    boolean existsByOwnerUserIdAndNormalizedTitle(UUID ownerUserId, String normalizedTitle);
}
