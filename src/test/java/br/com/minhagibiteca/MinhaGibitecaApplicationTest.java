package br.com.minhagibiteca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import br.com.minhagibiteca.auth.domain.UserAccount;
import br.com.minhagibiteca.auth.domain.UserRole;
import br.com.minhagibiteca.auth.application.BootstrapAdministratorService;
import br.com.minhagibiteca.auth.application.UserPrincipal;
import br.com.minhagibiteca.auth.infrastructure.UserAccountRepository;
import br.com.minhagibiteca.catalog.domain.Publisher;
import br.com.minhagibiteca.catalog.infrastructure.PublisherRepository;
import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class MinhaGibitecaApplicationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PublisherRepository publisherRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private BootstrapAdministratorService bootstrapAdministratorService;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void clearUsers() {
        publisherRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void rendersLoginPageWithTheProjectStylesheet() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/css/main.css")));
    }

    @Test
    void appliesFlywayMigrationToAnEmptyPostgresDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                     "select metadata_value from application_metadata where metadata_key = 'schema_baseline'")) {
            var resultSet = statement.executeQuery();
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString(1)).isEqualTo("minha-gibiteca-phase-1");
        }
    }

    @Test
    void exposesReadinessHealthWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"status\":\"UP\"")));
    }

    @Test
    void authenticatesPersistedUserAndProtectsDashboard() throws Exception {
        userAccountRepository.save(UserAccount.create(
                "colecionador@example.com", passwordEncoder.encode("senha-segura-123"), UserRole.USER));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(post("/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("username", "COLECIONADOR@EXAMPLE.COM")
                        .param("password", "senha-segura-123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void logsOutOnlyWithCsrfProtection() throws Exception {
        mockMvc.perform(post("/logout").with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void bootstrapsOnlyTheFirstAdministrator() {
        assertThat(bootstrapAdministratorService.createFirstAdministrator(
                "admin@example.com", "senha-segura-123")).isTrue();
        assertThat(userAccountRepository.findByEmail("admin@example.com")).isPresent();
        assertThat(bootstrapAdministratorService.createFirstAdministrator(
                "outro@example.com", "senha-segura-456")).isFalse();
    }

    @Test
    void managesPublishersWithValidationAndOwnerIsolation() throws Exception {
        UserAccount owner = userAccountRepository.save(UserAccount.create(
                "dono@example.com", passwordEncoder.encode("senha-segura-123"), UserRole.USER));
        UserAccount otherUser = userAccountRepository.save(UserAccount.create(
                "outro@example.com", passwordEncoder.encode("senha-segura-456"), UserRole.USER));

        mockMvc.perform(post("/editoras")
                        .with(SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner)))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("name", "Marvel Comics")
                        .param("countryCode", "us")
                        .param("notes", "Editora americana"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/editoras"));

        Publisher publisher = publisherRepository.findByOwnerUserIdAndNormalizedNameContaining(
                        owner.getId(), "marvel", org.springframework.data.domain.Pageable.unpaged())
                .getContent().getFirst();
        assertThat(publisher.getCountryCode()).isEqualTo("US");

        mockMvc.perform(get("/editoras/{id}/editar", publisher.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(otherUser))))
                .andExpect(status().isNotFound())
                .andExpect(view().name("errors/404"));

        mockMvc.perform(post("/editoras")
                        .with(SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner)))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("editoras/form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Informe o nome da editora.")));
    }
}
