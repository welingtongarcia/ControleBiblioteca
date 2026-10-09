package br.com.minhagibiteca.catalog.application;
import br.com.minhagibiteca.catalog.infrastructure.EditionRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class EditionRegistrationService {
 private final EditionService editions;private final EditionAssociationService associations;private final CharacterService characters;private final EditionRepository repo;
 public EditionRegistrationService(EditionService editions,EditionAssociationService associations,CharacterService characters,EditionRepository repo){this.editions=editions;this.associations=associations;this.characters=characters;this.repo=repo;}
 @Transactional public UUID save(UUID owner,UUID id,EditionService.Command command,List<UUID> works,List<UUID> creators,List<UUID> characterIds){
  characters.validate(owner,characterIds);UUID target=id;
  if(id==null){target=editions.createId(owner,command);repo.flush();}else editions.update(id,owner,command);
  associations.replace(target,owner,works,creators);characters.replace(owner,target,characterIds);return target;
 }
}
