package br.com.minhagibiteca.inventory.infrastructure;

import br.com.minhagibiteca.inventory.domain.PhysicalCopy;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhysicalCopyRepository extends JpaRepository<PhysicalCopy, UUID> {
    Optional<PhysicalCopy> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);
    long countByEditionIdAndOwnerUserId(UUID editionId, UUID ownerUserId);
}
