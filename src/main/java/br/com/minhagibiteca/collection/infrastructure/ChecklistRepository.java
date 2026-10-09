package br.com.minhagibiteca.collection.infrastructure;
import br.com.minhagibiteca.collection.domain.ChecklistItem;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChecklistRepository extends JpaRepository<ChecklistItem, UUID> {
    Optional<ChecklistItem> findByIdAndOwnerUserId(UUID id, UUID owner);
    boolean existsByOwnerUserIdAndSeriesIdAndNormalizedNumber(UUID owner, UUID series, String number);
}
