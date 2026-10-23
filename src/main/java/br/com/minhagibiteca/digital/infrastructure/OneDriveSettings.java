package br.com.minhagibiteca.digital.infrastructure;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.*;

@Component
public class OneDriveSettings {
    private final boolean enabled;
    private final ClientRegistration registration;
    private final String key;
    public OneDriveSettings(@Value("${app.onedrive.enabled:false}") boolean enabled,
            @Value("${app.onedrive.client-id:}") String clientId,@Value("${app.onedrive.client-secret:}") String secret,
            @Value("${app.onedrive.redirect-uri:}") String redirect,@Value("${app.onedrive.token-key:}") String key){
        this.enabled=enabled;this.key=key;
        if(!enabled){registration=null;return;}
        try{
            if(clientId.isBlank()||secret.isBlank()||java.util.Base64.getDecoder().decode(key).length!=32)throw new IllegalArgumentException();
            URI uri=URI.create(redirect);
            boolean local=java.util.Set.of("localhost","127.0.0.1","[::1]").contains(uri.getHost()==null?"":uri.getHost());
            if(uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||!uri.getPath().endsWith("/biblioteca-digital/onedrive/retorno")||!(uri.getScheme().equals("https")||(local&&uri.getScheme().equals("http"))))throw new IllegalArgumentException();
            registration=ClientRegistration.withRegistrationId("onedrive").clientId(clientId).clientSecret(secret)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST).authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri(redirect).scope("Files.Read","offline_access")
                    .authorizationUri("https://login.microsoftonline.com/common/oauth2/v2.0/authorize")
                    .tokenUri("https://login.microsoftonline.com/common/oauth2/v2.0/token").clientName("OneDrive").build();
        }catch(RuntimeException error){throw new IllegalStateException("Configuração OneDrive inválida: confira ID, segredo, retorno HTTPS (ou HTTP localhost) e chave Base64 de 32 bytes.");}
    }
    public boolean isEnabled(){return enabled;}
    public ClientRegistration registration(){if(!enabled)throw new br.com.minhagibiteca.digital.domain.OneDriveException("A integração OneDrive ainda não foi configurada nesta instalação.");return registration;}
    String key(){return key;}
}
