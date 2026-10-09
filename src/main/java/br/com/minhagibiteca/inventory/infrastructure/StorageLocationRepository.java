package br.com.minhagibiteca.inventory.infrastructure;

import br.com.minhagibiteca.inventory.domain.StorageLocation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageLocationRepository extends JpaRepository<StorageLocation, UUID> {
    Optional<StorageLocation> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);
    List<StorageLocation> findByParentIdAndOwnerUserId(UUID parentId, UUID ownerUserId);
    boolean existsByParentIdAndOwnerUserId(UUID parentId, UUID ownerUserId);
    List<StorageLocation> findByOwnerUserIdAndParentIdAndNormalizedName(UUID ownerUserId, UUID parentId, String normalizedName);
}
