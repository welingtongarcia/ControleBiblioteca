package br.com.minhagibiteca.catalog.application;

import br.com.minhagibiteca.catalog.domain.Publisher;
import br.com.minhagibiteca.catalog.infrastructure.PublisherRepository;
import br.com.minhagibiteca.shared.application.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublisherService {

    private final PublisherRepository publisherRepository;

    public PublisherService(PublisherRepository publisherRepository) {
        this.publisherRepository = publisherRepository;
    }

    @Transactional(readOnly = true)
    public Page<Publisher> list(UUID ownerUserId, String query, Pageable pageable) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        return publisherRepository.findByOwnerUserIdAndNormalizedNameContaining(ownerUserId, normalizedQuery, pageable);
    }

    @Transactional(readOnly = true)
    public Publisher findOwned(UUID id, UUID ownerUserId) {
        return publisherRepository.findByIdAndOwnerUserId(id, ownerUserId)
                .orElseThrow(ResourceNotFoundException::new);
    }

    @Transactional
    public Publisher create(UUID ownerUserId, PublisherCommand command) {
        ensureNameIsAvailable(ownerUserId, command.name());
        return publisherRepository.save(Publisher.create(ownerUserId, command.name(), command.countryCode(), command.notes()));
    }

    @Transactional
    public void update(UUID id, UUID ownerUserId, PublisherCommand command) {
        Publisher publisher = findOwned(id, ownerUserId);
        String normalizedName = Publisher.normalizeName(command.name());
        if (!publisher.getName().equalsIgnoreCase(command.name())
                && publisherRepository.existsByOwnerUserIdAndNormalizedName(ownerUserId, normalizedName)) {
            throw new DuplicatePublisherException();
        }
        publisher.update(command.name(), command.countryCode(), command.notes());
    }

    @Transactional
    public void delete(UUID id, UUID ownerUserId) {
        publisherRepository.delete(findOwned(id, ownerUserId));
    }

    private void ensureNameIsAvailable(UUID ownerUserId, String name) {
        if (publisherRepository.existsByOwnerUserIdAndNormalizedName(ownerUserId, Publisher.normalizeName(name))) {
            throw new DuplicatePublisherException();
        }
    }

    public record PublisherCommand(String name, String countryCode, String notes) {
    }

    public static class DuplicatePublisherException extends RuntimeException {
    }
}
