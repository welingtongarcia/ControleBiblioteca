package br.com.minhagibiteca.catalog.infrastructure;

import br.com.minhagibiteca.catalog.domain.Creator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreatorRepository extends JpaRepository<Creator, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Creator c where c.id=:id and c.ownerUserId=:owner")
    Optional<Creator> lockOwned(@org.springframework.data.repository.query.Param("id") UUID id,@org.springframework.data.repository.query.Param("owner") UUID owner);
    @org.springframework.data.jpa.repository.Query(value = """
        select exists(select 1 from edition_creators ec join editions e on e.id = ec.edition_id
            where ec.creator_id = :id and e.owner_user_id = :owner)
        """, nativeQuery = true)
    boolean hasReferences(@org.springframework.data.repository.query.Param("id") UUID id,
            @org.springframework.data.repository.query.Param("owner") UUID owner);
    Page<Creator> findByOwnerUserIdAndNormalizedNameContaining(UUID ownerUserId, String name, Pageable pageable);
    List<Creator> findByOwnerUserIdOrderByNormalizedNameAsc(UUID ownerUserId);
    Optional<Creator> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);
    boolean existsByOwnerUserIdAndNormalizedName(UUID ownerUserId, String normalizedName);
}
