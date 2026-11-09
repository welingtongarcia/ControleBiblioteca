package br.com.minhagibiteca.catalog;

import br.com.minhagibiteca.auth.application.UserPrincipal;
import br.com.minhagibiteca.auth.domain.*;
import br.com.minhagibiteca.auth.infrastructure.UserAccountRepository;
import br.com.minhagibiteca.catalog.application.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CatalogOrganizationIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserAccountRepository users;
    @Autowired CatalogOrganizationService organization;
    UserAccount owner, other;

    @BeforeEach void setup() {
        jdbc.execute("truncate users cascade");
        owner = users.save(UserAccount.create("organizer@example.com", "hash", UserRole.USER));
        other = users.save(UserAccount.create("other-organizer@example.com", "hash", UserRole.USER));
    }

    UUID edition(UUID user, String title, String isbn) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into editions(id, owner_user_id, title, normalized_title, isbn)
                values (?, ?, ?, lower(?), ?)
                """, id, user, title, title, isbn);
        return id;
    }

    String catalog(String reading, String missing) throws Exception {
        return mvc.perform(get("/edicoes").with(user(UserPrincipal.from(owner)))
                .param("reading", reading).param("missing", missing)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test void pendingAndReadingFiltersUseOnlyOwnedEditionsAndKeepPagination() throws Exception {
        UUID first = edition(owner.getId(), "Cadastro parcial", null);
        UUID second = edition(owner.getId(), "Cadastro completo", null);
        edition(other.getId(), "Informação privada", null);
        UUID publisher = UUID.randomUUID(), series = UUID.randomUUID();
        jdbc.update("insert into publishers(id, owner_user_id, name, normalized_name) values (?, ?, 'Editora', 'editora')", publisher, owner.getId());
        jdbc.update("insert into series(id, owner_user_id, name, normalized_name) values (?, ?, 'Série', 'série')", series, owner.getId());
        jdbc.update("""
                update editions set cover_file_name = 'cover.png', total_pages = 100, pages_read = 100, is_read = true,
                    publisher_id = ?, series_id = ?, reference_paid_price = 0 where id = ?
                """, publisher, series, second);
        jdbc.update("update editions set total_pages = 200, pages_read = 20 where id = ?", first);
        var pending = organization.pending(owner.getId());
        assertThat(pending).extracting(CatalogOrganizationService.Pending::count).containsExactly(1L, 0L, 1L, 1L, 1L);
        String progress = catalog("IN_PROGRESS", "COVER");
        assertThat(progress).contains("Cadastro parcial").doesNotContain("Cadastro completo", "Informação privada");
        assertThat(catalog("READ", "NONE")).contains("Cadastro completo").doesNotContain("Cadastro parcial");
        assertThat(catalog("UNREAD", "PAID")).contains("Cadastro parcial").doesNotContain("Cadastro completo");
        for (int i = 0; i < 21; i++) edition(owner.getId(), "Pendente " + i, null);
        var html = Jsoup.parse(catalog("UNREAD", "COVER"));
        assertThat(html.select(".pagination a").eachAttr("href")).allMatch(url ->
                url.contains("reading=UNREAD") && url.contains("missing=COVER"));
        mvc.perform(get("/edicoes/pendencias").with(user(UserPrincipal.from(owner))))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Pendências do catálogo")));
        mvc.perform(get("/edicoes/pendencias")).andExpect(status().is3xxRedirection());
    }

    @Test void savedFiltersArePrivateDynamicAndRequireCsrf() throws Exception {
        var auth = user(UserPrincipal.from(owner));
        var request = post("/edicoes/filtros").param("name", "Minhas leituras").param("reading", "UNREAD").param("missing", "COVER");
        mvc.perform(request.with(auth)).andExpect(status().isForbidden());
        mvc.perform(post("/edicoes/filtros").with(auth).with(csrf())
                .param("name", "Minhas leituras").param("reading", "UNREAD").param("missing", "COVER"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("successMessage"));
        var saved = organization.saved(owner.getId()).getFirst();
        assertThat(organization.saved(other.getId())).isEmpty();
        String url = br.com.minhagibiteca.catalog.presentation.CatalogLinks.url(saved.filter(), saved.order());
        edition(owner.getId(), "Edição adicionada depois", null);
        String html = mvc.perform(get(url).with(auth)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("Edição adicionada depois", "Minhas leituras");
        mvc.perform(post("/edicoes/filtros").with(auth).with(csrf())
                .param("name", "minhas leituras")).andExpect(flash().attributeExists("errorMessage"));
        mvc.perform(post("/edicoes/filtros/" + saved.id() + "/excluir")
                .with(user(UserPrincipal.from(other))).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/edicoes/filtros/" + saved.id() + "/excluir").with(auth).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(organization.saved(owner.getId())).isEmpty();
    }

    @Test void savedFilterCannotReferenceForeignCatalogAndMarksDeletedReferencesUnavailable() throws Exception {
        UUID publisher = UUID.randomUUID();
        jdbc.update("insert into publishers(id, owner_user_id, name, normalized_name) values (?, ?, 'Privada', 'privada')", publisher, other.getId());
        mvc.perform(post("/edicoes/filtros").with(user(UserPrincipal.from(owner))).with(csrf())
                .param("name", "Indevido").param("publisherId", publisher.toString())).andExpect(status().isNotFound());
        assertThat(organization.saved(owner.getId())).isEmpty();
        organization.save(other.getId(), "Editora", new CatalogFilter("", publisher, null, "", null), CatalogOrder.TITLE_ASC);
        jdbc.update("delete from publishers where id = ?", publisher);
        assertThat(organization.saved(other.getId()).getFirst().available()).isFalse();
    }

    @Test void duplicateWarningRecognizesBothIsbnFormsAndPreservesDraftUntilConfirmation() throws Exception {
        UUID existing = edition(owner.getId(), "Thorgal existente", "655448244X");
        edition(other.getId(), "Título privado", "9786554482448");
        var auth = user(UserPrincipal.from(owner));
        var warning = mvc.perform(post("/edicoes").with(auth).with(csrf()).param("title", "Meu novo volume")
                .param("isbn", "978-6554482448").param("referencePaidPrice", "35,90").param("notes", "Preservar minhas notas"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(warning).contains("Confira possíveis edições repetidas", "Thorgal existente", "Mesmo ISBN", "Preservar minhas notas")
                .doesNotContain("Título privado");
        assertThat(jdbc.queryForObject("select count(*) from editions where owner_user_id = ?", Long.class, owner.getId())).isEqualTo(1);
        assertThat(Jsoup.parse(warning).selectFirst("[name=referencePaidPrice]").attr("value")).isEqualTo("35,90");
        mvc.perform(post("/edicoes").with(auth).with(csrf()).param("title", "Meu novo volume").param("isbn", "978-6554482448")
                .param("referencePaidPrice", "35,90").param("confirmDuplicate", "true")).andExpect(status().is3xxRedirection());
        assertThat(jdbc.queryForObject("select count(*) from editions where owner_user_id = ?", Long.class, owner.getId())).isEqualTo(2);
        assertThat(jdbc.queryForObject("select title from editions where id = ?", String.class, existing)).isEqualTo("Thorgal existente");
    }

    @Test void seriesWarningDistinguishesNumbersAndVariants() throws Exception {
        UUID series = UUID.randomUUID();
        jdbc.update("insert into series(id, owner_user_id, name, normalized_name) values (?, ?, 'Batman', 'batman')", series, owner.getId());
        UUID existing = edition(owner.getId(), "Batman", null);
        jdbc.update("update editions set series_id = ?, editorial_number = '1' where id = ?", series, existing);
        var auth = user(UserPrincipal.from(owner));
        mvc.perform(post("/edicoes").with(auth).with(csrf()).param("title", "Batman")
                .param("seriesId", series.toString()).param("editorialNumber", "1"))
                .andExpect(status().isOk()).andExpect(model().attributeExists("duplicateCandidates"));
        mvc.perform(post("/edicoes").with(auth).with(csrf()).param("title", "Batman")
                .param("seriesId", series.toString()).param("editorialNumber", "1").param("variant", "Capa B"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/edicoes").with(auth).with(csrf()).param("title", "Batman")
                .param("seriesId", series.toString()).param("editorialNumber", "2"))
                .andExpect(status().is3xxRedirection());
    }
}
