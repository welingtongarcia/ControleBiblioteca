package br.com.minhagibiteca.catalog;

import br.com.minhagibiteca.auth.application.UserPrincipal;
import br.com.minhagibiteca.auth.domain.*;
import br.com.minhagibiteca.auth.infrastructure.UserAccountRepository;
import br.com.minhagibiteca.catalog.application.*;
import br.com.minhagibiteca.catalog.presentation.EditionForm;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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

@SpringBootTest @AutoConfigureMockMvc @Testcontainers
class AmazonImportIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) { r.add("spring.datasource.url",POSTGRES::getJdbcUrl);r.add("spring.datasource.username",POSTGRES::getUsername);r.add("spring.datasource.password",POSTGRES::getPassword); }
    @Autowired MockMvc mvc;@Autowired JdbcTemplate jdbc;@Autowired UserAccountRepository users;
    @MockitoBean AmazonPageProvider page;@MockitoBean IsbnLookupService isbn;
    UserAccount owner,other;
    @BeforeEach void setup() {
        jdbc.update("delete from editions");
        jdbc.update("delete from creators");
        jdbc.update("delete from publishers");
        jdbc.update("delete from users");
        owner=users.save(UserAccount.create("amazon-test@example.com","hash",UserRole.USER));
        other=users.save(UserAccount.create("amazon-other@example.com","hash",UserRole.USER));
    }
    @Test void previewsWithCsrfAndOwnerTokenAppliesOnlyEmptyFieldsAndSavesOnlyOnExplicitRegistration() throws Exception {
        var auth=user(UserPrincipal.from(owner));var product=new AmazonProductLink("655448244X");
        UUID publisher=UUID.randomUUID(),foreignPublisher=UUID.randomUUID(),creator=UUID.randomUUID();
        jdbc.update("insert into publishers(id,owner_user_id,name,normalized_name) values(?,?,'Editora Amazon','editora amazon'),(?,?,'Editora Amazon','editora amazon')",publisher,owner.getId(),foreignPublisher,other.getId());
        jdbc.update("insert into creators(id,owner_user_id,name,normalized_name) values(?,?,'Criador Amazon','criador amazon')",creator,owner.getId());
        var metadata=new AmazonMetadata(product.isbn(),"Thorgal <especial>",2025,420,"Capa dura",List.of("Editora Amazon"),List.of("Criador Amazon"),"Sinopse encontrada",null,"Amazon Brasil",product.url());
        when(page.lookup(product)).thenReturn(Optional.of(metadata));
        mvc.perform(post("/edicoes/amazon/buscar").with(auth).param("amazonLink",product.url())).andExpect(status().isForbidden());
        mvc.perform(post("/edicoes/amazon/buscar").with(csrf()).param("amazonLink",product.url())).andExpect(status().is3xxRedirection());
        verifyNoInteractions(page);
        var search=mvc.perform(post("/edicoes/amazon/buscar").with(auth).with(csrf()).param("amazonLink",product.url()+"?tracking=private").param("referencePaidPrice","19,50").param("mediaType","DIGITAL"))
                .andExpect(redirectedUrl("/edicoes/nova")).andReturn();
        var preview=(AmazonImportService.Preview)search.getFlashMap().get("amazonPreview");
        var draft=(EditionForm)search.getFlashMap().get("editionForm");
        assertThat(draft.getTitle()).isNull();assertThat(draft.getReferencePaidPrice()).isEqualByComparingTo("19.50");
        assertThat(jdbc.queryForObject("select count(*) from editions",Long.class)).isZero();
        String html=mvc.perform(get("/edicoes/nova").with(auth).flashAttrs(search.getFlashMap())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(html).contains("Confira os dados antes de aplicar","Thorgal &lt;especial&gt;","name=\"_csrf\"","/edicoes/amazon/aplicar").doesNotContain("Thorgal <especial>","tracking=private");
        Files.createDirectories(Path.of("target/ui-review"));Files.writeString(Path.of("target/ui-review/amazon-preview.html"),html);
        mvc.perform(post("/edicoes/amazon/aplicar").with(auth).param("amazonToken",preview.token().toString())).andExpect(status().isForbidden());
        mvc.perform(post("/edicoes/amazon/aplicar").with(user(UserPrincipal.from(other))).with(csrf()).param("amazonToken",preview.token().toString())).andExpect(flash().attributeExists("amazonMessage"));
        var foreign=mvc.perform(post("/edicoes/amazon/aplicar").with(user(UserPrincipal.from(other))).with(csrf()).param("amazonToken",preview.token().toString())).andReturn();
        assertThat(((EditionForm)foreign.getFlashMap().get("editionForm")).getTitle()).isNull();
        var applied=mvc.perform(post("/edicoes/amazon/aplicar").with(auth).with(csrf()).param("amazonToken",preview.token().toString()).param("referencePaidPrice","19,50").param("mediaType","DIGITAL").param("purchaseDate","2026-01-01").param("pagesRead","7"))
                .andExpect(redirectedUrl("/edicoes/nova")).andReturn();
        var form=(EditionForm)applied.getFlashMap().get("editionForm");
        assertThat(form.getTitle()).isEqualTo(metadata.title());assertThat(form.getIsbn()).isEqualTo(product.isbn());
        assertThat(form.getTotalPages()).isEqualTo(420);assertThat(form.getPagesRead()).isEqualTo(7);
        assertThat(form.getPublisherId()).isEqualTo(publisher);assertThat(form.getCreatorIds()).containsExactly(creator);
        assertThat(form.getNotes()).isEqualTo("Sinopse encontrada");assertThat(form.getReferencePaidPrice()).isEqualByComparingTo("19.50");
        assertThat(jdbc.queryForObject("select count(*) from editions",Long.class)).isZero();
        var preserved=mvc.perform(post("/edicoes/amazon/aplicar").with(auth).with(csrf()).param("amazonToken",preview.token().toString()).param("title","Título manual").param("isbn","manual").param("publicationYear","2020").param("totalPages","500").param("format","Meu formato").param("notes","Minhas notas").param("publisherId",publisher.toString()).param("creatorIds",creator.toString()))
                .andExpect(redirectedUrl("/edicoes/nova")).andReturn();
        var manual=(EditionForm)preserved.getFlashMap().get("editionForm");
        assertThat(manual.getTitle()).isEqualTo("Título manual");assertThat(manual.getIsbn()).isEqualTo("manual");assertThat(manual.getPublicationYear()).isEqualTo(2020);
        assertThat(manual.getTotalPages()).isEqualTo(500);assertThat(manual.getFormat()).isEqualTo("Meu formato");assertThat(manual.getNotes()).isEqualTo("Minhas notas");
        mvc.perform(post("/edicoes").with(auth).with(csrf()).param("title",form.getTitle()).param("isbn",form.getIsbn()).param("totalPages","420"))
                .andExpect(status().is3xxRedirection());
        assertThat(jdbc.queryForObject("select count(*) from editions where owner_user_id=?",Long.class,owner.getId())).isEqualTo(1);
        verifyNoInteractions(isbn);
    }
    @Test void invalidLinkAndBindingErrorsPreserveDraftWithoutRequests() throws Exception {
        var auth=user(UserPrincipal.from(owner));
        var invalid=mvc.perform(post("/edicoes/amazon/buscar").with(auth).with(csrf()).param("amazonLink","https://127.0.0.1/private").param("title","Rascunho").param("notes","Não perder"))
                .andExpect(redirectedUrl("/edicoes/nova")).andReturn();
        assertThat(((EditionForm)invalid.getFlashMap().get("editionForm")).getTitle()).isEqualTo("Rascunho");
        assertThat(invalid.getFlashMap().get("amazonMessage").toString()).doesNotContain("127.0.0.1");
        for(String route:List.of("buscar","aplicar")) {
            var result=mvc.perform(post("/edicoes/amazon/"+route).with(auth).with(csrf()).param("amazonLink","https://www.amazon.com.br/dp/655448244X").param("referencePaidPrice","inválido").param("notes","Não perder"))
                    .andExpect(redirectedUrl("/edicoes/nova")).andReturn();
            assertThat(result.getFlashMap().containsKey("org.springframework.validation.BindingResult.editionForm")).isTrue();
            assertThat(((EditionForm)result.getFlashMap().get("editionForm")).getNotes()).isEqualTo("Não perder");
        }
        verifyNoInteractions(page,isbn);
    }
}
