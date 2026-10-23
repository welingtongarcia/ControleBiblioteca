package br.com.minhagibiteca.digital.application;

import br.com.minhagibiteca.digital.domain.*;
import br.com.minhagibiteca.digital.infrastructure.*;
import java.time.*;
import java.util.*;
import org.springframework.security.oauth2.core.endpoint.*;
import org.springframework.stereotype.Service;

@Service
public class OneDriveService {
    public static final List<String> EXTENSIONS=List.of("pdf","cbr","cbz","epub","mobi","azw","azw3");
    private final OneDriveSettings settings;private final OneDriveRepository repo;private final OneDriveTokenCipher cipher;private final OneDriveOAuthClient oauth;private final MicrosoftGraphClient graph;
    private final RcloneClient rclone;
    public OneDriveService(OneDriveSettings settings,OneDriveRepository repo,OneDriveTokenCipher cipher,OneDriveOAuthClient oauth,MicrosoftGraphClient graph,RcloneClient rclone){this.settings=settings;this.repo=repo;this.cipher=cipher;this.oauth=oauth;this.graph=graph;this.rclone=rclone;}
    public record Pending(UUID owner,Instant expires,OAuth2AuthorizationRequest request) implements java.io.Serializable {@Override public String toString(){return "Pending OneDrive authorization [protected]";}}
    public record Status(boolean configured,boolean connected,String driveName,String lastSync,String error,boolean rclone){}
    public boolean rcloneEnabled(){return rclone.enabled();}
    public boolean oauthEnabled(){return settings.isEnabled();}
    private boolean isRclone(OneDriveRepository.Connection c){return RcloneClient.PROVIDER.equals(c.scopes());}
    public Status status(UUID owner){var c=repo.connection(owner);boolean rc=c.map(this::isRclone).orElse(false);return new Status(c.isEmpty()?(settings.isEnabled()||rclone.enabled()):(rc?rclone.enabled():settings.isEnabled()),c.isPresent(),c.map(OneDriveRepository.Connection::driveName).orElse(""),c.map(OneDriveRepository.Connection::lastSync).map(i->i.atZone(ZoneId.of("America/Sao_Paulo")).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).orElse("Ainda não sincronizado"),c.map(OneDriveRepository.Connection::error).orElse(null),rc);}
    public void connectRclone(UUID owner){rclone.verify(owner);repo.connect(owner,"rclone","OneDrive via rclone","external","external",Instant.parse("2100-01-01T00:00:00Z"),RcloneClient.PROVIDER);}
    public Pending begin(UUID owner){return new Pending(owner,Instant.now().plusSeconds(600),oauth.authorization());}
    public boolean valid(Pending pending,UUID owner,String state){return pending!=null&&pending.owner().equals(owner)&&pending.expires().isAfter(Instant.now())&&state!=null&&state.length()<=256&&java.security.MessageDigest.isEqual(pending.request().getState().getBytes(java.nio.charset.StandardCharsets.UTF_8),state.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    public void finish(UUID owner,Pending pending,String code){
        if(pending==null||!pending.owner().equals(owner)||!pending.expires().isAfter(Instant.now())||code==null||code.isBlank()||code.length()>16384)throw new OneDriveException("Autorização inválida ou expirada. Conecte novamente.");
        var tokens=oauth.exchange(pending.request(),code);if(tokens.getRefreshToken()==null)throw new OneDriveException("A Microsoft não forneceu autorização de renovação. Conecte novamente e autorize offline_access.");
        var drive=graph.drive(tokens.getAccessToken().getTokenValue());
        repo.connect(owner,drive.id(),drive.name(),cipher.encrypt(owner,tokens.getAccessToken().getTokenValue()),cipher.encrypt(owner,tokens.getRefreshToken().getTokenValue()),tokens.getAccessToken().getExpiresAt(),String.join(" ",tokens.getAccessToken().getScopes()));
    }
    private OneDriveRepository.Connection connected(UUID owner){var c=repo.connection(owner).orElseThrow(()->new OneDriveException("Conecte sua conta OneDrive para continuar."));if(isRclone(c)){if(!rclone.enabled())throw new OneDriveException("O modo rclone está desativado nesta instalação.");}else settings.registration();return c;}
    private String token(UUID owner,OneDriveRepository.Connection connection){
        String access=cipher.decrypt(owner,connection.accessToken());if(connection.expiresAt().isAfter(Instant.now().plusSeconds(60)))return access;
        String refresh=cipher.decrypt(owner,connection.refreshToken());var response=oauth.refresh(access,refresh,connection.expiresAt(),connection.scopes().isBlank()?Set.of("Files.Read"):Set.copyOf(Arrays.asList(connection.scopes().split(" +"))));
        var next=response.getAccessToken();String rotated=response.getRefreshToken()==null?refresh:response.getRefreshToken().getTokenValue();
        repo.refreshed(owner,connection.generation(),cipher.encrypt(owner,next.getTokenValue()),cipher.encrypt(owner,rotated),next.getExpiresAt(),String.join(" ",next.getScopes()));return next.getTokenValue();
    }
    public MicrosoftGraphClient.Items browse(UUID owner,String folder,String next){var c=connected(owner);return isRclone(c)?rclone.browse(owner,folder,next):graph.children(token(owner,c),c.driveId(),folder,next);}
    public String folderName(UUID owner,String folder){if(folder.equals("root"))return "Raiz do OneDrive";var c=connected(owner);return (isRclone(c)?rclone.folder(owner,folder):graph.folder(token(owner,c),c.driveId(),folder)).name();}
    public void select(UUID owner,String remoteId){var c=connected(owner);var folder=isRclone(c)?rclone.folder(owner,remoteId):graph.folder(token(owner,c),c.driveId(),remoteId);repo.addFolder(owner,c.generation(),remoteId,folder.name());}
    public int synchronize(UUID owner){
        var c=connected(owner);String access=isRclone(c)?null:token(owner,c);UUID run=repo.begin(owner,c.generation());
        try{
            var roots=repo.folders(owner);
            record Work(String id,String path){}var pending=new ArrayDeque<Work>();roots.forEach(f->pending.add(new Work(f.remoteId(),f.name())));
            var visited=new HashSet<String>();var files=new LinkedHashMap<String,DigitalFile>();Instant deadline=Instant.now().plusSeconds(90);int count=0,requests=0;
            while(!pending.isEmpty()){
                var folder=pending.removeFirst();if(!visited.add(folder.id()))continue;
                if(visited.size()>1000||folder.path().length()>2000)throw limit();String next=null;var cursors=new HashSet<String>();
                do{
                    if(++requests>200||Instant.now().isAfter(deadline))throw limit();
                    var page=isRclone(c)?rclone.children(owner,folder.id()):graph.children(access,c.driveId(),folder.id(),next);
                    for(var item:page.items()){
                        if(++count>10_000)throw limit();if(item.remote())continue;
                        if(item.folder()){pending.add(new Work(item.id(),folder.path()+"/"+item.name()));continue;}
                        int dot=item.name().lastIndexOf('.');String extension=dot<0?"":item.name().substring(dot+1).toLowerCase(Locale.ROOT);
                        if(EXTENSIONS.contains(extension))files.putIfAbsent(item.id(),new DigitalFile(null,item.id(),item.name(),extension,folder.path(),item.size(),item.modified(),item.webUrl(),true,null,null));
                    }
                    next=page.next();if(next!=null&&!cursors.add(next))throw limit();
                }while(next!=null);
            }
            if(Instant.now().isAfter(deadline))throw limit();repo.publish(owner,c.generation(),run,List.copyOf(files.values()));return files.size();
        }catch(RuntimeException error){repo.failed(owner,c.generation(),run);if(error instanceof OneDriveException)throw error;throw new OneDriveException("Não foi possível concluir a sincronização. A listagem anterior foi mantida.");}
    }
    private OneDriveException limit(){return new OneDriveException("A pasta excedeu o limite desta sincronização (10.000 itens, 1.000 pastas, 200 consultas ou 90 segundos). Selecione pastas menores. A listagem anterior foi mantida.");}
}
