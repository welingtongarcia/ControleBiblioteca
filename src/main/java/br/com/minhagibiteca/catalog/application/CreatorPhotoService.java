package br.com.minhagibiteca.catalog.application;

import br.com.minhagibiteca.catalog.infrastructure.CreatorRepository;
import br.com.minhagibiteca.shared.application.ResourceNotFoundException;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CreatorPhotoService {
    private static final long MAX_BYTES=5L*1024*1024;
    private final CreatorRepository creators;private final Path storage;
    public CreatorPhotoService(CreatorRepository creators,@Value("${app.cover-storage-path}") String path){this.creators=creators;storage=Path.of(path).toAbsolutePath().normalize();}
    @Transactional public void replace(UUID id,UUID owner,MultipartFile file){
        var creator=creators.lockOwned(id,owner).orElseThrow(ResourceNotFoundException::new);
        if(file==null||file.isEmpty())throw invalid("Selecione uma foto.");
        if(file.getSize()>MAX_BYTES)throw invalid("A foto deve ter no máximo 5 MB.");
        String mime=file.getContentType();if(!Set.of("image/png","image/jpeg").contains(mime==null?"":mime))throw invalid("Envie uma foto PNG ou JPEG válida.");
        String extension=mime.equals("image/png")?"png":"jpg";
        Path temporary=null,target=null;
        try {
            Files.createDirectories(storage);temporary=Files.createTempFile(storage,"creator-photo-",".upload");
            try(var input=file.getInputStream();var output=Files.newOutputStream(temporary)){byte[] buffer=new byte[8192];long total=0;int n;while((n=input.read(buffer))!=-1){total+=n;if(total>MAX_BYTES)throw invalid("A foto deve ter no máximo 5 MB.");output.write(buffer,0,n);}}
            try(var stream=ImageIO.createImageInputStream(temporary.toFile())){
                var readers=ImageIO.getImageReaders(stream);if(!readers.hasNext())throw invalid("O arquivo enviado não é uma imagem válida.");
                var reader=readers.next();try{reader.setInput(stream,true,true);if(!reader.getFormatName().equalsIgnoreCase(extension.equals("jpg")?"jpeg":"png"))throw invalid("O conteúdo da foto não corresponde ao formato informado.");
                    int width=reader.getWidth(0),height=reader.getHeight(0);if(width<1||height<1||(long)width*height>40_000_000)throw invalid("A foto deve ter até 40 milhões de pixels.");
                    var image=reader.read(0);if(image==null)throw invalid("Foto ilegível.");image.flush();
                }finally{reader.dispose();}
            }
            String name=id+"-"+UUID.randomUUID()+"."+extension;target=storage.resolve(name);
            Files.move(temporary,target);String previous=creator.getPhotoFileName();creator.updatePhoto(name,mime);
            Path saved=target;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
                @Override public void afterCompletion(int status){if(status==STATUS_COMMITTED)deleteQuietly(previous);else deleteQuietly(saved);}
            });
            target=null;
        }catch(IOException error){throw invalid("Não foi possível salvar a foto. Tente novamente.");}
        finally{if(temporary!=null)deleteQuietly(temporary);if(target!=null)deleteQuietly(target);}
    }
    @Transactional public void remove(UUID id,UUID owner){var creator=creators.lockOwned(id,owner).orElseThrow(ResourceNotFoundException::new);String previous=creator.getPhotoFileName();creator.updatePhoto(null,null);deleteAfterCommit(previous);}
    public void deleteAfterCommit(String name){if(name!=null)TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){deleteQuietly(name);}});}
    @Transactional(readOnly=true) public Photo load(UUID id,UUID owner){var c=creators.findByIdAndOwnerUserId(id,owner).orElseThrow(ResourceNotFoundException::new);if(c.getPhotoFileName()==null)throw new ResourceNotFoundException();Path path=storage.resolve(c.getPhotoFileName()).normalize();if(!path.getParent().equals(storage)||!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))throw new ResourceNotFoundException();return new Photo(new FileSystemResource(path),c.getPhotoContentType());}
    private void deleteQuietly(String name){if(name!=null){Path path=storage.resolve(name).normalize();if(path.getParent().equals(storage))deleteQuietly(path);}}
    private void deleteQuietly(Path path){try{Files.deleteIfExists(path);}catch(IOException ignored){}}
    private InvalidPhotoException invalid(String message){return new InvalidPhotoException(message);}
    public record Photo(Resource resource,String contentType){}
    public static class InvalidPhotoException extends RuntimeException {public InvalidPhotoException(String message){super(message);}}
}
