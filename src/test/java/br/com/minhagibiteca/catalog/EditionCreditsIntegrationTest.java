package br.com.minhagibiteca.catalog;

import br.com.minhagibiteca.auth.application.UserPrincipal;
import br.com.minhagibiteca.auth.domain.*;
import br.com.minhagibiteca.auth.infrastructure.UserAccountRepository;
import br.com.minhagibiteca.catalog.application.*;
import br.com.minhagibiteca.reports.application.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Testcontainers
class EditionCreditsIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserAccountRepository users;
    @Autowired EditionAssociationService associations;
    @Autowired ReportService reports;
    UserAccount owner, other;
    UUID creator, foreignCreator;

    @BeforeEach void setup() {
        jdbc.execute("truncate users cascade");
        owner = users.save(UserAccount.create("credits@example.com", "hash", UserRole.USER));
        other = users.save(UserAccount.create("foreign-credits@example.com", "hash", UserRole.USER));
        creator = UUID.randomUUID();
        foreignCreator = UUID.randomUUID();
        jdbc.update("insert into creators(id, owner_user_id, name, normalized_name) values (?, ?, 'Ana', 'ana'), (?, ?, 'Outra pessoa', 'outra pessoa')",
                creator, owner.getId(), foreignCreator, other.getId());
        jdbc.update("insert into creator_functions(creator_id, function) values (?, 'ROTEIRISTA')", creator);
    }

    UUID create(String... credits) throws Exception {
        var response = mvc.perform(post("/edicoes").with(user(UserPrincipal.from(owner))).with(csrf())
                .param("title", "HQ com créditos").param("creatorIds", creator.toString())
                .param("_creatorCredits", "on").param("creatorCredits", credits))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse();
        return UUID.fromString(response.getRedirectedUrl().substring("/edicoes/".length()));
    }

    List<String> roles(UUID edition) {
        return jdbc.queryForList("select creator_role from edition_creators where edition_id = ? order by creator_role", String.class, edition);
    }

    @Test void savesMultipleRolesForOneCreatorAndDisplaysThemInDetailsAndReports() throws Exception {
        UUID edition = create(creator + ":DESENHISTA", creator + ":CAPISTA", creator + ":CAPISTA");
        assertThat(roles(edition)).containsExactly("CAPISTA", "DESENHISTA");
        assertThat(associations.creatorIds(edition)).containsExactly(creator);
        assertThat(jdbc.queryForList("select function from creator_functions where creator_id = ?", String.class, creator))
                .containsExactly("ROTEIRISTA");
        var auth = user(UserPrincipal.from(owner));
        String detail = mvc.perform(get("/edicoes/" + edition).with(auth)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(detail).contains("Créditos nesta edição:", "Capista, Desenhista", "Roteirista");
        String edit = mvc.perform(get("/edicoes/" + edition + "/editar").with(auth)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(Jsoup.parse(edit).select("input[name=creatorCredits][checked]").eachAttr("value"))
                .containsExactlyInAnyOrder(creator + ":CAPISTA", creator + ":DESENHISTA");
        var filter = new ReportFilter("", null, null, null, null, null, "ALL", "ALL", null, null);
        String reportCredits = reports.preview(owner.getId(), filter, 0).entries().getContent().getFirst().creators();
        assertThat(reportCredits).contains("Ana (Roteirista)", "Capista", "Desenhista", "Créditos nesta edição");
        String csv = mvc.perform(get("/relatorios/csv").with(auth).param("type", "CATALOG"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv).contains("Créditos nesta edição", "Desenhista");
    }

    @Test void updatesClearsAndPreservesCreditsForExistingCallers() throws Exception {
        UUID edition = create(creator + ":ESCRITOR", creator + ":TRADUTOR");
        var auth = user(UserPrincipal.from(owner));
        mvc.perform(post("/edicoes/" + edition + "/editar").with(auth).with(csrf())
                .param("title", "Título atualizado").param("creatorIds", creator.toString()))
                .andExpect(status().is3xxRedirection());
        assertThat(roles(edition)).containsExactly("ESCRITOR", "TRADUTOR");
        mvc.perform(post("/edicoes/" + edition + "/editar").with(auth).with(csrf())
                .param("title", "Título atualizado").param("creatorIds", creator.toString())
                .param("creatorCredits", creator + ":COLORISTA").param("_creatorCredits", "on"))
                .andExpect(status().is3xxRedirection());
        assertThat(roles(edition)).containsExactly("COLORISTA");
        mvc.perform(post("/edicoes/" + edition + "/editar").with(auth).with(csrf())
                .param("title", "Título atualizado").param("creatorIds", creator.toString()).param("_creatorCredits", "on"))
                .andExpect(status().is3xxRedirection());
        assertThat(roles(edition)).containsExactly("NAO_INFORMADO");
        mvc.perform(post("/edicoes/" + edition + "/editar").with(auth).with(csrf())
                .param("title", "Sem criadores").param("_creatorIds", "on").param("_creatorCredits", "on"))
                .andExpect(status().is3xxRedirection());
        assertThat(roles(edition)).isEmpty();
    }

    @Test void rejectsUnknownRolesAndUnselectedPeopleWithoutSavingPartialChanges() throws Exception {
        UUID edition = create(creator + ":CAPISTA");
        var auth = user(UserPrincipal.from(owner));
        for (String token : List.of(creator + ":ADMIN", foreignCreator + ":DESENHISTA", "inválido")) {
            mvc.perform(post("/edicoes/" + edition + "/editar").with(auth).with(csrf())
                    .param("title", "Não deve persistir").param("creatorIds", creator.toString()).param("creatorCredits", token))
                    .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("editionForm", "creatorCredits"));
            assertThat(roles(edition)).containsExactly("CAPISTA");
            assertThat(jdbc.queryForObject("select title from editions where id = ?", String.class, edition)).isEqualTo("HQ com créditos");
        }
        mvc.perform(post("/edicoes/" + edition + "/editar").with(user(UserPrincipal.from(other))).with(csrf())
                .param("title", "Acesso indevido").param("creatorIds", creator.toString())
                .param("creatorCredits", creator + ":DESENHISTA")).andExpect(status().isNotFound());
        mvc.perform(post("/edicoes").with(auth).param("title", "Sem CSRF")).andExpect(status().isForbidden());
    }

    @Test void validationErrorsPreserveCheckedCredits() throws Exception {
        String html = mvc.perform(post("/edicoes").with(user(UserPrincipal.from(owner))).with(csrf())
                .param("creatorIds", creator.toString()).param("creatorCredits", creator + ":CAPISTA"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("editionForm", "title"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(Jsoup.parse(html).select("input[name=creatorCredits][checked]").eachAttr("value"))
                .containsExactly(creator + ":CAPISTA");
    }

    @Test void upgradesLegacyUnspecifiedRolesAndPreservesKnownCredits() {
        var before = Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas("credits_upgrade").defaultSchema("credits_upgrade").target("26").load();
        before.migrate();
        String separator = POSTGRES.getJdbcUrl().contains("?") ? "&" : "?";
        var source = new DriverManagerDataSource(POSTGRES.getJdbcUrl() + separator + "currentSchema=credits_upgrade",
                POSTGRES.getUsername(), POSTGRES.getPassword());
        var upgrade = new JdbcTemplate(source);
        UUID user = UUID.randomUUID(), edition = UUID.randomUUID(), person = UUID.randomUUID();
        upgrade.update("insert into users(id,email,password_hash,role) values (?, 'upgrade@example.com', 'hash', 'USER')", user);
        upgrade.update("insert into editions(id,owner_user_id,title,normalized_title) values (?,?,'HQ','hq')", edition, user);
        upgrade.update("insert into creators(id,owner_user_id,name,normalized_name) values (?,?,'Ana','ana')", person, user);
        upgrade.update("insert into edition_creators(edition_id,creator_id,creator_role) values (?,?,'OUTRO'), (?,?,'ROTEIRISTA')",
                edition, person, edition, person);
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas("credits_upgrade").defaultSchema("credits_upgrade").load().migrate();
        assertThat(upgrade.queryForList("select creator_role from edition_creators order by creator_role", String.class))
                .containsExactly("NAO_INFORMADO", "ROTEIRISTA");
    }
}
