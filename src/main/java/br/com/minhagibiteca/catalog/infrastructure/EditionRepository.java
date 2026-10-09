package br.com.minhagibiteca.catalog.infrastructure;
import br.com.minhagibiteca.catalog.domain.Edition; import java.util.*; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface EditionRepository extends JpaRepository<Edition,UUID>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Edition> {
    Page<Edition> findByOwnerUserIdAndNormalizedTitleContaining(UUID owner,String q,Pageable p);
    Optional<Edition> findByIdAndOwnerUserId(UUID id,UUID owner);
    @org.springframework.data.jpa.repository.Query("select distinct e.format from Edition e where e.ownerUserId = :owner and e.format is not null order by e.format")
    List<String> formats(@org.springframework.data.repository.query.Param("owner") UUID owner);
}
