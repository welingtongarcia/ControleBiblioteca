package br.com.minhagibiteca.catalog.application;

import br.com.minhagibiteca.catalog.infrastructure.EditionRepository;
import br.com.minhagibiteca.shared.application.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EditionRatingService {
    private final EditionRepository editions;
    public EditionRatingService(EditionRepository editions) { this.editions = editions; }

    @Transactional(readOnly = true)
    public void assertOwned(UUID id, UUID owner) {
        editions.findByIdAndOwnerUserId(id, owner).orElseThrow(ResourceNotFoundException::new);
    }

    @Transactional
    public void rate(UUID id, UUID owner, Integer rating) {
        editions.findByIdAndOwnerUserId(id, owner).orElseThrow(ResourceNotFoundException::new).rate(rating);
        editions.flush();
    }
}
