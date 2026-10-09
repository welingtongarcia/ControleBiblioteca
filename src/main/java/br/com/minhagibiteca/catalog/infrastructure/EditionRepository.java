package br.com.minhagibiteca.catalog.infrastructure;
import br.com.minhagibiteca.catalog.domain.Edition; import java.util.*; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface EditionRepository extends JpaRepository<Edition,UUID>{ Page<Edition> findByOwnerUserIdAndNormalizedTitleContaining(UUID owner,String q,Pageable p); Optional<Edition> findByIdAndOwnerUserId(UUID id,UUID owner); }
