package br.com.minhagibiteca.digital.infrastructure;

import br.com.minhagibiteca.digital.domain.OneDriveException;
import java.io.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OneDriveSecurityTest {
    static OneDriveSettings configured(){return new OneDriveSettings(true,"test-client","not-a-real-secret","http://localhost:8080/biblioteca-digital/onedrive/retorno",Base64.getEncoder().encodeToString(new byte[32]));}
    @Test void encryptsWithRandomNonceAndBindsCredentialsToOwner(){var cipher=new OneDriveTokenCipher(configured());var owner=UUID.randomUUID();String a=cipher.encrypt(owner,"synthetic-access-token"),b=cipher.encrypt(owner,"synthetic-access-token");assertThat(a).isNotEqualTo(b).doesNotContain("synthetic");assertThat(cipher.decrypt(owner,a)).isEqualTo("synthetic-access-token");assertThatThrownBy(()->cipher.decrypt(UUID.randomUUID(),a)).isInstanceOf(OneDriveException.class);assertThatThrownBy(()->cipher.decrypt(owner,a.substring(1))).isInstanceOf(OneDriveException.class);}
    @Test void usesPkceStateAndReadOnlyScopes()throws Exception{var request=new OneDriveOAuthClient(configured()).authorization();assertThat(request.getScopes()).containsExactlyInAnyOrder("Files.Read","offline_access");assertThat(request.getState()).hasSizeGreaterThanOrEqualTo(32);assertThat(request.getAdditionalParameters()).containsEntry("code_challenge_method","S256");String verifier=request.getAttribute("code_verifier");assertThat(request.getAdditionalParameters().get("code_challenge")).isEqualTo(Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII))));assertThat(request.getAuthorizationRequestUri()).doesNotContain("not-a-real-secret",verifier);}
    @Test void rejectsUnsafeConfigurationAndWebLinks(){assertThatThrownBy(()->new OneDriveSettings(true,"id","secret","http://public.example/biblioteca-digital/onedrive/retorno",configured().key())).isInstanceOf(IllegalStateException.class);assertThat(MicrosoftGraphClient.safeWebUrl("javascript:alert(1)")).isNull();assertThat(MicrosoftGraphClient.safeWebUrl("https://evil.example/file.pdf")).isNull();assertThat(MicrosoftGraphClient.safeWebUrl("https://onedrive.live.com/?id=1")).isNotNull();assertThat(MicrosoftGraphClient.safeWebUrl("https://tenant.sharepoint.com/file.pdf")).isNotNull();}
    @Test void refusesForeignPaginationHostsAndPathsBeforeSendingTokens(){var http=mock(HttpClient.class);var graph=new MicrosoftGraphClient(http);for(String next:List.of("http://graph.microsoft.com/v1.0/drives/drive/items/A/children","https://evil.example/v1.0/drives/drive/items/A/children","https://graph.microsoft.com/v1.0/me/messages","https://user@graph.microsoft.com/v1.0/drives/drive/items/A/children"))assertThatThrownBy(()->graph.children("sensitive-token","drive","A",next)).isInstanceOf(OneDriveException.class);verifyNoInteractions(http);}
    @SuppressWarnings("unchecked") @Test void readsOnlyMetadataAndHandlesProviderErrorsWithoutLeakingTokens()throws Exception{
        var http=mock(HttpClient.class);HttpResponse<InputStream> response=mock(HttpResponse.class);when(http.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(response);
        var graph=new MicrosoftGraphClient(http);when(response.statusCode()).thenReturn(200);when(response.body()).thenReturn(new ByteArrayInputStream("{\"value\":[{\"id\":\"A\",\"name\":\"Comic.PDF\",\"file\":{},\"folder\":null,\"remoteItem\":null,\"size\":1024,\"webUrl\":\"https://onedrive.live.com/a\"}]}".getBytes(StandardCharsets.UTF_8)));
        var page=graph.children("sensitive-token","drive","root",null);assertThat(page.items()).hasSize(1);assertThat(page.items().getFirst().name()).isEqualTo("Comic.PDF");assertThat(page.items().getFirst().folder()).isFalse();assertThat(page.items().getFirst().remote()).isFalse();
        var captured=org.mockito.ArgumentCaptor.forClass(HttpRequest.class);verify(http).send(captured.capture(),any(HttpResponse.BodyHandler.class));assertThat(captured.getValue().method()).isEqualTo("GET");assertThat(captured.getValue().uri().getHost()).isEqualTo("graph.microsoft.com");assertThat(captured.getValue().uri().toString()).doesNotContain("content","sensitive-token");
        for(int status:List.of(401,403,429,503,500)){when(response.statusCode()).thenReturn(status);when(response.body()).thenReturn(new ByteArrayInputStream("sensitive-token provider message".getBytes()));assertThatThrownBy(()->graph.children("sensitive-token","drive","root",null)).isInstanceOf(OneDriveException.class).hasMessageNotContaining("sensitive-token").hasMessageNotContaining("provider message");}
    }
}
