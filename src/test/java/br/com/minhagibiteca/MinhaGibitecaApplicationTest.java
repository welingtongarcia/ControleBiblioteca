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
import br.com.minhagibiteca.inventory.application.CopyService;
import br.com.minhagibiteca.inventory.domain.CopyCondition;
import br.com.minhagibiteca.inventory.infrastructure.PhysicalCopyRepository;
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

    @Autowired private CopyService copyService;
    @Autowired private PhysicalCopyRepository physicalCopyRepository;
    @Autowired private br.com.minhagibiteca.collection.application.ChecklistService checklistService;
    @Autowired private br.com.minhagibiteca.collection.infrastructure.ChecklistRepository checklistRepository;
    @Autowired private br.com.minhagibiteca.catalog.application.ComicSeriesService seriesService;

    @Autowired
    private WorkService workService;

    @Autowired
    private CreatorService creatorService;

    @Autowired
    private EditionAssociationService editionAssociationService;

    @Autowired
    private EditionCoverService editionCoverService;

    @Autowired
    private br.com.minhagibiteca.inventory.application.LocationService locationService;

    @Autowired
    private br.com.minhagibiteca.inventory.infrastructure.StorageLocationRepository storageLocationRepository;

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
        checklistRepository.deleteAllInBatch();
        physicalCopyRepository.deleteAllInBatch();
        editionRepository.deleteAll();
        workRepository.deleteAll();
        creatorRepository.deleteAll();
        storageLocationRepository.deleteAllInBatch();
        comicSeriesRepository.deleteAll();
        publisherRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void summarizesCollectionWithoutDuplicatingPricesAndRefreshesAfterChanges() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("summary@example.com", "hash", UserRole.USER));
        var other = userAccountRepository.save(UserAccount.create("other-summary@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var empty = (br.com.minhagibiteca.dashboard.application.CollectionSummary) mockMvc.perform(get("/dashboard").with(auth))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel().get("summary");
        assertThat(empty.copies()).isZero(); assertThat(empty.editions()).isZero();
        assertThat(empty.totalPaidLabel()).contains("0,00");
        var edition = Edition.create(owner.getId(), null, null, "Coleção resumo", "1", null, null, null, null, null);
        edition.updatePricingAndMedia(br.com.minhagibiteca.catalog.domain.MediaType.PHYSICAL, new java.math.BigDecimal("100.00"), new java.math.BigDecimal("999.00"));
        editionRepository.save(edition);
        var digital = Edition.create(owner.getId(), null, null, "Digital resumo", "2", null, null, null, null, null);
        digital.updatePricingAndMedia(br.com.minhagibiteca.catalog.domain.MediaType.DIGITAL, null, new java.math.BigDecimal("500.00"));
        editionRepository.save(digital);
        var foreign = editionRepository.save(Edition.create(other.getId(), null, null, "Privado", null, null, null, null, null, null));
        copyService.create(foreign.getId(), other.getId(), new CopyService.Command(null, CopyCondition.GOOD, null, null, null, new java.math.BigDecimal("1000.00"), null));
        var unknown = copyService.create(edition.getId(), owner.getId(), new CopyService.Command(null, CopyCondition.GOOD, null, null, null, null, null));
        var allUnknown = (br.com.minhagibiteca.dashboard.application.CollectionSummary) mockMvc.perform(get("/").with(auth))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Não informado")))
                .andReturn().getModelAndView().getModel().get("summary");
        assertThat(allUnknown.totalPaid()).isNull(); assertThat(allUnknown.unpricedCopies()).isEqualTo(1);
        var paid = copyService.create(edition.getId(), owner.getId(), new CopyService.Command(null, CopyCondition.GOOD, null, null, null, new java.math.BigDecimal("10.10"), null));
        copyService.create(edition.getId(), owner.getId(), new CopyService.Command(null, CopyCondition.GOOD, null, null, null, new java.math.BigDecimal("20.20"), null));
        copyService.create(edition.getId(), owner.getId(), new CopyService.Command(null, CopyCondition.GOOD, null, null, null, java.math.BigDecimal.ZERO, null));
        var result = mockMvc.perform(get("/dashboard").with(auth)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("30,30")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("pode estar incompleto"))).andReturn();
        var summary = (br.com.minhagibiteca.dashboard.application.CollectionSummary) result.getModelAndView().getModel().get("summary");
        assertThat(summary.editions()).isEqualTo(2); assertThat(summary.copies()).isEqualTo(4);
        assertThat(summary.ownedEditions()).isEqualTo(1); assertThat(summary.pricedCopies()).isEqualTo(3);
        assertThat(summary.totalPaid()).isEqualByComparingTo("30.30");
        var preview = java.nio.file.Path.of("target/ui-summary"); java.nio.file.Files.createDirectories(preview.resolve("css"));
        java.nio.file.Files.writeString(preview.resolve("index.html"), result.getResponse().getContentAsString());
        for (String css : List.of("main.css", "dashboard.css")) java.nio.file.Files.copy(java.nio.file.Path.of("src/main/resources/static/css", css), preview.resolve("css").resolve(css), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        copyService.update(paid, owner.getId(), new CopyService.Command(null, CopyCondition.GOOD, null, null, null, new java.math.BigDecimal("15.15"), null));
        copyService.delete(unknown, owner.getId());
        var refreshed = (br.com.minhagibiteca.dashboard.application.CollectionSummary) mockMvc.perform(get("/dashboard").with(auth))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel().get("summary");
        assertThat(refreshed.copies()).isEqualTo(3); assertThat(refreshed.unpricedCopies()).isZero();
        assertThat(refreshed.totalPaid()).isEqualByComparingTo("35.35");
        mockMvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection());
    }

    @Test
    void refreshesSummaryAfterEverySuccessfulCopyFormSubmission() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("summary-forms@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var edition = editionRepository.save(Edition.create(owner.getId(), null, null, "Resumo automático", null, null, null, null, null, null));
        for (int i = 1; i <= 2; i++) {
            mockMvc.perform(post("/edicoes/{id}/exemplares", edition.getId()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                            .param("condition", "GOOD").param("acquisitionPrice", "10,50"))
                    .andExpect(redirectedUrl("/edicoes/" + edition.getId()));
            var summary = (br.com.minhagibiteca.dashboard.application.CollectionSummary) mockMvc.perform(get("/dashboard").with(auth))
                    .andExpect(status().isOk()).andReturn().getModelAndView().getModel().get("summary");
            assertThat(summary.copies()).isEqualTo(i);
            assertThat(summary.pricedCopies()).isEqualTo(i);
            assertThat(summary.ownedEditions()).isEqualTo(1);
            assertThat(summary.totalPaid()).isEqualByComparingTo(new java.math.BigDecimal("10.50").multiply(java.math.BigDecimal.valueOf(i)));
        }
        var id = copyService.list(edition.getId(), owner.getId(), 0).getContent().getFirst().id();
        mockMvc.perform(post("/exemplares/{id}/editar", id).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("condition", "GOOD").param("acquisitionPrice", "25,75"))
                .andExpect(redirectedUrl("/edicoes/" + edition.getId()));
        var updated = (br.com.minhagibiteca.dashboard.application.CollectionSummary) mockMvc.perform(get("/").with(auth))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("36,25")))
                .andReturn().getModelAndView().getModel().get("summary");
        assertThat(updated.copies()).isEqualTo(2); assertThat(updated.totalPaid()).isEqualByComparingTo("36.25");
        mockMvc.perform(post("/exemplares/{id}/editar", id).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("condition", "GOOD").param("acquisitionPrice", "-1,00"))
                .andExpect(status().isOk()).andExpect(view().name("exemplares/form"));
        var unchanged = (br.com.minhagibiteca.dashboard.application.CollectionSummary) mockMvc.perform(get("/dashboard").with(auth))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel().get("summary");
        assertThat(unchanged).isEqualTo(updated);
        mockMvc.perform(post("/exemplares/{id}/editar", id).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("condition", "GOOD").param("acquisitionPrice", ""))
                .andExpect(redirectedUrl("/edicoes/" + edition.getId()));
        var removedPrice = (br.com.minhagibiteca.dashboard.application.CollectionSummary) mockMvc.perform(get("/dashboard").with(auth))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel().get("summary");
        assertThat(removedPrice.copies()).isEqualTo(2); assertThat(removedPrice.pricedCopies()).isEqualTo(1);
        assertThat(removedPrice.unpricedCopies()).isEqualTo(1); assertThat(removedPrice.totalPaid()).isEqualByComparingTo("10.50");
    }

    @Test
    void rendersImprovedHomeWithSearchModuleLinksAndSecureLogout() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("home-preview@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        String html = null;
        for (String path : List.of("/", "/dashboard")) {
            html = mockMvc.perform(get(path).with(auth)).andExpect(status().isOk()).andExpect(view().name("dashboard/index"))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("/css/dashboard.css")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Pular para o conteúdo")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Seu espaço de colecionador")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/logout\" method=\"post\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"q\"")))
                    .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            for (String destination : List.of("edicoes", "edicoes/nova", "series", "localizacoes", "editoras", "obras", "criadores"))
                assertThat(html).contains("href=\"/" + destination + "\"");
        }
        mockMvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(post("/logout").with(auth)).andExpect(status().isForbidden());
        mockMvc.perform(post("/logout").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(redirectedUrl("/login?logout"));
        // Prévia local do HTML realmente renderizado; só contém conteúdo estático e sessão de teste.
        var preview = java.nio.file.Path.of("target/ui-preview");
        java.nio.file.Files.createDirectories(preview.resolve("css"));
        java.nio.file.Files.writeString(preview.resolve("dashboard.html"), html, java.nio.charset.StandardCharsets.UTF_8);
        for (String css : List.of("main.css", "dashboard.css")) java.nio.file.Files.copy(java.nio.file.Path.of("src/main/resources/static/css", css), preview.resolve("css").resolve(css), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    void sharesNavigationAcrossPrivatePagesAndLinksChildrenToTheirOwnedParents() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("navigation@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var publisher = publisherRepository.save(Publisher.create(owner.getId(), "Editora de navegação", null, null));
        var series = comicSeriesRepository.save(ComicSeries.create(owner.getId(), publisher.getId(), "Série de navegação", null, null, null, null));
        var work = workService.create(owner.getId(), new WorkService.Command("Obra de navegação", null, null));
        var creator = creatorService.create(owner.getId(), new CreatorService.Command("Criador de navegação", null));
        var edition = editionService.create(owner.getId(), new EditionService.Command(publisher.getId(), series.getId(), "HQ de navegação", "1A", null, null, null, null, null));
        var location = locationService.create(owner.getId(), new br.com.minhagibiteca.inventory.application.LocationService.Command(null, br.com.minhagibiteca.inventory.domain.LocationLevel.BOX, "Caixa de navegação", null));
        var copy = copyService.create(edition.getId(), owner.getId(), new CopyService.Command(location, CopyCondition.GOOD, null, null, null, null, null));
        var item = checklistService.create(series.getId(), owner.getId(), new br.com.minhagibiteca.collection.application.ChecklistService.Command("1A", null, false, null));
        var paths = List.of("/dashboard", "/edicoes", "/edicoes/nova", "/edicoes/" + edition.getId(), "/edicoes/" + edition.getId() + "/editar",
                "/edicoes/" + edition.getId() + "/exemplares/novo", "/exemplares/" + copy + "/editar", "/exemplares/" + copy + "/excluir",
                "/series", "/series/nova", "/series/" + series.getId() + "/editar", "/series/" + series.getId() + "/checklist", "/series/" + series.getId() + "/faltantes",
                "/checklist/itens/" + item + "/editar", "/checklist/itens/" + item + "/excluir", "/editoras", "/editoras/nova", "/editoras/" + publisher.getId() + "/editar",
                "/obras", "/obras/nova", "/obras/" + work.getId() + "/editar", "/criadores", "/criadores/novo", "/criadores/" + creator.getId() + "/editar",
                "/localizacoes", "/localizacoes/nova", "/localizacoes/" + location + "/editar", "/localizacoes/" + location + "/excluir");
        var preview = java.nio.file.Path.of("target/ui-navigation");
        java.nio.file.Files.createDirectories(preview.resolve("css"));
        for (String css : List.of("main.css", "dashboard.css")) java.nio.file.Files.copy(java.nio.file.Path.of("src/main/resources/static/css", css), preview.resolve("css").resolve(css), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        for (String path : paths) {
            String html = mockMvc.perform(get(path).with(auth)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            assertThat(html).contains("Navegação principal", "Pular para o conteúdo", "action=\"/logout\" method=\"post\"", "name=\"_csrf\"");
            assertThat(html.split("class=\"nav-current\"", -1)).hasSize(2);
            for (String module : List.of("dashboard", "edicoes", "series", "editoras", "obras", "criadores", "localizacoes")) assertThat(html).contains("href=\"/" + module + "\"");
            if (!path.equals("/dashboard")) assertThat(html).contains("Você está aqui", "navigation-back");
            var directory = preview.resolve(path.substring(1)); java.nio.file.Files.createDirectories(directory);
            java.nio.file.Files.writeString(directory.resolve("index.html"), html, java.nio.charset.StandardCharsets.UTF_8);
        }
        mockMvc.perform(get("/edicoes/{id}/editar", edition.getId()).with(auth)).andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/edicoes/" + edition.getId() + "\">Cancelar")));
        mockMvc.perform(get("/exemplares/{id}/editar", copy).with(auth)).andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/edicoes/" + edition.getId() + "\">Cancelar")));
        mockMvc.perform(get("/login")).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Navegação principal"))));
    }

    @Test
    void preservesCatalogFiltersThroughValidationEditingAndListRedirects() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("context@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var edition = editionService.create(owner.getId(), new EditionService.Command(null, null, "HQ de contexto", null, null, null, null, null, null,
                br.com.minhagibiteca.catalog.domain.MediaType.PHYSICAL, null, null));
        String origin = "/edicoes?q=HQ&mediaType=PHYSICAL&order=TITLE_DESC&page=1";
        var listResult = mockMvc.perform(get("/edicoes").with(auth).param("q", "HQ").param("mediaType", "PHYSICAL").param("order", "TITLE_DESC").param("page", "1"))
                .andExpect(status().isOk()).andReturn();
        var nav = (br.com.minhagibiteca.shared.presentation.NavigationContext) listResult.getModelAndView().getModel().get("navigation");
        assertThat(nav.pageReturnUrl()).isEqualTo(origin);
        assertThat(listResult.getResponse().getContentAsString()).contains("returnTo=");
        mockMvc.perform(get("/edicoes/{id}", edition.getId()).with(auth).param("returnTo", origin)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/edicoes?q=HQ&amp;mediaType=PHYSICAL&amp;order=TITLE_DESC&amp;page=1\"")));
        var invalid = mockMvc.perform(post("/edicoes/{id}/editar", edition.getId()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("title", "").param("returnTo", origin))
                .andExpect(status().isOk()).andExpect(view().name("edicoes/form")).andReturn();
        assertThat(((br.com.minhagibiteca.shared.presentation.NavigationContext) invalid.getModelAndView().getModel().get("navigation")).returnUrl()).isEqualTo(origin);
        var saved = mockMvc.perform(post("/edicoes/{id}/editar", edition.getId()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("title", "HQ atualizada").param("mediaType", "PHYSICAL").param("returnTo", origin))
                .andExpect(status().is3xxRedirection()).andReturn();
        String redirect = saved.getResponse().getRedirectedUrl();
        assertThat(redirect).startsWith("/edicoes/" + edition.getId() + "?returnTo=");
        assertThat(java.net.URLDecoder.decode(redirect.split("returnTo=")[1], java.nio.charset.StandardCharsets.UTF_8)).isEqualTo(origin);
        mockMvc.perform(post("/edicoes").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("title", "Nova HQ").param("returnTo", origin)).andExpect(redirectedUrl(origin));
        String publisherOrigin = "/editoras?q=Editora&page=2";
        mockMvc.perform(post("/editoras").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("name", "Editora contextual").param("returnTo", publisherOrigin)).andExpect(redirectedUrl(publisherOrigin));
    }

    @Test
    void keepsChecklistMissingContextAndRejectsUnsafeReturnsWithoutBypassingOwnership() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("missing-context@example.com", "hash", UserRole.USER));
        var other = userAccountRepository.save(UserAccount.create("foreign-context@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var series = comicSeriesRepository.save(ComicSeries.create(owner.getId(), null, "Série contextual", null, null, null, null));
        var item = checklistService.create(series.getId(), owner.getId(), new br.com.minhagibiteca.collection.application.ChecklistService.Command("Especial", null, false, null));
        String origin = "/series/" + series.getId() + "/faltantes?page=1";
        mockMvc.perform(get("/checklist/itens/{id}/editar", item).with(auth).param("returnTo", origin)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"" + origin + "\">Cancelar")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Faltantes de Série contextual")));
        mockMvc.perform(post("/checklist/itens/{id}/editar", item).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("editorialNumber", "Especial").param("owned", "true").param("returnTo", origin)).andExpect(redirectedUrl(origin));
        for (String unsafe : List.of("https://example.com", "//example.com", "/series", "/edicoes/nova")) {
            mockMvc.perform(post("/edicoes").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("title", "Retorno seguro").param("returnTo", unsafe)).andExpect(redirectedUrl("/edicoes"));
        }
        mockMvc.perform(get("/checklist/itens/{id}/editar", item).with(SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(other))).param("returnTo", origin))
                .andExpect(status().isNotFound()).andExpect(view().name("errors/404"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Série contextual"))));
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

    @Test
    void managesLocationHierarchyAndRejectsCyclesDuplicatesAndInvalidLevels() {
        UserAccount owner = userAccountRepository.save(UserAccount.create(
                "local@example.com", passwordEncoder.encode("senha-segura-123"), UserRole.USER));
        var room = locationService.create(owner.getId(), new br.com.minhagibiteca.inventory.application.LocationService.Command(
                null, br.com.minhagibiteca.inventory.domain.LocationLevel.ROOM, "Escritório", null));
        var unit = locationService.create(owner.getId(), new br.com.minhagibiteca.inventory.application.LocationService.Command(
                room, br.com.minhagibiteca.inventory.domain.LocationLevel.SHELF_UNIT, "Estante A", null));
        var shelf = locationService.create(owner.getId(), new br.com.minhagibiteca.inventory.application.LocationService.Command(
                unit, br.com.minhagibiteca.inventory.domain.LocationLevel.SHELF, "Prateleira 1", null));
        var box = locationService.create(owner.getId(), new br.com.minhagibiteca.inventory.application.LocationService.Command(
                shelf, br.com.minhagibiteca.inventory.domain.LocationLevel.BOX, "Caixa 1", null));
        assertThat(locationService.find(box, owner.getId()).path()).isEqualTo("Escritório › Estante A › Prateleira 1 › Caixa 1");
        locationService.update(box, owner.getId(), new br.com.minhagibiteca.inventory.application.LocationService.Command(
                room, br.com.minhagibiteca.inventory.domain.LocationLevel.BOX, "Caixa movida", "HQs"));
        assertThat(locationService.find(box, owner.getId()).path()).isEqualTo("Escritório › Caixa movida");
        assertThatThrownBy(() -> locationService.update(room, owner.getId(), new br.com.minhagibiteca.inventory.application.LocationService.Command(
                room, br.com.minhagibiteca.inventory.domain.LocationLevel.ROOM, "Escritório", null))).hasMessageContaining("si mesma");
        assertThatThrownBy(() -> locationService.update(room, owner.getId(), new br.com.minhagibiteca.inventory.application.LocationService.Command(
                shelf, br.com.minhagibiteca.inventory.domain.LocationLevel.ROOM, "Escritório", null))).hasMessageContaining("si mesma");
        assertThatThrownBy(() -> locationService.create(owner.getId(), new br.com.minhagibiteca.inventory.application.LocationService.Command(
                box, br.com.minhagibiteca.inventory.domain.LocationLevel.SHELF, "Inválida", null))).hasMessageContaining("vir antes");
        assertThatThrownBy(() -> locationService.update(unit, owner.getId(), new br.com.minhagibiteca.inventory.application.LocationService.Command(
                room, br.com.minhagibiteca.inventory.domain.LocationLevel.BOX, "Estante A", null))).hasMessageContaining("internas existentes");
        assertThatThrownBy(() -> locationService.create(owner.getId(), new br.com.minhagibiteca.inventory.application.LocationService.Command(
                null, br.com.minhagibiteca.inventory.domain.LocationLevel.ROOM, " ESCRITÓRIO ", null))).hasMessageContaining("Já existe");
        assertThatThrownBy(() -> locationService.delete(room, owner.getId())).hasMessageContaining("contém outras");
        locationService.delete(box, owner.getId());
        assertThat(storageLocationRepository.findById(box)).isEmpty();
    }

    @Test
    void rendersLocationFormsChecksOwnerAndRequiresConfirmedCsrfDeletion() throws Exception {
        UserAccount owner = userAccountRepository.save(UserAccount.create(
                "mvc-local@example.com", passwordEncoder.encode("senha-segura-123"), UserRole.USER));
        UserAccount other = userAccountRepository.save(UserAccount.create(
                "outro-local@example.com", passwordEncoder.encode("senha-segura-123"), UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var root = locationService.create(owner.getId(), new br.com.minhagibiteca.inventory.application.LocationService.Command(
                null, br.com.minhagibiteca.inventory.domain.LocationLevel.ROOM, "Sala privada", null));
        mockMvc.perform(get("/localizacoes/nova").with(auth)).andExpect(status().isOk()).andExpect(view().name("localizacoes/form"));
        mockMvc.perform(post("/localizacoes").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("name", "Caixa azul").param("level", "BOX").param("parentId", root.toString()))
                .andExpect(redirectedUrl("/localizacoes"));
        var box = locationService.list(owner.getId(), "azul", 0).getContent().getFirst();
        mockMvc.perform(get("/localizacoes").with(auth)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sala privada › Caixa azul")));
        mockMvc.perform(get("/localizacoes/{id}/editar", box.id()).with(auth)).andExpect(status().isOk());
        mockMvc.perform(post("/localizacoes").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("name", ""))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Informe o nome da localização.")));
        mockMvc.perform(post("/localizacoes").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("name", "Caixa azul").param("level", "BOX").param("parentId", root.toString()))
                .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model()
                        .attributeHasFieldErrors("locationForm", "name"));
        var foreign = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(other));
        mockMvc.perform(get("/localizacoes/{id}/editar", box.id()).with(foreign)).andExpect(status().isNotFound());
        mockMvc.perform(post("/localizacoes").with(foreign).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("name", "Intrusão").param("level", "BOX").param("parentId", root.toString())).andExpect(status().isNotFound());
        mockMvc.perform(get("/localizacoes/nova").with(foreign)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Sala privada"))));
        mockMvc.perform(post("/localizacoes/{id}/excluir", box.id()).with(auth).param("confirmed", "true")).andExpect(status().isForbidden());
        mockMvc.perform(post("/localizacoes/{id}/excluir", box.id()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(redirectedUrl("/localizacoes/" + box.id() + "/excluir"));
        mockMvc.perform(get("/localizacoes/{id}/excluir", box.id()).with(auth)).andExpect(status().isOk()).andExpect(view().name("localizacoes/delete"));
        mockMvc.perform(post("/localizacoes/{id}/excluir", root).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("confirmed", "true"))
                .andExpect(redirectedUrl("/localizacoes")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attributeExists("errorMessage"));
        mockMvc.perform(post("/localizacoes/{id}/excluir", box.id()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("confirmed", "true"))
                .andExpect(redirectedUrl("/localizacoes"));
        assertThat(storageLocationRepository.findById(box.id())).isEmpty();
    }

    @Test
    void registersDuplicateCopiesWithIndependentPricesAndUpdatesTheirDetails() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("copias@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var edition = editionService.create(owner.getId(), new EditionService.Command(null, null, "HQ física", null, null, null, null, null, null,
                br.com.minhagibiteca.catalog.domain.MediaType.PHYSICAL, null, new java.math.BigDecimal("80.00")));
        var room = locationService.create(owner.getId(), new br.com.minhagibiteca.inventory.application.LocationService.Command(
                null, br.com.minhagibiteca.inventory.domain.LocationLevel.ROOM, "Sala HQ", null));
        var box = locationService.create(owner.getId(), new br.com.minhagibiteca.inventory.application.LocationService.Command(
                room, br.com.minhagibiteca.inventory.domain.LocationLevel.BOX, "Caixa HQ", null));
        mockMvc.perform(get("/edicoes/{id}/exemplares/novo", edition.getId()).with(auth)).andExpect(status().isOk())
                .andExpect(view().name("exemplares/form"));
        for (String price : List.of("29,90", "0,00")) {
            mockMvc.perform(post("/edicoes/{id}/exemplares", edition.getId()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                            .param("condition", "GOOD").param("storageLocationId", box.toString()).param("acquisitionPrice", price)
                            .param("acquisitionDate", "2020-03-15").param("acquisitionSource", "Sebo"))
                    .andExpect(redirectedUrl("/edicoes/" + edition.getId()));
        }
        var page = copyService.list(edition.getId(), owner.getId(), 0);
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().getFirst().acquisitionPrice()).isEqualByComparingTo("29.90");
        assertThat(page.getContent().getLast().acquisitionPrice()).isEqualByComparingTo("0.00");
        mockMvc.perform(get("/edicoes/{id}", edition.getId()).with(auth)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Duplicados são permitidos")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sala HQ › Caixa HQ")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("15/03/2020")));
        var id = page.getContent().getFirst().id();
        mockMvc.perform(get("/exemplares/{id}/editar", id).with(auth)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("29,90")));
        mockMvc.perform(post("/exemplares/{id}/editar", id).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("condition", "FAIR").param("notes", "<script>alert(1)</script>").param("editionId", java.util.UUID.randomUUID().toString()))
                .andExpect(redirectedUrl("/edicoes/" + edition.getId()));
        var updated = copyService.find(id, owner.getId());
        assertThat(updated.editionId()).isEqualTo(edition.getId());
        assertThat(updated.condition()).isEqualTo(CopyCondition.FAIR);
        assertThat(updated.storageLocationId()).isNull();
        assertThat(updated.acquisitionPrice()).isNull();
        mockMvc.perform(get("/edicoes/{id}", edition.getId()).with(auth)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;script&gt;")));
    }

    @Test
    void rejectsInvalidCopyValuesDigitalEditionsAndForeignReferences() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("valid-copias@example.com", "hash", UserRole.USER));
        var other = userAccountRepository.save(UserAccount.create("foreign-copias@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var foreign = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(other));
        var edition = editionService.create(owner.getId(), new EditionService.Command(null, null, "Suporte desconhecido", null, null, null, null, null, null));
        var digital = editionService.create(owner.getId(), new EditionService.Command(null, null, "HQ digital", null, null, null, null, null, null,
                br.com.minhagibiteca.catalog.domain.MediaType.DIGITAL, null, null));
        var location = locationService.create(other.getId(), new br.com.minhagibiteca.inventory.application.LocationService.Command(
                null, br.com.minhagibiteca.inventory.domain.LocationLevel.BOX, "Caixa alheia", null));
        for (String price : List.of("-1", "1,234", "10000000000", "texto", "1.234,56")) {
            mockMvc.perform(post("/edicoes/{id}/exemplares", edition.getId()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                            .param("condition", "GOOD").param("acquisitionPrice", price))
                    .andExpect(status().isOk()).andExpect(view().name("exemplares/form"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model().attributeHasFieldErrors("copyForm", "acquisitionPrice"));
        }
        for (String date : List.of("2999-01-01", "2026-99-99")) {
            mockMvc.perform(post("/edicoes/{id}/exemplares", edition.getId()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                            .param("condition", "GOOD").param("acquisitionDate", date)).andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model().attributeHasFieldErrors("copyForm", "acquisitionDate"));
        }
        mockMvc.perform(post("/edicoes/{id}/exemplares", edition.getId()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("condition", "INVALID"))
                .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model().attributeHasFieldErrors("copyForm", "condition"));
        mockMvc.perform(get("/edicoes/{id}/exemplares/novo", digital.getId()).with(auth)).andExpect(redirectedUrl("/edicoes/" + digital.getId()));
        mockMvc.perform(post("/edicoes/{id}/exemplares", digital.getId()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(redirectedUrl("/edicoes/" + digital.getId()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attributeExists("errorMessage"));
        mockMvc.perform(get("/edicoes/{id}", digital.getId()).with(auth)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Novo exemplar"))));
        mockMvc.perform(post("/edicoes/{id}/exemplares", edition.getId()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("storageLocationId", location.toString())).andExpect(status().isNotFound());
        mockMvc.perform(get("/edicoes/{id}/exemplares/novo", edition.getId()).with(foreign)).andExpect(status().isNotFound());
        mockMvc.perform(post("/edicoes/{id}/exemplares", edition.getId()).with(foreign).with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound());
        assertThat(physicalCopyRepository.count()).isZero();
        var id = copyService.create(edition.getId(), owner.getId(), new CopyService.Command(null, CopyCondition.UNKNOWN, null, null, null, null, null));
        for (String action : List.of("editar", "excluir")) {
            mockMvc.perform(get("/exemplares/{id}/" + action, id).with(foreign)).andExpect(status().isNotFound());
            mockMvc.perform(post("/exemplares/{id}/" + action, id).with(foreign).with(SecurityMockMvcRequestPostProcessors.csrf()))
                    .andExpect(status().isNotFound());
        }
        assertThat(physicalCopyRepository.count()).isEqualTo(1);
    }

    @Test
    void protectsReferencedEditionsAndLocationsAndRequiresConfirmedCopyDeletion() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("delete-copias@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var edition = editionService.create(owner.getId(), new EditionService.Command(null, null, "HQ protegida", null, null, null, null, null, null));
        var box = locationService.create(owner.getId(), new br.com.minhagibiteca.inventory.application.LocationService.Command(
                null, br.com.minhagibiteca.inventory.domain.LocationLevel.BOX, "Caixa ocupada", null));
        var copy = copyService.create(edition.getId(), owner.getId(), new CopyService.Command(box, CopyCondition.NEW, null, null, null, null, null));
        mockMvc.perform(post("/edicoes/{id}/excluir", edition.getId()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(redirectedUrl("/edicoes")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attributeExists("errorMessage"));
        mockMvc.perform(post("/localizacoes/{id}/excluir", box).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("confirmed", "true"))
                .andExpect(redirectedUrl("/localizacoes")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attributeExists("errorMessage"));
        mockMvc.perform(post("/edicoes/{id}/editar", edition.getId()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("title", "HQ protegida").param("mediaType", "DIGITAL"))
                .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model().attributeHasFieldErrors("editionForm", "mediaType"));
        assertThat(editionService.viewOwned(edition.getId(), owner.getId()).mediaType()).isNull();
        copyService.update(copy, owner.getId(), new CopyService.Command(null, CopyCondition.NEW, null, null, null, null, null));
        locationService.delete(box, owner.getId());
        mockMvc.perform(get("/exemplares/{id}/excluir", copy).with(auth)).andExpect(status().isOk()).andExpect(view().name("exemplares/delete"));
        mockMvc.perform(post("/exemplares/{id}/excluir", copy).with(auth).param("confirmed", "true")).andExpect(status().isForbidden());
        mockMvc.perform(post("/exemplares/{id}/excluir", copy).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(redirectedUrl("/exemplares/" + copy + "/excluir"));
        assertThat(physicalCopyRepository.existsById(copy)).isTrue();
        mockMvc.perform(post("/exemplares/{id}/excluir", copy).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("confirmed", "true"))
                .andExpect(redirectedUrl("/edicoes/" + edition.getId()));
        assertThat(physicalCopyRepository.existsById(copy)).isFalse();
        assertThat(editionRepository.existsById(edition.getId())).isTrue();
        editionService.delete(edition.getId(), owner.getId());
    }

    @Test
    void paginatesCopiesAndEnforcesCompositeOwnerForeignKeysInPostgres() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("page-copias@example.com", "hash", UserRole.USER));
        var other = userAccountRepository.save(UserAccount.create("fk-copias@example.com", "hash", UserRole.USER));
        var edition = editionService.create(owner.getId(), new EditionService.Command(null, null, "HQ paginada", null, null, null, null, null, null));
        var foreignEdition = editionService.create(other.getId(), new EditionService.Command(null, null, "HQ alheia", null, null, null, null, null, null));
        var box = locationService.create(other.getId(), new br.com.minhagibiteca.inventory.application.LocationService.Command(
                null, br.com.minhagibiteca.inventory.domain.LocationLevel.BOX, "Caixa privada", null));
        var ids = new java.util.ArrayList<java.util.UUID>();
        for (int i = 0; i < 21; i++) ids.add(copyService.create(edition.getId(), owner.getId(), new CopyService.Command(null, CopyCondition.UNKNOWN, null, null, null, null, null)));
        assertThat(copyService.list(edition.getId(), owner.getId(), 0).getContent()).hasSize(20);
        assertThat(copyService.list(edition.getId(), owner.getId(), 1).getContent()).hasSize(1);
        mockMvc.perform(get("/edicoes/{id}", edition.getId()).with(SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner))))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("copyPage=1")));
        mockMvc.perform(get("/edicoes/{id}", edition.getId()).param("copyPage", "1").with(SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner))))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Exemplar 21")));
        try (Connection connection = dataSource.getConnection()) {
            for (String column : List.of("edition_id", "storage_location_id")) {
                try (var sql = connection.prepareStatement("update physical_copies set " + column + " = ? where id = ?")) {
                    sql.setObject(1, column.equals("edition_id") ? foreignEdition.getId() : box); sql.setObject(2, ids.getFirst());
                    assertThatThrownBy(sql::executeUpdate).isInstanceOf(java.sql.SQLException.class);
                }
            }
            try (var sql = connection.prepareStatement("delete from editions where id = ?")) {
                sql.setObject(1, edition.getId()); assertThatThrownBy(sql::executeUpdate).isInstanceOf(java.sql.SQLException.class);
            }
        }
    }

    @Test
    void managesExplicitChecklistWithoutInferringOrSynchronizingNumbers() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("checklist@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var series = comicSeriesRepository.save(ComicSeries.create(owner.getId(), null, "Série irregular", null, null, null, null));
        mockMvc.perform(get("/series/{id}/faltantes", series.getId()).with(auth)).andExpect(status().isOk()).andExpect(view().name("checklist/list"));
        assertThat(checklistRepository.count()).isZero();
        for (String number : List.of("0", "1A", "Especial")) {
            mockMvc.perform(post("/series/{id}/checklist", series.getId()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                            .param("editorialNumber", number).param("owned", number.equals("1A") ? "false" : "true"))
                    .andExpect(redirectedUrl("/series/" + series.getId() + "/checklist"));
        }
        var missing = checklistService.list(series.getId(), owner.getId(), true, 0);
        assertThat(missing.getContent()).extracting(br.com.minhagibiteca.collection.application.ChecklistView::editorialNumber).containsExactly("1A");
        var id = missing.getContent().getFirst().id();
        var edition = editionService.create(owner.getId(), new EditionService.Command(null, series.getId(), "Edição 1A", "1A", null, null, null, null, null));
        var copy = copyService.create(edition.getId(), owner.getId(), new CopyService.Command(null, CopyCondition.GOOD, null, null, null, null, null));
        assertThat(checklistService.find(id, owner.getId()).owned()).isFalse();
        assertThat(checklistRepository.count()).isEqualTo(3);
        mockMvc.perform(get("/checklist/itens/{id}/editar", id).with(auth)).andExpect(status().isOk()).andExpect(view().name("checklist/form"));
        mockMvc.perform(post("/checklist/itens/{id}/editar", id).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("editorialNumber", "1A").param("owned", "true").param("label", "<script>texto</script>").param("seriesId", java.util.UUID.randomUUID().toString()))
                .andExpect(redirectedUrl("/series/" + series.getId() + "/checklist"));
        assertThat(checklistService.find(id, owner.getId()).seriesId()).isEqualTo(series.getId());
        copyService.delete(copy, owner.getId());
        assertThat(checklistService.find(id, owner.getId()).owned()).isTrue();
        assertThat(checklistService.list(series.getId(), owner.getId(), true, 0).getTotalElements()).isZero();
        mockMvc.perform(get("/series/{id}/checklist", series.getId()).with(auth)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;script&gt;texto&lt;/script&gt;")));
        mockMvc.perform(post("/checklist/itens/{id}/editar", id).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("editorialNumber", "1A").param("owned", "false"))
                .andExpect(redirectedUrl("/series/" + series.getId() + "/checklist"));
        assertThat(checklistService.list(series.getId(), owner.getId(), true, 0).getTotalElements()).isEqualTo(1);
    }

    @Test
    void validatesChecklistNumbersAndKeepsOwnersAndSeriesIsolated() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("valid-checklist@example.com", "hash", UserRole.USER));
        var other = userAccountRepository.save(UserAccount.create("foreign-checklist@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var foreign = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(other));
        var series = comicSeriesRepository.save(ComicSeries.create(owner.getId(), null, "Checklist privado", null, null, null, null));
        var second = comicSeriesRepository.save(ComicSeries.create(owner.getId(), null, "Outra série", null, null, null, null));
        var id = checklistService.create(series.getId(), owner.getId(), new br.com.minhagibiteca.collection.application.ChecklistService.Command("1A", null, false, null));
        for (String number : List.of("", " ", "x".repeat(41), " 1a ")) {
            mockMvc.perform(post("/series/{id}/checklist", series.getId()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("editorialNumber", number))
                    .andExpect(status().isOk()).andExpect(view().name("checklist/list"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model().attributeHasFieldErrors("checklistForm", "editorialNumber"));
        }
        mockMvc.perform(post("/series/{id}/checklist", series.getId()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("editorialNumber", "2").param("label", "x".repeat(181)).param("notes", "x".repeat(2001)).param("owned", "inválido"))
                .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model().attributeHasFieldErrors("checklistForm", "label", "notes", "owned"));
        mockMvc.perform(post("/series/{id}/checklist", second.getId()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("editorialNumber", "1A"))
                .andExpect(redirectedUrl("/series/" + second.getId() + "/checklist"));
        for (String action : List.of("checklist", "faltantes")) mockMvc.perform(get("/series/{id}/" + action, series.getId()).with(foreign)).andExpect(status().isNotFound());
        mockMvc.perform(post("/series/{id}/checklist", series.getId()).with(foreign).with(SecurityMockMvcRequestPostProcessors.csrf()).param("editorialNumber", "3"))
                .andExpect(status().isNotFound());
        for (String action : List.of("editar", "excluir")) {
            mockMvc.perform(get("/checklist/itens/{id}/" + action, id).with(foreign)).andExpect(status().isNotFound());
            mockMvc.perform(post("/checklist/itens/{id}/" + action, id).with(foreign).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isNotFound());
        }
        assertThat(checklistRepository.count()).isEqualTo(2);
        mockMvc.perform(post("/series/{id}/checklist", series.getId()).with(auth).param("editorialNumber", "3")).andExpect(status().isForbidden());
        mockMvc.perform(post("/checklist/itens/{id}/editar", id).with(auth).param("editorialNumber", "3")).andExpect(status().isForbidden());
    }

    @Test
    void protectsChecklistSeriesAndConfirmsCsrfDeletionWithoutRemovingCatalog() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("delete-checklist@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var series = comicSeriesRepository.save(ComicSeries.create(owner.getId(), null, "Série protegida", null, null, null, null));
        var id = checklistService.create(series.getId(), owner.getId(), new br.com.minhagibiteca.collection.application.ChecklistService.Command("Especial", null, false, null));
        mockMvc.perform(post("/series/{id}/excluir", series.getId()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(redirectedUrl("/series")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attributeExists("errorMessage"));
        mockMvc.perform(get("/checklist/itens/{id}/excluir", id).with(auth)).andExpect(status().isOk()).andExpect(view().name("checklist/delete"));
        mockMvc.perform(post("/checklist/itens/{id}/excluir", id).with(auth).param("confirmed", "true")).andExpect(status().isForbidden());
        mockMvc.perform(post("/checklist/itens/{id}/excluir", id).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(redirectedUrl("/checklist/itens/" + id + "/excluir"));
        assertThat(checklistRepository.existsById(id)).isTrue();
        mockMvc.perform(post("/checklist/itens/{id}/excluir", id).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("confirmed", "true"))
                .andExpect(redirectedUrl("/series/" + series.getId() + "/checklist"));
        assertThat(checklistRepository.existsById(id)).isFalse();
        assertThat(comicSeriesRepository.existsById(series.getId())).isTrue();
        var edition = editionService.create(owner.getId(), new EditionService.Command(null, series.getId(), "HQ vinculada", null, null, null, null, null, null));
        assertThatThrownBy(() -> seriesService.delete(series.getId(), owner.getId())).hasMessageContaining("possui edições");
        editionService.delete(edition.getId(), owner.getId()); seriesService.delete(series.getId(), owner.getId());
        assertThat(comicSeriesRepository.existsById(series.getId())).isFalse();
    }

    @Test
    void paginatesMissingChecklistAndEnforcesSeriesOwnershipAndDuplicatesInDatabase() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("page-checklist@example.com", "hash", UserRole.USER));
        var other = userAccountRepository.save(UserAccount.create("fk-checklist@example.com", "hash", UserRole.USER));
        var series = comicSeriesRepository.save(ComicSeries.create(owner.getId(), null, "Lista longa", null, null, null, null));
        var foreignSeries = comicSeriesRepository.save(ComicSeries.create(other.getId(), null, "Lista alheia", null, null, null, null));
        for (int i = 0; i < 25; i++) checklistService.create(series.getId(), owner.getId(), new br.com.minhagibiteca.collection.application.ChecklistService.Command("E" + i, null, false, null));
        var ownedId = checklistService.create(series.getId(), owner.getId(), new br.com.minhagibiteca.collection.application.ChecklistService.Command("00", null, true, null));
        assertThat(checklistService.list(series.getId(), owner.getId(), true, 0).getContent()).hasSize(20);
        assertThat(checklistService.list(series.getId(), owner.getId(), true, 1).getContent()).hasSize(5);
        assertThat(checklistService.list(series.getId(), owner.getId(), false, 1).getContent()).hasSize(6);
        assertThat(checklistService.list(series.getId(), owner.getId(), true, -1).getNumber()).isZero();
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        mockMvc.perform(get("/series/{id}/faltantes", series.getId()).with(auth)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/faltantes?page=1")));
        mockMvc.perform(get("/series/{id}/checklist", series.getId()).with(auth)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/checklist?page=1")));
        mockMvc.perform(get("/series/{id}/faltantes", series.getId()).with(auth).param("page", "1")).andExpect(status().isOk());
        mockMvc.perform(get("/series/{id}/checklist", series.getId()).with(auth).param("page", "texto")).andExpect(status().isBadRequest());
        try (Connection connection = dataSource.getConnection()) {
            for (String sqlText : List.of("update series_checklist_items set series_id = ? where id = ?", "update series_checklist_items set normalized_number = ? where id = ?", "delete from series where id = ?")) {
                try (var sql = connection.prepareStatement(sqlText)) {
                    if (sqlText.startsWith("delete")) sql.setObject(1, series.getId());
                    else { sql.setObject(1, sqlText.contains("normalized_number") ? "e0" : foreignSeries.getId()); sql.setObject(2, ownedId); }
                    assertThatThrownBy(sql::executeUpdate).isInstanceOf(java.sql.SQLException.class);
                }
            }
        }
    }

    @Test
    void paginatesLocationsAndEnforcesParentOwnershipInDatabase() throws Exception {
        UserAccount owner = userAccountRepository.save(UserAccount.create("pag-local@example.com", "hash", UserRole.USER));
        UserAccount other = userAccountRepository.save(UserAccount.create("foreign-local@example.com", "hash", UserRole.USER));
        for (int i = 0; i < 25; i++) locationService.create(owner.getId(), new br.com.minhagibiteca.inventory.application.LocationService.Command(
                null, br.com.minhagibiteca.inventory.domain.LocationLevel.BOX, "Caixa " + i, null));
        assertThat(locationService.list(owner.getId(), "caixa", 0).getContent()).hasSize(20);
        assertThat(locationService.list(owner.getId(), "caixa", 1).getContent()).hasSize(5);
        assertThat(locationService.list(other.getId(), "caixa", 0).getTotalElements()).isZero();
        var parent = locationService.create(owner.getId(), new br.com.minhagibiteca.inventory.application.LocationService.Command(
                null, br.com.minhagibiteca.inventory.domain.LocationLevel.ROOM, "Pai privado", null));
        try (Connection connection = dataSource.getConnection(); var sql = connection.prepareStatement(
                "insert into storage_locations (id, owner_user_id, parent_id, level, name, normalized_name) values (?, ?, ?, 'BOX', 'Alheia', 'alheia')")) {
            sql.setObject(1, java.util.UUID.randomUUID()); sql.setObject(2, other.getId()); sql.setObject(3, parent);
            assertThatThrownBy(sql::executeUpdate).isInstanceOf(java.sql.SQLException.class);
        }
    }
}
