package br.com.minhagibiteca.digital.infrastructure;

import br.com.minhagibiteca.digital.domain.OneDriveException;
import com.fasterxml.jackson.databind.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class MicrosoftGraphClient {
    private final HttpClient http;
    public MicrosoftGraphClient(){this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build());}
    MicrosoftGraphClient(HttpClient http){this.http=http;}
    private final ObjectMapper mapper=new ObjectMapper();
    public record Item(String id,String name,boolean folder,boolean remote,long size,Instant modified,String webUrl){}
    public record Items(List<Item> items,String next){}
    public record Drive(String id,String name){}
    public Drive drive(String token){var n=get(URI.create("https://graph.microsoft.com/v1.0/me/drive?$select=id,name"),token);return new Drive(id(n.path("id").asText()),name(n.path("name").asText("OneDrive"),300));}
    public Item folder(String token,String drive,String folder){var item=item(get(uri(drive,folder,false),token));if(!item.folder()||item.remote())throw new OneDriveException("Selecione uma pasta do seu OneDrive. Atalhos compartilhados não são percorridos nesta versão.");return item;}
    public Items children(String token,String drive,String folder,String next){
        URI base=uri(drive,folder,true),request=base;
        if(next!=null&&!next.isBlank()){
            try{request=URI.create(next);if(next.length()>6000||!request.getScheme().equals("https")||!"graph.microsoft.com".equals(request.getHost())||request.getPort()!=-1||request.getUserInfo()!=null||request.getFragment()!=null||!base.getRawPath().equals(request.getRawPath()))throw new IllegalArgumentException();}catch(RuntimeException e){throw new OneDriveException("Paginação do OneDrive inválida. Reabra a pasta.");}
        }
        var n=get(request,token);if(!n.path("value").isArray())throw invalid();var items=new ArrayList<Item>();for(var row:n.path("value"))items.add(item(row));String after=n.path("@odata.nextLink").asText(null);return new Items(List.copyOf(items),after);
    }
    private URI uri(String drive,String folder,boolean children){
        var b=UriComponentsBuilder.fromUriString("https://graph.microsoft.com/v1.0").pathSegment("drives",id(drive));
        if(folder.equals("root"))b.pathSegment("root");else b.pathSegment("items",id(folder));
        if(children)b.pathSegment("children");
        b.queryParam("$select","id,name,folder,file,size,lastModifiedDateTime,webUrl,remoteItem");if(children)b.queryParam("$top",200);return b.build().encode().toUri();
    }
    private Item item(JsonNode n){Instant modified=null;try{if(n.hasNonNull("lastModifiedDateTime"))modified=Instant.parse(n.path("lastModifiedDateTime").asText());}catch(RuntimeException ignored){}
        boolean folder=n.path("folder").isObject(),remote=n.hasNonNull("remoteItem");if(!folder&&!remote&&!n.path("file").isObject())return new Item(id(n.path("id").asText()),name(n.path("name").asText(),400),false,true,0,modified,null);
        return new Item(id(n.path("id").asText()),name(n.path("name").asText(),400),folder,remote,Math.max(0,n.path("size").asLong()),modified,safeWebUrl(n.path("webUrl").asText(null)));
    }
    public static String safeWebUrl(String url){try{URI u=URI.create(url);String host=u.getHost();if(!"https".equals(u.getScheme())||u.getUserInfo()!=null||host==null||url.length()>3000)return null;host=host.toLowerCase(Locale.ROOT);return host.equals("onedrive.live.com")||host.equals("1drv.ms")||host.endsWith(".sharepoint.com")?url:null;}catch(RuntimeException e){return null;}}
    private JsonNode get(URI uri,String token){
        try{
            var response=http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15)).header("Authorization","Bearer "+token).header("Accept","application/json").GET().build(),HttpResponse.BodyHandlers.ofInputStream());
            try(var stream=response.body()){
                if(response.statusCode()==401)throw new OneDriveException("A autorização OneDrive expirou ou foi revogada. Reconecte a conta.");
                if(response.statusCode()==429||response.statusCode()==503)throw new OneDriveException("O OneDrive está limitando as consultas. Aguarde alguns minutos e tente novamente.");
                if(response.statusCode()==403)throw new OneDriveException("A conta não autorizou esta leitura. Confira o consentimento ou a política da organização.");
                if(response.statusCode()<200||response.statusCode()>299)throw new OneDriveException("Não foi possível consultar o OneDrive. A pasta pode ter sido movida ou removida.");
                byte[] body=stream.readNBytes(1_048_577);if(body.length>1_048_576)throw invalid();var json=mapper.readTree(body);if(json==null||!json.isObject())throw invalid();return json;
            }
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw new OneDriveException("Consulta interrompida. Tente novamente.");}
        catch(IOException|IllegalArgumentException e){throw new OneDriveException("Não foi possível acessar o OneDrive. Confira a conexão e tente novamente.");}
    }
    private static String id(String value){if(value==null||value.equals(".")||value.equals("..")||!value.matches("[A-Za-z0-9!_.-]{1,256}"))throw invalid();return value;}
    private static String name(String value,int max){if(value==null||value.isBlank()||value.length()>max)throw invalid();return value;}
    private static OneDriveException invalid(){return new OneDriveException("O OneDrive retornou dados inválidos ou acima do limite permitido.");}
}
