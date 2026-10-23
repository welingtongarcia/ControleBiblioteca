package br.com.minhagibiteca.digital.infrastructure;

import br.com.minhagibiteca.digital.domain.OneDriveException;
import java.net.http.HttpClient;
import java.time.*;
import java.util.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.endpoint.*;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.*;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OneDriveOAuthClient {
    private final OneDriveSettings settings;
    private final RestClientAuthorizationCodeTokenResponseClient codes=new RestClientAuthorizationCodeTokenResponseClient();
    private final RestClientRefreshTokenTokenResponseClient refreshes=new RestClientRefreshTokenTokenResponseClient();
    public OneDriveOAuthClient(OneDriveSettings settings){
        this.settings=settings;
        var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build());factory.setReadTimeout(Duration.ofSeconds(15));
        var rest=RestClient.builder().requestFactory(factory).messageConverters(c->{c.clear();c.add(new FormHttpMessageConverter());c.add(new OAuth2AccessTokenResponseHttpMessageConverter());}).defaultStatusHandler(new OAuth2ErrorResponseErrorHandler()).build();
        codes.setRestClient(rest);refreshes.setRestClient(rest);
    }
    public OAuth2AuthorizationRequest authorization(){
        var reg=settings.registration();byte[] random=new byte[32];new java.security.SecureRandom().nextBytes(random);
        var request=OAuth2AuthorizationRequest.authorizationCode().authorizationUri(reg.getProviderDetails().getAuthorizationUri()).clientId(reg.getClientId()).redirectUri(reg.getRedirectUri()).scopes(reg.getScopes()).state(Base64.getUrlEncoder().withoutPadding().encodeToString(random)).additionalParameters(p->p.put("prompt","select_account"));
        OAuth2AuthorizationRequestCustomizers.withPkce().accept(request);return request.build();
    }
    public OAuth2AccessTokenResponse exchange(OAuth2AuthorizationRequest request,String code){
        try{var reg=settings.registration();var response=OAuth2AuthorizationResponse.success(code).state(request.getState()).redirectUri(reg.getRedirectUri()).build();return codes.getTokenResponse(new OAuth2AuthorizationCodeGrantRequest(reg,new OAuth2AuthorizationExchange(request,response)));}
        catch(RuntimeException error){throw new OneDriveException("Não foi possível concluir a autorização Microsoft. Confira a configuração e conecte novamente.");}
    }
    public OAuth2AccessTokenResponse refresh(String access,String refresh,Instant expires,Set<String> scopes){
        try{return refreshes.getTokenResponse(new OAuth2RefreshTokenGrantRequest(settings.registration(),new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,access,expires.minusSeconds(3600),expires,scopes),new OAuth2RefreshToken(refresh,Instant.EPOCH),scopes));}
        catch(RuntimeException error){throw new OneDriveException("A autorização OneDrive precisa ser renovada. Reconecte a conta.");}
    }
}
