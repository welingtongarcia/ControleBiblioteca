package br.com.minhagibiteca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import br.com.minhagibiteca.catalog.domain.ComicSeries;
import br.com.minhagibiteca.catalog.infrastructure.PublisherRepository;
import br.com.minhagibiteca.catalog.infrastructure.ComicSeriesRepository;
import br.com.minhagibiteca.catalog.infrastructure.EditionRepository;
import br.com.minhagibiteca.catalog.infrastructure.WorkRepository;
import br.com.minhagibiteca.catalog.infrastructure.CreatorRepository;
import br.com.minhagibiteca.catalog.application.CreatorService;
import br.com.minhagibiteca.catalog.application.EditionAssociationService;
import br.com.minhagibiteca.catalog.application.EditionCoverService;
import br.com.minhagibiteca.catalog.application.EditionService;
import br.com.minhagibiteca.catalog.application.WorkService;
import br.com.minhagibiteca.catalog.domain.Edition;
import java.sql.Connection;
import java.util.List;
import java.util.Base64;
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
import org.springframework.mock.web.MockMultipartFile;
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
    private ComicSeriesRepository comicSeriesRepository;

    @Autowired
    private EditionRepository editionRepository;

    @Autowired
    private WorkRepository workRepository;

    @Autowired
    private CreatorRepository creatorRepository;

    @Autowired
    private EditionService editionService;

    @Autowired
    private WorkService workService;

    @Autowired
    private CreatorService creatorService;

    @Autowired
    private EditionAssociationService editionAssociationService;

    @Autowired
    private EditionCoverService editionCoverService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private BootstrapAdministratorService bootstrapAdministratorService;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.cover-storage-path", () -> "target/test-covers");
    }

    @BeforeEach
    void clearUsers() {
        editionRepository.deleteAll();
        workRepository.deleteAll();
        creatorRepository.deleteAll();
        comicSeriesRepository.deleteAll();
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

    @Test
    void managesSeriesOnlyWithPublishersFromTheSameOwner() throws Exception {
        UserAccount owner = userAccountRepository.save(UserAccount.create(
                "dono@example.com", passwordEncoder.encode("senha-segura-123"), UserRole.USER));
        UserAccount otherUser = userAccountRepository.save(UserAccount.create(
                "outro@example.com", passwordEncoder.encode("senha-segura-456"), UserRole.USER));
        Publisher publisher = publisherRepository.save(Publisher.create(owner.getId(), "DC Comics", "US", null));

        mockMvc.perform(post("/series")
                        .with(SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner)))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("name", "Batman")
                        .param("publisherId", publisher.getId().toString())
                        .param("startYear", "1940")
                        .param("status", "ONGOING"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/series"));

        ComicSeries series = comicSeriesRepository.findByOwnerUserIdAndNormalizedNameContaining(
                owner.getId(), "batman", org.springframework.data.domain.Pageable.unpaged()).getContent().getFirst();
        mockMvc.perform(get("/series/{id}/editar", series.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(otherUser))))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/series")
                        .with(SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(otherUser)))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("name", "Série inválida")
                        .param("publisherId", publisher.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void persistsTextualEditionNumberAndAssociatesOnlyOwnedWorksAndCreators() {
        UserAccount owner = userAccountRepository.save(UserAccount.create(
                "dono@example.com", passwordEncoder.encode("senha-segura-123"), UserRole.USER));
        UserAccount otherUser = userAccountRepository.save(UserAccount.create(
                "outro@example.com", passwordEncoder.encode("senha-segura-456"), UserRole.USER));
        var work = workService.create(owner.getId(), new WorkService.Command("A Queda do Morcego", 1993, null));
        var creator = creatorService.create(owner.getId(), new CreatorService.Command("Doug Moench", null));
        var otherWork = workService.create(otherUser.getId(), new WorkService.Command("Obra alheia", null, null));
        Edition edition = editionService.create(owner.getId(), new EditionService.Command(
                null, null, "Batman", "1A", null, "", null, null, null));

        editionAssociationService.replace(edition.getId(), owner.getId(), List.of(work.getId()), List.of(creator.getId()));

        assertThat(edition.getEditorialNumber()).isEqualTo("1A");
        assertThat(edition.getIsbn()).isNull();
        assertThat(editionAssociationService.works(edition.getId(), owner.getId())).extracting("id").containsExactly(work.getId());
        assertThat(editionAssociationService.creators(edition.getId(), owner.getId())).extracting("id").containsExactly(creator.getId());
        assertThatThrownBy(() -> editionAssociationService.replace(edition.getId(), owner.getId(), List.of(otherWork.getId()), List.of()))
                .isInstanceOf(br.com.minhagibiteca.shared.application.ResourceNotFoundException.class);
    }

    @Test
    void storesAValidEditionCoverAndRejectsAnInvalidImage() {
        UserAccount owner = userAccountRepository.save(UserAccount.create(
                "dono@example.com", passwordEncoder.encode("senha-segura-123"), UserRole.USER));
        Edition edition = editionService.create(owner.getId(), new EditionService.Command(
                null, null, "Capa de teste", null, null, null, null, null, null));
        byte[] png = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");

        editionCoverService.replace(edition.getId(), owner.getId(), new MockMultipartFile("cover", "capa.png", "image/png", png));

        assertThat(editionCoverService.load(edition.getId(), owner.getId()).resource().exists()).isTrue();
        assertThatThrownBy(() -> editionCoverService.replace(edition.getId(), owner.getId(),
                new MockMultipartFile("cover", "texto.png", "image/png", "não é imagem".getBytes())))
                .isInstanceOf(EditionCoverService.InvalidCoverException.class);
    }

    @Test
    void createsAndEditsDigitalEditionWithExactPricesAndOwnerIsolation() throws Exception {
        UserAccount owner = userAccountRepository.save(UserAccount.create(
                "precos@example.com", passwordEncoder.encode("senha-segura-123"), UserRole.USER));
        UserAccount other = userAccountRepository.save(UserAccount.create(
                "outro@example.com", passwordEncoder.encode("senha-segura-123"), UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        mockMvc.perform(post("/edicoes").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("title", "Quadrinho digital").param("mediaType", "DIGITAL")
                        .param("coverPrice", "29,90").param("referencePaidPrice", "19.50"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/edicoes"));
        Edition edition = editionRepository.findAll().getFirst();
        assertThat(edition.getMediaType()).isEqualTo(br.com.minhagibiteca.catalog.domain.MediaType.DIGITAL);
        assertThat(edition.getCoverPrice()).isEqualByComparingTo("29.90");
        assertThat(edition.getReferencePaidPrice()).isEqualByComparingTo("19.50");
        mockMvc.perform(get("/edicoes/{id}", edition.getId()).with(auth))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Digital")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("29,90")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("19,50")));
        mockMvc.perform(get("/edicoes/{id}/editar", edition.getId()).with(auth))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("29,90")));
        mockMvc.perform(get("/edicoes").with(auth)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Digital")));
        mockMvc.perform(post("/edicoes/{id}/editar", edition.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(other)))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()).param("title", "Intrusão")
                        .param("coverPrice", "10,00"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/edicoes/{id}/editar", edition.getId()).with(auth)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()).param("title", "Quadrinho físico")
                        .param("mediaType", "PHYSICAL").param("coverPrice", "").param("referencePaidPrice", "0"))
                .andExpect(redirectedUrl("/edicoes/" + edition.getId()));
        Edition updated = editionRepository.findById(edition.getId()).orElseThrow();
        assertThat(updated.getCoverPrice()).isNull();
        assertThat(updated.getReferencePaidPrice()).isEqualByComparingTo("0");
        assertThat(updated.getMediaType()).isEqualTo(br.com.minhagibiteca.catalog.domain.MediaType.PHYSICAL);
        mockMvc.perform(get("/edicoes/{id}", edition.getId()).with(auth))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("0,00")));
    }

    @Test
    void rejectsInvalidPricesWithoutSavingOrLosingFormValues() throws Exception {
        UserAccount owner = userAccountRepository.save(UserAccount.create(
                "validacao@example.com", passwordEncoder.encode("senha-segura-123"), UserRole.USER));
        for (String value : List.of("-1,00", "1,234", "10000000000", "texto", "1.234,56")) {
            mockMvc.perform(post("/edicoes")
                            .with(SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner)))
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .param("title", "Preço inválido").param("mediaType", "DIGITAL")
                            .param("coverPrice", value).param("referencePaidPrice", value))
                    .andExpect(status().isOk()).andExpect(view().name("edicoes/form"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model()
                            .attributeHasFieldErrors("editionForm", "coverPrice", "referencePaidPrice"));
        }
        assertThat(editionRepository.count()).isZero();
        mockMvc.perform(get("/edicoes/nova")
                        .with(SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner))))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Preço de capa")));
    }

    @Test
    void upgradesExistingEditionWithoutInventingPricesOrMedia() throws Exception {
        String schema = "pricing_upgrade_test";
        org.flywaydb.core.Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(schema).defaultSchema(schema).target("7").load().migrate();
        try (Connection connection = dataSource.getConnection(); var sql = connection.createStatement()) {
            sql.executeUpdate("insert into pricing_upgrade_test.users (id, email, password_hash, role) "
                    + "values ('00000000-0000-0000-0000-000000000001', 'migration@example.com', 'test', 'USER')");
            sql.executeUpdate("insert into pricing_upgrade_test.editions (id, owner_user_id, title, normalized_title, editorial_number) "
                    + "values ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Anterior', 'anterior', '1A')");
        }
        org.flywaydb.core.Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(schema).defaultSchema(schema).load().migrate();
        try (Connection connection = dataSource.getConnection(); var sql = connection.createStatement();
                var row = sql.executeQuery("select title, editorial_number, media_type, cover_price, reference_paid_price from pricing_upgrade_test.editions")) {
            assertThat(row.next()).isTrue();
            assertThat(row.getString("title")).isEqualTo("Anterior");
            assertThat(row.getString("editorial_number")).isEqualTo("1A");
            assertThat(row.getString("media_type")).isNull();
            assertThat(row.getBigDecimal("cover_price")).isNull();
            assertThat(row.getBigDecimal("reference_paid_price")).isNull();
        }
    }

    @Test
    void filtersCatalogInDatabaseAndPreservesParametersAcrossPages() throws Exception {
        UserAccount owner = userAccountRepository.save(UserAccount.create(
                "catalogo@example.com", passwordEncoder.encode("senha-segura-123"), UserRole.USER));
        UserAccount other = userAccountRepository.save(UserAccount.create(
                "alheio@example.com", passwordEncoder.encode("senha-segura-123"), UserRole.USER));
        var publisher = publisherRepository.save(Publisher.create(owner.getId(), "Editora do catálogo", "BR", null));
        var series = comicSeriesRepository.save(ComicSeries.create(owner.getId(), publisher.getId(), "Série do catálogo", null, null, null, null));
        for (int i = 0; i < 25; i++) {
            editionService.create(owner.getId(), new EditionService.Command(publisher.getId(), series.getId(),
                    "HQ " + String.format("%02d", i), "1A", null, "978TESTE", 2026, "Encadernado", null,
                    br.com.minhagibiteca.catalog.domain.MediaType.DIGITAL, null, null));
        }
        editionService.create(owner.getId(), new EditionService.Command(publisher.getId(), series.getId(),
                "HQ físico", null, null, null, null, "Encadernado", null,
                br.com.minhagibiteca.catalog.domain.MediaType.PHYSICAL, null, null));
        editionService.create(other.getId(), new EditionService.Command(null, null, "HQ alheia", null, null, null, null, "Formato privado", null));
        var filter = new br.com.minhagibiteca.catalog.application.CatalogFilter("hq", publisher.getId(), series.getId(), "encadernado",
                br.com.minhagibiteca.catalog.domain.MediaType.DIGITAL);
        var first = editionService.search(owner.getId(), filter, br.com.minhagibiteca.catalog.application.CatalogOrder.TITLE_DESC, 0);
        var second = editionService.search(owner.getId(), filter, br.com.minhagibiteca.catalog.application.CatalogOrder.TITLE_DESC, 1);
        assertThat(first.getTotalElements()).isEqualTo(25);
        assertThat(first.getContent()).hasSize(20);
        assertThat(first.getContent().getFirst().title()).isEqualTo("HQ 24");
        assertThat(second.getContent()).hasSize(5);
        assertThat(second.getContent().getFirst().title()).isEqualTo("HQ 04");
        assertThat(editionService.search(owner.getId(), filter, br.com.minhagibiteca.catalog.application.CatalogOrder.TITLE_ASC, -1).getNumber()).isZero();
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        mockMvc.perform(get("/edicoes").with(auth).param("q", "hq").param("publisherId", publisher.getId().toString())
                        .param("seriesId", series.getId().toString()).param("format", "Encadernado")
                        .param("mediaType", "DIGITAL").param("order", "TITLE_DESC"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("publisherId=" + publisher.getId())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("seriesId=" + series.getId())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("mediaType=DIGITAL")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("format=Encadernado")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("order=TITLE_DESC")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("page=1")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Formato privado"))));
        mockMvc.perform(get("/edicoes").with(auth).param("order", "qualquer-coluna"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model().attribute("selectedOrder",
                        br.com.minhagibiteca.catalog.application.CatalogOrder.TITLE_ASC));
        mockMvc.perform(get("/edicoes").with(auth).param("mediaType", "INVALIDO")).andExpect(status().isBadRequest())
                .andExpect(view().name("errors/400"));
        mockMvc.perform(get("/edicoes").with(auth).param("publisherId", "inválido")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/edicoes").with(auth).param("page", "inválido")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/edicoes").with(SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(other)))
                        .param("publisherId", publisher.getId().toString())).andExpect(status().isNotFound());
        mockMvc.perform(get("/edicoes").with(SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(other)))
                        .param("seriesId", series.getId().toString())).andExpect(status().isNotFound());
    }

    @Test
    void searchesLiteralWildcardsIsbnAndEditorialNumberAndRendersBibliographicDetail() throws Exception {
        UserAccount owner = userAccountRepository.save(UserAccount.create(
                "busca@example.com", passwordEncoder.encode("senha-segura-123"), UserRole.USER));
        var publisher = publisherRepository.save(Publisher.create(owner.getId(), "Editora brasileira", "BR", null));
        var series = comicSeriesRepository.save(ComicSeries.create(owner.getId(), publisher.getId(), "Série especial", null, null, null, null));
        var edition = editionService.create(owner.getId(), new EditionService.Command(publisher.getId(), series.getId(),
                "HQ 100%_especial", "1A", null, "978-123", null, null, null));
        editionService.create(owner.getId(), new EditionService.Command(null, null, "HQ 100XXespecial", null, null, null, null, null, null));
        for (String term : List.of("%_", "978-123", "1a")) {
            var result = editionService.search(owner.getId(), new br.com.minhagibiteca.catalog.application.CatalogFilter(term, null, null, null, null),
                    br.com.minhagibiteca.catalog.application.CatalogOrder.TITLE_ASC, 0);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().getFirst().id()).isEqualTo(edition.getId());
        }
        var work = workService.create(owner.getId(), new WorkService.Command("História associada", null, null));
        var creator = creatorService.create(owner.getId(), new CreatorService.Command("Criador associado", null));
        editionAssociationService.replace(edition.getId(), owner.getId(), List.of(work.getId()), List.of(creator.getId()));
        mockMvc.perform(get("/edicoes/{id}", edition.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner))))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Editora brasileira")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Série especial")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("História associada")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Criador associado")));
    }

    @Test
    void keepsDuplicateTitlesStableAcrossPagesAndOrdersByRegistrationDate() throws Exception {
        UserAccount owner = userAccountRepository.save(UserAccount.create(
                "ordem@example.com", passwordEncoder.encode("senha-segura-123"), UserRole.USER));
        var ids = new java.util.ArrayList<java.util.UUID>();
        for (int i = 0; i < 25; i++) {
            ids.add(editionService.create(owner.getId(), new EditionService.Command(null, null,
                    "Mesmo título", null, null, null, null, null, null)).getId());
        }
        var filter = new br.com.minhagibiteca.catalog.application.CatalogFilter("", null, null, null, null);
        var first = editionService.search(owner.getId(), filter, br.com.minhagibiteca.catalog.application.CatalogOrder.TITLE_ASC, 0);
        var second = editionService.search(owner.getId(), filter, br.com.minhagibiteca.catalog.application.CatalogOrder.TITLE_ASC, 1);
        var actual = new java.util.ArrayList<java.util.UUID>();
        first.forEach(e -> actual.add(e.id())); second.forEach(e -> actual.add(e.id()));
        ids.sort(java.util.Comparator.comparing(java.util.UUID::toString));
        assertThat(actual).containsExactlyElementsOf(ids);
        try (Connection connection = dataSource.getConnection(); var sql = connection.prepareStatement(
                "update editions set created_at = ? where id = ?")) {
            sql.setObject(1, java.time.OffsetDateTime.parse("2000-01-01T00:00:00Z")); sql.setObject(2, ids.getFirst()); sql.executeUpdate();
            sql.setObject(1, java.time.OffsetDateTime.parse("2100-01-01T00:00:00Z")); sql.setObject(2, ids.getLast()); sql.executeUpdate();
        }
        assertThat(editionService.search(owner.getId(), filter, br.com.minhagibiteca.catalog.application.CatalogOrder.OLDEST, 0)
                .getContent().getFirst().id()).isEqualTo(ids.getFirst());
        assertThat(editionService.search(owner.getId(), filter, br.com.minhagibiteca.catalog.application.CatalogOrder.NEWEST, 0)
                .getContent().getFirst().id()).isEqualTo(ids.getLast());
    }
}
