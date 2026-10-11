package br.com.minhagibiteca.catalog.application;

import br.com.minhagibiteca.catalog.infrastructure.CreatorRepository;
import br.com.minhagibiteca.shared.application.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CreatorRegistrationService {
    private final CreatorService creators;private final CreatorPhotoService photos;private final CreatorRepository repository;
    public CreatorRegistrationService(CreatorService creators,CreatorPhotoService photos,CreatorRepository repository){this.creators=creators;this.photos=photos;this.repository=repository;}
    @Transactional public UUID create(UUID owner,CreatorService.Command command,MultipartFile photo){var creator=creators.create(owner,command);repository.flush();if(photo!=null&&!photo.isEmpty())photos.replace(creator.getId(),owner,photo);return creator.getId();}
    @Transactional public void update(UUID id,UUID owner,CreatorService.Command command,MultipartFile photo){repository.lockOwned(id,owner).orElseThrow(ResourceNotFoundException::new);creators.update(id,owner,command);if(photo!=null&&!photo.isEmpty())photos.replace(id,owner,photo);}
    @Transactional public void delete(UUID id,UUID owner){var creator=repository.lockOwned(id,owner).orElseThrow(ResourceNotFoundException::new);String name=creator.getPhotoFileName();creators.delete(id,owner);photos.deleteAfterCommit(name);}
}
