package br.com.minhagibiteca.catalog.application;
import br.com.minhagibiteca.catalog.infrastructure.CharacterRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
@Service
public class CharacterService {
 private final CharacterRepository repo;private final EditionService editions;
 public CharacterService(CharacterRepository repo,EditionService editions){this.repo=repo;this.editions=editions;}
 @Transactional(readOnly=true)public CharacterView find(UUID owner,UUID id){return repo.find(owner,id);}
 @Transactional(readOnly=true)public List<CharacterView> options(UUID owner){return repo.options(owner);}
 @Transactional(readOnly=true)public Page<CharacterView> list(UUID owner,String q,int page){return repo.list(owner,q==null?"":q.strip(),page);}
 @Transactional public UUID save(UUID owner,UUID id,String name){
  repo.lock(owner);if(id!=null)repo.find(owner,id);
  if(name==null||name.isBlank()||name.strip().length()>180)throw new IllegalArgumentException("Informe um nome com até 180 caracteres.");
  UUID target=id==null?UUID.randomUUID():id;String normalized=name.strip().toLowerCase(Locale.ROOT);
  if(repo.duplicate(owner,normalized,target))throw new CharacterRule("Já existe um personagem com este nome.");
  repo.save(owner,target,name.strip(),normalized,id!=null);return target;
 }
 @Transactional public void delete(UUID owner,UUID id){repo.lock(owner);repo.find(owner,id);if(repo.referenced(owner,id))throw new CharacterRule("Desvincule este personagem das edições antes de excluí-lo.");repo.delete(owner,id);}
 @Transactional(readOnly=true)public void validate(UUID owner,List<UUID> ids){for(UUID id:unique(ids))repo.find(owner,id);}
 @Transactional public void replace(UUID owner,UUID edition,List<UUID> ids){repo.lock(owner);editions.viewOwned(edition,owner);var selected=unique(ids);validate(owner,selected);repo.replace(owner,edition,selected);}
 @Transactional(readOnly=true)public List<UUID> ids(UUID owner,UUID edition){editions.viewOwned(edition,owner);return repo.ids(owner,edition);}
 @Transactional(readOnly=true)public List<CharacterView> edition(UUID owner,UUID edition){editions.viewOwned(edition,owner);return repo.edition(owner,edition);}
 private List<UUID> unique(List<UUID> ids){return ids==null?List.of():new ArrayList<>(new LinkedHashSet<>(ids));}
 public static class CharacterRule extends RuntimeException { public CharacterRule(String m){super(m);} }
}
