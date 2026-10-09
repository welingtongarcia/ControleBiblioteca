package br.com.minhagibiteca.catalog.infrastructure;

import br.com.minhagibiteca.catalog.domain.ComicSeries;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComicSeriesRepository extends JpaRepository<ComicSeries, UUID> {
    Page<ComicSeries> findByOwnerUserIdAndNormalizedNameContaining(UUID ownerUserId, String normalizedName, Pageable pageable);
    Optional<ComicSeries> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);
    boolean existsByOwnerUserIdAndNormalizedName(UUID ownerUserId, String normalizedName);
}
