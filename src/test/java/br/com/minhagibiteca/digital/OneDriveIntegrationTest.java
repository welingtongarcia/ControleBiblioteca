package br.com.minhagibiteca.digital;

import br.com.minhagibiteca.auth.application.UserPrincipal;
import br.com.minhagibiteca.auth.domain.*;
import br.com.minhagibiteca.auth.infrastructure.UserAccountRepository;
import br.com.minhagibiteca.backup.infrastructure.BackupRepository;
import br.com.minhagibiteca.digital.application.OneDriveService;
import br.com.minhagibiteca.digital.domain.*;
import br.com.minhagibiteca.digital.infrastructure.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"app.rclone.enabled=true","app.onedrive.enabled=true","app.onedrive.client-id=test-client","app.onedrive.client-secret=synthetic-secret","app.onedrive.redirect-uri=http://localhost:8080/biblioteca-digital/onedrive/retorno","app.onedrive.token-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="})
@AutoConfigureMockMvc @Testcontainers
class OneDriveIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",POSTGRES::getJdbcUrl);r.add("spring.datasource.username",POSTGRES::getUsername);r.add("spring.datasource.password",POSTGRES::getPassword);}
    @Autowired MockMvc mvc;@Autowired JdbcTemplate jdbc;@Autowired UserAccountRepository users;@Autowired OneDriveService service;@Autowired OneDriveRepository repo;@Autowired OneDriveTokenCipher cipher;@Autowired OneDriveSettings settings;@Autowired BackupRepository backup;
    @MockitoBean MicrosoftGraphClient graph;@MockitoBean OneDriveOAuthClient oauth;
    @MockitoBean RcloneCommandRunner rcloneRunner;
    UserAccount owner,other;
    @BeforeEach void setup(){jdbc.update("delete from onedrive_connections");jdbc.update("delete from editions");users.deleteAll();owner=users.save(UserAccount.create("cloud@example.com","hash",UserRole.USER));other=users.save(UserAccount.create("other-cloud@example.com","hash",UserRole.USER));when(oauth.authorization()).thenAnswer(x->new OneDriveOAuthClient(settings).authorization());}
    void connect(){repo.connect(owner.getId(),"drive","Minha nuvem",cipher.encrypt(owner.getId(),"synthetic-access"),cipher.encrypt(owner.getId(),"synthetic-refresh"),Instant.now().plusSeconds(3600),"Files.Read");}
    MicrosoftGraphClient.Item folder(String id){return new MicrosoftGraphClient.Item(id,"Pasta "+id,true,false,0,null,null);}
    MicrosoftGraphClient.Item file(String id,String name){return new MicrosoftGraphClient.Item(id,name,false,false,1024,Instant.parse("2026-10-20T12:00:00Z"),"https://onedrive.live.com/?id="+id);}
    void selectA(){when(graph.folder(anyString(),eq("drive"),eq("A"))).thenReturn(folder("A"));service.select(owner.getId(),"A");}
    void oneFile(){connect();selectA();when(graph.children(anyString(),eq("drive"),eq("A"),isNull())).thenReturn(new MicrosoftGraphClient.Items(List.of(file("F","Quadrinho.PDF")),null));assertThat(service.synchronize(owner.getId())).isEqualTo(1);}
    void connectRclone(){
        when(rcloneRunner.run(eq(owner.getId()),eq(List.of("listremotes","--long")))).thenReturn("onedrive: onedrive\n");
        when(rcloneRunner.run(eq(owner.getId()),eq(List.of("lsjson","onedrive:","--stat","--no-mimetype")))).thenReturn("{\"IsDir\":true}");
        service.connectRclone(owner.getId());
    }
    @Test void rcloneRequiresCsrfAndUsesOnlyAuthenticatedOwnersConfiguration()throws Exception{
        mvc.perform(post("/biblioteca-digital/rclone/conectar").with(user(UserPrincipal.from(owner)))).andExpect(status().isForbidden());
        verifyNoInteractions(rcloneRunner);
        connectRclone();
        var connection=repo.connection(owner.getId()).orElseThrow();
        assertThat(connection.scopes()).isEqualTo("rclone");assertThat(connection.accessToken()).isEqualTo("external");
        assertThat(repo.connection(other.getId())).isEmpty();
        String html=mvc.perform(get("/biblioteca-digital").with(user(UserPrincipal.from(owner)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(html).contains("Conectado ao OneDrive via rclone",owner.getId().toString(),"docs/onedrive-rclone.md","name=\"_csrf\"").doesNotContain(other.getId().toString());
        java.nio.file.Files.createDirectories(java.nio.file.Path.of("target/ui-review"));
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/ui-review/rclone-biblioteca.html"),html);
        when(rcloneRunner.run(eq(other.getId()),anyList())).thenThrow(new OneDriveException("Autorização ausente"));
        mvc.perform(post("/biblioteca-digital/rclone/conectar").with(user(UserPrincipal.from(other))).with(csrf())).andExpect(redirectedUrl("/biblioteca-digital")).andExpect(flash().attribute("errorMessage","Autorização ausente"));
        assertThat(repo.connection(other.getId())).isEmpty();verifyNoInteractions(graph,oauth);
    }
    @Test void rcloneScansSubfoldersPreservesLinksOnFailureAndNeverCreatesPublicLinks()throws Exception{
        connectRclone();service.select(owner.getId(),"root");
        when(rcloneRunner.run(eq(owner.getId()),eq(List.of("lsjson","onedrive:","--no-mimetype")))).thenReturn("[{\"Name\":\"Livros\",\"IsDir\":true},{\"ID\":\"f1\",\"Name\":\"Quadrinho.pdf\",\"Size\":100}]");
        when(rcloneRunner.run(eq(owner.getId()),eq(List.of("lsjson","onedrive:Livros","--no-mimetype")))).thenReturn("[{\"ID\":\"f2\",\"Name\":\"Livro.epub\",\"Size\":200},{\"ID\":\"f3\",\"Name\":\"notas.txt\",\"Size\":1}]");
        assertThat(service.synchronize(owner.getId())).isEqualTo(2);
        var file=repo.files(owner.getId(),"","pdf",0).getContent().getFirst();
        UUID edition=UUID.randomUUID();jdbc.update("insert into editions(id,owner_user_id,title,normalized_title) values(?,?,'HQ','hq')",edition,owner.getId());repo.link(file.id(),owner.getId(),edition);
        assertThat(file.webUrl()).isNull();
        when(rcloneRunner.run(eq(owner.getId()),eq(List.of("lsjson","onedrive:Livros","--no-mimetype")))).thenThrow(new OneDriveException("Falha sintética"));
        assertThatThrownBy(()->service.synchronize(owner.getId())).isInstanceOf(OneDriveException.class);
        assertThat(repo.file(file.id(),owner.getId()).editionId()).isEqualTo(edition);
        assertThat(repo.file(file.id(),owner.getId()).available()).isTrue();
        assertThat(repo.files(other.getId(),"","",0).isEmpty()).isTrue();
        mvc.perform(get("/biblioteca-digital/arquivos/"+file.id()+"/vincular").with(user(UserPrincipal.from(other)))).andExpect(status().isNotFound());
        mvc.perform(get("/biblioteca-digital").with(user(UserPrincipal.from(owner)))).andExpect(status().isOk());
        verifyNoInteractions(graph,oauth);
    }
    @Test void rcloneSwitchAndDisconnectClearOnlyLocalConnection()throws Exception{
        oneFile();assertThat(repo.files(owner.getId(),"","",0).getTotalElements()).isEqualTo(1);
        connectRclone();assertThat(repo.files(owner.getId(),"","",0).getTotalElements()).isZero();assertThat(repo.folders(owner.getId())).isEmpty();
        clearInvocations(rcloneRunner);
        mvc.perform(post("/biblioteca-digital/onedrive/desconectar").with(user(UserPrincipal.from(owner))).with(csrf())).andExpect(redirectedUrl("/biblioteca-digital"));
        assertThat(repo.connection(owner.getId())).isEmpty();verifyNoInteractions(rcloneRunner);
    }
    @Test void rcloneWorksWithDirectOAuthDisabledAndDoesNotNeedCipherOrClientCredentials(){
        connectRclone();
        var rclone=new RcloneClient(new RcloneSettings(true,"rclone","unused"),rcloneRunner);
        var withoutAzure=new OneDriveService(new OneDriveSettings(false,"","","",""),repo,cipher,oauth,graph,rclone);
        assertThat(withoutAzure.status(owner.getId()).configured()).isTrue();
        assertThat(withoutAzure.oauthEnabled()).isFalse();
        withoutAzure.select(owner.getId(),"root");
        when(rcloneRunner.run(eq(owner.getId()),eq(List.of("lsjson","onedrive:","--no-mimetype")))).thenReturn("[]");
        assertThat(withoutAzure.synchronize(owner.getId())).isZero();
        verifyNoInteractions(oauth,graph);
    }
    @Test void rendersConnectedLibraryFolderPickerAndEditionLinkWithEscapedMetadata()throws Exception{
        oneFile();var auth=user(UserPrincipal.from(owner));var file=repo.files(owner.getId(),"","",0).getContent().getFirst();
        UUID edition=UUID.randomUUID();jdbc.update("insert into editions(id,owner_user_id,title,normalized_title) values(?,?,'Livro para vincular','livro para vincular')",edition,owner.getId());
        jdbc.update("update onedrive_files set name=? where id=?","HQ <script>.pdf",file.id());
        var preview=java.nio.file.Path.of("target/ui-review");java.nio.file.Files.createDirectories(preview.resolve("css"));java.nio.file.Files.createDirectories(preview.resolve("js"));
        for(String css:List.of("main","dashboard","home-showcase"))java.nio.file.Files.copy(java.nio.file.Path.of("src/main/resources/static/css/"+css+".css"),preview.resolve("css/"+css+".css"),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        for(String js:List.of("sidebar","home-banner"))java.nio.file.Files.copy(java.nio.file.Path.of("src/main/resources/static/js/"+js+".js"),preview.resolve("js/"+js+".js"),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        for(var entry:Map.of("onedrive-biblioteca","/biblioteca-digital","onedrive-pastas","/biblioteca-digital/pastas?folder=A","onedrive-vincular","/biblioteca-digital/arquivos/"+file.id()+"/vincular").entrySet()){
            String html=mvc.perform(get(entry.getValue()).with(auth)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            assertThat(html).contains("Biblioteca digital","name=\"_csrf\"").doesNotContain("synthetic-access","synthetic-refresh","HQ <script>");java.nio.file.Files.writeString(preview.resolve(entry.getKey()+".html"),html);
        }
    }
    @Test void rejectsOversizedScanWithoutPublishingPartialResults(){
        oneFile();when(graph.children(anyString(),eq("drive"),eq("A"),isNull())).thenReturn(new MicrosoftGraphClient.Items(Collections.nCopies(10001,file("X","Duplicado.pdf")),null));
        assertThatThrownBy(()->service.synchronize(owner.getId())).isInstanceOf(OneDriveException.class).hasMessageContaining("10.000");assertThat(repo.files(owner.getId(),"","",0).getContent().getFirst().name()).isEqualTo("Quadrinho.PDF");
    }
    @Test void authorizesWithOwnerBoundOneTimeStateAndNeverUsesMicrosoftLoginAsLocalLogin()throws Exception{
        mvc.perform(get("/biblioteca-digital")).andExpect(status().is3xxRedirection());mvc.perform(post("/biblioteca-digital/onedrive/conectar").with(user(UserPrincipal.from(owner)))).andExpect(status().isForbidden());
        var result=mvc.perform(post("/biblioteca-digital/onedrive/conectar").with(user(UserPrincipal.from(owner))).with(csrf())).andExpect(status().is3xxRedirection()).andReturn();
        assertThat(result.getResponse().getRedirectedUrl()).startsWith("https://login.microsoftonline.com/common/oauth2/v2.0/authorize").contains("code_challenge").doesNotContain("synthetic-secret");
        var session=(MockHttpSession)result.getRequest().getSession();var pending=(OneDriveService.Pending)session.getAttribute("onedrive.pending");
        mvc.perform(get("/biblioteca-digital/onedrive/retorno").session(session).with(user(UserPrincipal.from(other))).param("state",pending.request().getState()).param("code","code" )).andExpect(redirectedUrl("/biblioteca-digital"));verify(oauth,never()).exchange(any(),anyString());
        when(oauth.exchange(any(),eq("code"))).thenReturn(OAuth2AccessTokenResponse.withToken("synthetic-access").tokenType(OAuth2AccessToken.TokenType.BEARER).expiresIn(3600).refreshToken("synthetic-refresh").scopes(Set.of("Files.Read")).build());when(graph.drive("synthetic-access")).thenReturn(new MicrosoftGraphClient.Drive("drive","Minha nuvem"));
        mvc.perform(get("/biblioteca-digital/onedrive/retorno").session(session).with(user(UserPrincipal.from(owner))).param("state",pending.request().getState()).param("code","code")).andExpect(redirectedUrl("/biblioteca-digital"));
        assertThat(repo.connection(owner.getId())).isPresent();assertThat(repo.connection(other.getId())).isEmpty();assertThat(repo.connection(owner.getId()).orElseThrow().accessToken()).doesNotContain("synthetic-access");
        mvc.perform(get("/biblioteca-digital/onedrive/retorno").session(session).with(user(UserPrincipal.from(owner))).param("state",pending.request().getState()).param("code","code")).andExpect(redirectedUrl("/biblioteca-digital"));verify(oauth,times(1)).exchange(any(),anyString());
    }
    @Test void refusesExpiredAndMismatchedState(){var pending=service.begin(owner.getId());assertThat(service.valid(pending,owner.getId(),pending.request().getState())).isTrue();assertThat(service.valid(pending,owner.getId(),"wrong")).isFalse();assertThat(service.valid(new OneDriveService.Pending(owner.getId(),Instant.EPOCH,pending.request()),owner.getId(),pending.request().getState())).isFalse();}
    @Test void indexesSubfoldersPaginationAndSupportedFilesWithoutDuplicates(){
        connect();selectA();when(graph.folder(anyString(),eq("drive"),eq("B"))).thenReturn(folder("B"));service.select(owner.getId(),"B");
        when(graph.children(anyString(),eq("drive"),eq("A"),isNull())).thenReturn(new MicrosoftGraphClient.Items(List.of(folder("B"),file("F","Quadrinho.PDF")),"next"));
        when(graph.children(anyString(),eq("drive"),eq("A"),eq("next"))).thenReturn(new MicrosoftGraphClient.Items(List.of(file("E","Livro.epub"),file("X","outro.txt")),null));
        when(graph.children(anyString(),eq("drive"),eq("B"),isNull())).thenReturn(new MicrosoftGraphClient.Items(List.of(file("C","Comic.cbr")),null));
        assertThat(service.synchronize(owner.getId())).isEqualTo(3);assertThat(repo.files(owner.getId(),"","",0).getTotalElements()).isEqualTo(3);assertThat(repo.files(owner.getId(),"","pdf",0).getTotalElements()).isEqualTo(1);assertThat(repo.files(other.getId(),"","",0).isEmpty()).isTrue();assertThat(jdbc.queryForObject("select count(*) from editions",Long.class)).isZero();verify(graph,times(1)).children(anyString(),eq("drive"),eq("B"),isNull());
    }
    @Test void preservesPreviousIndexAndLinksWhenScanFailsAndMarksMissingFilesAfterCompleteScan(){
        oneFile();var original=repo.files(owner.getId(),"","",0).getContent().getFirst();var synced=repo.connection(owner.getId()).orElseThrow().lastSync();
        when(graph.children(anyString(),eq("drive"),eq("A"),isNull())).thenThrow(new OneDriveException("Consulta limitada."));assertThatThrownBy(()->service.synchronize(owner.getId())).isInstanceOf(OneDriveException.class);assertThat(repo.file(original.id(),owner.getId()).available()).isTrue();assertThat(repo.connection(owner.getId()).orElseThrow().lastSync()).isEqualTo(synced);
        when(graph.children(anyString(),eq("drive"),eq("A"),isNull())).thenReturn(new MicrosoftGraphClient.Items(List.of(),null));assertThat(service.synchronize(owner.getId())).isZero();assertThat(repo.file(original.id(),owner.getId()).available()).isFalse();
        repo.removeFolder(repo.folders(owner.getId()).getFirst().id(),owner.getId());clearInvocations(graph);assertThat(service.synchronize(owner.getId())).isZero();verifyNoInteractions(graph);
    }
    @Test void blocksConcurrentScansAndFolderChangesAndRejectsStalePublication(){
        connect();selectA();var generation=repo.connection(owner.getId()).orElseThrow().generation();UUID run=repo.begin(owner.getId(),generation);assertThatThrownBy(()->service.synchronize(owner.getId())).isInstanceOf(OneDriveException.class);assertThatThrownBy(()->repo.removeFolder(repo.folders(owner.getId()).getFirst().id(),owner.getId())).isInstanceOf(OneDriveException.class);repo.disconnect(owner.getId());connect();assertThatThrownBy(()->repo.publish(owner.getId(),generation,run,List.of())).isInstanceOf(OneDriveException.class);assertThat(repo.files(owner.getId(),"","",0)).isEmpty();
    }
    @Test void linksOnlyOwnedEditionsAndDoesNotExportCredentialsOrCloudIndex()throws Exception{
        oneFile();var file=repo.files(owner.getId(),"","",0).getContent().getFirst();UUID edition=UUID.randomUUID(),foreign=UUID.randomUUID();jdbc.update("insert into editions(id,owner_user_id,title,normalized_title) values(?,?,'Minha edição','minha edição'),(?,?,'Privada','privada')",edition,owner.getId(),foreign,other.getId());
        mvc.perform(post("/biblioteca-digital/arquivos/"+file.id()+"/vincular").with(user(UserPrincipal.from(owner))).with(csrf()).param("editionId",foreign.toString())).andExpect(status().isNotFound());
        mvc.perform(get("/biblioteca-digital/arquivos/"+file.id()+"/vincular").with(user(UserPrincipal.from(other)))).andExpect(status().isNotFound());
        mvc.perform(post("/biblioteca-digital/arquivos/"+file.id()+"/vincular").with(user(UserPrincipal.from(owner))).with(csrf()).param("editionId",edition.toString())).andExpect(redirectedUrl("/biblioteca-digital"));assertThat(repo.file(file.id(),owner.getId()).editionId()).isEqualTo(edition);
        assertThat(backup.snapshot(owner.getId()).toString()).doesNotContain("onedrive_connections","synthetic-access","synthetic-refresh","Quadrinho.PDF");
        jdbc.update("delete from editions where id=?",edition);assertThat(repo.file(file.id(),owner.getId()).editionId()).isNull();repo.disconnect(owner.getId());assertThat(repo.files(owner.getId(),"","",0)).isEmpty();
    }
    @Test void refreshesExpiredTokensAndPaginatesCachedFiles(){
        repo.connect(owner.getId(),"drive","Minha nuvem",cipher.encrypt(owner.getId(),"old-access"),cipher.encrypt(owner.getId(),"old-refresh"),Instant.now().minusSeconds(5),"Files.Read");
        when(oauth.refresh(eq("old-access"),eq("old-refresh"),any(),any())).thenReturn(OAuth2AccessTokenResponse.withToken("new-access").tokenType(OAuth2AccessToken.TokenType.BEARER).expiresIn(3600).refreshToken("new-refresh").scopes(Set.of("Files.Read")).build());
        when(graph.children(eq("new-access"),eq("drive"),eq("root"),isNull())).thenReturn(new MicrosoftGraphClient.Items(List.of(),null));service.browse(owner.getId(),"root",null);assertThat(cipher.decrypt(owner.getId(),repo.connection(owner.getId()).orElseThrow().refreshToken())).isEqualTo("new-refresh");
        var generation=repo.connection(owner.getId()).orElseThrow().generation();UUID run=repo.begin(owner.getId(),generation);var files=new ArrayList<DigitalFile>();for(int i=0;i<25;i++)files.add(new DigitalFile(null,"F"+i,"Livro "+i+".pdf","pdf","Pasta",1024,null,null,true,null,null));repo.publish(owner.getId(),generation,run,files);assertThat(repo.files(owner.getId(),"Livro","",1).getNumberOfElements()).isEqualTo(5);
    }
}
