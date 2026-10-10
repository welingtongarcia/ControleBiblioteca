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
import java.math.BigDecimal;
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
    @Autowired private br.com.minhagibiteca.collection.application.ReadingService readingService;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private br.com.minhagibiteca.catalog.application.IsbnLookupService isbnLookup;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private br.com.minhagibiteca.catalog.application.MetadataSearchProvider metadataProvider;
    @Autowired private PhysicalCopyRepository physicalCopyRepository;
    @Autowired private br.com.minhagibiteca.collection.application.ChecklistService checklistService;
    @Autowired private br.com.minhagibiteca.collection.infrastructure.ChecklistRepository checklistRepository;
    @Autowired private br.com.minhagibiteca.catalog.application.ComicSeriesService seriesService;

    @Autowired
    private WorkService workService;

    @Autowired
    private CreatorService creatorService;

    @Autowired private br.com.minhagibiteca.catalog.application.CharacterService characterService;

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
        new org.springframework.jdbc.core.JdbcTemplate(dataSource).update("delete from comic_characters");
        workRepository.deleteAll();
        creatorRepository.deleteAll();
        storageLocationRepository.deleteAllInBatch();
        comicSeriesRepository.deleteAll();
        publisherRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void searchesAndAppliesFreeMetadataWithCsrfOwnerIsolationAndDraftPreservation() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("metadata@example.com", "hash", UserRole.USER));
        var other = userAccountRepository.save(UserAccount.create("metadata-other@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var foreignAuth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(other));
        var publisher = publisherRepository.save(Publisher.create(owner.getId(), "Editora", null, null));
        publisherRepository.save(Publisher.create(other.getId(), "Editora", null, null));
        var creator = creatorService.create(owner.getId(), new CreatorService.Command("Autor", null));
        creatorService.create(other.getId(), new CreatorService.Command("Estrangeiro", null));
        var candidate = new br.com.minhagibiteca.catalog.application.MetadataCandidate("OL123M", "9780140328721", "HQ <script>", 2021,
                List.of("Editora"), List.of("Autor", "Estrangeiro"), br.com.minhagibiteca.catalog.application.MetadataCandidate.Source.OPEN_LIBRARY);
        org.mockito.Mockito.when(metadataProvider.search(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of(candidate));
        mockMvc.perform(post("/edicoes/metadados/buscar").with(auth).param("metadataQuery", "Batman")).andExpect(status().isForbidden());
        mockMvc.perform(post("/edicoes/metadados/buscar").with(SecurityMockMvcRequestPostProcessors.csrf()).param("metadataQuery", "Batman")).andExpect(status().is3xxRedirection());
        var result = mockMvc.perform(post("/edicoes/metadados/buscar").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                .param("metadataQuery", "Batman").param("referencePaidPrice", "19,50").param("purchaseDate", "2025-10-01").param("mediaType", "DIGITAL").param("notes", "Meu rascunho"))
                .andExpect(redirectedUrl("/edicoes/nova")).andReturn();
        var search = (br.com.minhagibiteca.catalog.application.MetadataSearchService.Result) result.getFlashMap().get("metadataResult");
        var draft = (br.com.minhagibiteca.catalog.presentation.EditionForm) result.getFlashMap().get("editionForm");
        assertThat(draft.getTitle()).isNull(); assertThat(editionRepository.count()).isZero();
        var rendered = mockMvc.perform(get("/edicoes/nova").with(auth).flashAttrs(result.getFlashMap()))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("HQ &lt;script&gt;")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("https://openlibrary.org/books/OL123M")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Aplicar dados desta edição"))).andReturn();
        var preview = java.nio.file.Path.of("target/ui-metadata"); java.nio.file.Files.createDirectories(preview.resolve("css"));
        java.nio.file.Files.writeString(preview.resolve("index.html"), rendered.getResponse().getContentAsString());
        for (String css : List.of("main.css", "dashboard.css")) java.nio.file.Files.copy(java.nio.file.Path.of("src/main/resources/static/css", css), preview.resolve("css").resolve(css), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        mockMvc.perform(post("/edicoes/metadados/aplicar").with(auth).param("metadataToken", search.token().toString()).param("candidateIndex", "0")).andExpect(status().isForbidden());
        var foreign = mockMvc.perform(post("/edicoes/metadados/aplicar").with(foreignAuth).with(SecurityMockMvcRequestPostProcessors.csrf())
                .param("metadataToken", search.token().toString()).param("candidateIndex", "0"))
                .andExpect(redirectedUrl("/edicoes/nova")).andReturn();
        assertThat(foreign.getFlashMap().get("metadataMessage").toString()).contains("inválida");
        var applied = mockMvc.perform(post("/edicoes/metadados/aplicar").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                .param("metadataToken", search.token().toString()).param("candidateIndex", "0").param("referencePaidPrice", "19,50")
                .param("purchaseDate", "2025-10-01").param("mediaType", "DIGITAL").param("notes", "Meu rascunho"))
                .andExpect(redirectedUrl("/edicoes/nova")).andReturn();
        var filled = (br.com.minhagibiteca.catalog.presentation.EditionForm) applied.getFlashMap().get("editionForm");
        assertThat(filled.getTitle()).isEqualTo("HQ <script>"); assertThat(filled.getIsbn()).isEqualTo("9780140328721");
        assertThat(filled.getPublicationYear()).isEqualTo(2021); assertThat(filled.getPublisherId()).isEqualTo(publisher.getId());
        assertThat(filled.getCreatorIds()).containsExactly(creator.getId()); assertThat(filled.getReferencePaidPrice()).isEqualByComparingTo("19.50");
        assertThat(filled.getPurchaseDate()).isEqualTo(java.time.LocalDate.of(2025, 10, 1)); assertThat(filled.getMediaType()).isEqualTo(br.com.minhagibiteca.catalog.domain.MediaType.DIGITAL);
        assertThat(filled.getNotes()).isEqualTo("Meu rascunho"); assertThat(editionRepository.count()).isZero();
        mockMvc.perform(get("/edicoes/nova").with(auth).flashAttrs(applied.getFlashMap())).andExpect(status().isOk());
        var preserve = mockMvc.perform(post("/edicoes/metadados/aplicar").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                .param("metadataToken", search.token().toString()).param("candidateIndex", "0").param("title", "Meu título").param("isbn", "manual")
                .param("publicationYear", "2020").param("publisherId", publisher.getId().toString()).param("creatorIds", creator.getId().toString()))
                .andExpect(redirectedUrl("/edicoes/nova")).andReturn();
        var kept = (br.com.minhagibiteca.catalog.presentation.EditionForm) preserve.getFlashMap().get("editionForm");
        assertThat(kept.getTitle()).isEqualTo("Meu título"); assertThat(kept.getIsbn()).isEqualTo("manual"); assertThat(kept.getPublicationYear()).isEqualTo(2020);
        for (String index : List.of("-1", "99", "bad")) {
            var invalid = mockMvc.perform(post("/edicoes/metadados/aplicar").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                    .param("metadataToken", search.token().toString()).param("candidateIndex", index)).andExpect(redirectedUrl("/edicoes/nova")).andReturn();
            assertThat(invalid.getFlashMap().get("metadataMessage").toString()).containsIgnoringCase("inválida");
        }
        var invalidFields = mockMvc.perform(post("/edicoes/metadados/buscar").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                .param("metadataQuery", "Batman").param("referencePaidPrice", "bad")).andExpect(redirectedUrl("/edicoes/nova")).andReturn();
        assertThat(invalidFields.getFlashMap().containsKey(org.springframework.validation.BindingResult.MODEL_KEY_PREFIX + "editionForm")).isTrue();
        mockMvc.perform(get("/edicoes/nova").with(auth).flashAttrs(invalidFields.getFlashMap())).andExpect(status().isOk());
        var invalidSource = mockMvc.perform(post("/edicoes/metadados/buscar").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                .param("metadataQuery", "Batman").param("metadataSource", "PAID"))
                .andExpect(redirectedUrl("/edicoes/nova")).andReturn();
        assertThat(invalidSource.getFlashMap().get("metadataMessage").toString()).contains("fonte disponível");
        org.mockito.Mockito.verify(metadataProvider, org.mockito.Mockito.times(1)).search(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void fillsIsbnDraftWithoutSavingOrOverwritingAndAssociatesOnlyOwnedNames() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("isbn@example.com", "hash", UserRole.USER));
        var other = userAccountRepository.save(UserAccount.create("foreign-isbn@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var pub = publisherRepository.save(Publisher.create(owner.getId(), "Editora ISBN", null, null));
        var creator = creatorService.create(owner.getId(), new CreatorService.Command("Autor ISBN", null));
        publisherRepository.save(Publisher.create(other.getId(), "Editora ISBN", null, null));
        creatorService.create(other.getId(), new CreatorService.Command("Autor estrangeiro", null));
        var found = new br.com.minhagibiteca.catalog.application.IsbnMetadata("9780140328721", "Título encontrado <script>", 2001,
                List.of("editora isbn"), List.of("autor isbn", "Autor estrangeiro"), true);
        org.mockito.Mockito.when(isbnLookup.lookup("9780140328721")).thenReturn(java.util.Optional.of(found));
        mockMvc.perform(post("/edicoes/isbn").with(auth).param("isbn", "9780140328721")).andExpect(status().isForbidden());
        mockMvc.perform(post("/edicoes/isbn").with(SecurityMockMvcRequestPostProcessors.csrf()).param("isbn", "9780140328721"))
                .andExpect(status().is3xxRedirection());
        var result = mockMvc.perform(post("/edicoes/isbn").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("isbn", "9780140328721").param("referencePaidPrice", "19,50").param("mediaType", "DIGITAL")
                        .param("notes", "Anotação própria").param("returnTo", "/edicoes?q=HQ&page=1"))
                .andExpect(redirectedUrl("/edicoes/nova?returnTo=/edicoes?q%3DHQ%26page%3D1")).andReturn();
        var draft = (br.com.minhagibiteca.catalog.presentation.EditionForm) result.getFlashMap().get("editionForm");
        assertThat(draft.getTitle()).isEqualTo(found.title()); assertThat(draft.getPublicationYear()).isEqualTo(2001);
        assertThat(draft.getPublisherId()).isEqualTo(pub.getId()); assertThat(draft.getCreatorIds()).containsExactly(creator.getId());
        assertThat(draft.getReferencePaidPrice()).isEqualByComparingTo("19.50"); assertThat(draft.getMediaType()).isEqualTo(br.com.minhagibiteca.catalog.domain.MediaType.DIGITAL);
        assertThat(draft.getNotes()).isEqualTo("Anotação própria"); assertThat(editionRepository.count()).isZero();
        var rendered = mockMvc.perform(get("/edicoes/nova").param("returnTo", "/edicoes?q=HQ&page=1").with(auth).flashAttrs(result.getFlashMap()))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Título encontrado &lt;script&gt;")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/edicoes?q=HQ&amp;page=1")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Buscar pelo ISBN"))).andReturn();
        var preview = java.nio.file.Path.of("target/ui-isbn"); java.nio.file.Files.createDirectories(preview.resolve("css"));
        java.nio.file.Files.writeString(preview.resolve("index.html"), rendered.getResponse().getContentAsString());
        for (String css : List.of("main.css", "dashboard.css")) java.nio.file.Files.copy(java.nio.file.Path.of("src/main/resources/static/css", css), preview.resolve("css").resolve(css), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        var preserve = mockMvc.perform(post("/edicoes/isbn").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("isbn", "9780140328721").param("title", "Meu título").param("publicationYear", "2020")
                        .param("publisherId", pub.getId().toString()).param("creatorIds", creator.getId().toString()))
                .andExpect(redirectedUrl("/edicoes/nova")).andReturn();
        var kept = (br.com.minhagibiteca.catalog.presentation.EditionForm) preserve.getFlashMap().get("editionForm");
        assertThat(kept.getTitle()).isEqualTo("Meu título"); assertThat(kept.getPublicationYear()).isEqualTo(2020);
        mockMvc.perform(post("/edicoes").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("title", draft.getTitle()).param("isbn", draft.getIsbn()).param("publicationYear", draft.getPublicationYear().toString())
                        .param("publisherId", draft.getPublisherId().toString()).param("creatorIds", creator.getId().toString())
                        .param("mediaType", "DIGITAL").param("referencePaidPrice", "19,50"))
                .andExpect(redirectedUrl("/edicoes"));
        assertThat(editionRepository.count()).isEqualTo(1);
    }

    @Test
    void rendersGroupedEditionFormAndExplainsEmptyOptionalAssociations() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("form-empty@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var result = mockMvc.perform(get("/edicoes/nova").with(auth).param("returnTo", "/edicoes?q=HQ&page=1"))
                .andExpect(status().isOk()).andReturn();
        var html = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(html).contains("Identificação da edição", "Suporte e valores", "Vínculos bibliográficos",
                "Somente o título é obrigatório", "Nenhuma obra cadastrada", "Nenhum criador cadastrado",
                "formaction=\"/edicoes/isbn\"", "formnovalidate", "name=\"_csrf\"", "A capa pode ser enviada",
                "href=\"/edicoes?q=HQ&amp;page=1\"").doesNotContain("aria-invalid=\"true\"");
        saveEditionFormPreview("empty.html", html);
    }

    @Test
    void savesAndClearsCheckboxAssociationsWithoutLosingEditionContext() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("form-checks@example.com", "hash", UserRole.USER));
        var other = userAccountRepository.save(UserAccount.create("form-checks-other@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var pub = publisherRepository.save(Publisher.create(owner.getId(), "Editora do formulário", null, null));
        var series = comicSeriesRepository.save(ComicSeries.create(owner.getId(), pub.getId(), "Série do formulário", null, null, null, null));
        var workA = workService.create(owner.getId(), new WorkService.Command("Uma aventura em quadrinhos", null, null));
        var workB = workService.create(owner.getId(), new WorkService.Command("Obra com nome extenso para testar seleção em telas pequenas", null, null));
        var creatorA = creatorService.create(owner.getId(), new CreatorService.Command("Autora da história", null));
        var creatorB = creatorService.create(owner.getId(), new CreatorService.Command("Artista dos desenhos", null));
        workService.create(other.getId(), new WorkService.Command("Obra privada de outro usuário", null, null));
        mockMvc.perform(post("/edicoes").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                .param("title", "A aventura do colecionador").param("editorialNumber", "1A").param("isbn", "9786559601325")
                .param("publicationYear", "2026").param("format", "Encadernado").param("mediaType", "PHYSICAL")
                .param("referencePaidPrice", "39,50").param("coverPrice", "59,90")
                .param("publisherId", pub.getId().toString()).param("seriesId", series.getId().toString())
                .param("workIds", workA.getId().toString(), workB.getId().toString()).param("creatorIds", creatorA.getId().toString(), creatorB.getId().toString())
                .param("returnTo", "/edicoes?q=aventura&page=1"))
                .andExpect(redirectedUrl("/edicoes?q=aventura&page=1"));
        var edition = editionRepository.findAll().getFirst();
        assertThat(editionAssociationService.workIds(edition.getId())).containsExactlyInAnyOrder(workA.getId(), workB.getId());
        assertThat(editionAssociationService.creatorIds(edition.getId())).containsExactlyInAnyOrder(creatorA.getId(), creatorB.getId());
        var rendered = mockMvc.perform(get("/edicoes/{id}/editar", edition.getId()).with(auth)
                .param("returnTo", "/edicoes?q=aventura&page=1")).andExpect(status().isOk()).andReturn();
        var html = rendered.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(html.split("checked=\"checked\"", -1)).hasSize(5);
        assertThat(html).contains("type=\"checkbox\"", "name=\"_workIds\"", "name=\"_creatorIds\"", "39,50", "59,90")
                .doesNotContain("Buscar pelo ISBN", "Obra privada de outro usuário");
        saveEditionFormPreview("edit.html", html);
        var draft = rendered.getModelAndView().getModel().get("editionForm");
        var newForm = mockMvc.perform(get("/edicoes/nova").with(auth).flashAttr("editionForm", draft)).andExpect(status().isOk()).andReturn();
        saveEditionFormPreview("filled.html", newForm.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        mockMvc.perform(post("/edicoes/{id}/editar", edition.getId()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                .param("title", "A aventura do colecionador").param("_workIds", "on").param("_creatorIds", "on")
                .param("mediaType", "PHYSICAL").param("referencePaidPrice", "39,50")
                .param("returnTo", "/edicoes?q=aventura&page=1")).andExpect(status().is3xxRedirection());
        assertThat(editionAssociationService.workIds(edition.getId())).isEmpty();
        assertThat(editionAssociationService.creatorIds(edition.getId())).isEmpty();
    }

    @Test
    void linksEditionErrorsToFieldsAndPreservesInvalidDraft() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("form-errors@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var result = mockMvc.perform(post("/edicoes").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                .param("title", "").param("publicationYear", "1700").param("referencePaidPrice", "-1")
                .param("coverPrice", "inválido").param("publisherId", "inválido").param("workIds", "inválido")
                .param("variant", "Meu rascunho").param("notes", "Anotação preservada <script>")
                .param("returnTo", "/edicoes?q=HQ")).andExpect(status().isOk()).andReturn();
        var html = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(html).contains("Revise os campos indicados", "href=\"#title\"", "href=\"#publicationYear\"",
                "href=\"#referencePaidPrice\"", "href=\"#coverPrice\"", "href=\"#publisherId\"", "href=\"#workIds\"",
                "id=\"title-error\"", "aria-invalid=\"true\"", "Meu rascunho", "preservada &lt;script",
                "value=\"1700\"", "href=\"/edicoes?q=HQ\"", "Selecione uma editora da lista", "Selecione apenas obras")
                .doesNotContain("Failed to convert", "java.util.UUID", "java.util.List");
        assertThat(editionRepository.count()).isZero();
        saveEditionFormPreview("errors.html", html);
    }

    @Test
    void rendersReadingSwitchInBothStatesAndPreservesPostContext() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("switch@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var edition = editionService.create(owner.getId(), new EditionService.Command(null, null, "Leitura com interruptor", null, null, null, null, null, null));
        var preview = java.nio.file.Path.of("target/ui-reading-switch");
        java.nio.file.Files.createDirectories(preview.resolve("css"));
        for (String css : List.of("main.css", "dashboard.css"))
            java.nio.file.Files.copy(java.nio.file.Path.of("src/main/resources/static/css", css),
                    preview.resolve("css").resolve(css), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        for (boolean read : List.of(false, true)) {
            for (boolean catalog : List.of(true, false)) {
                var path = catalog ? "/edicoes" : "/edicoes/" + edition.getId();
                var request = get(path).with(auth);
                if (catalog) request.param("q", "Leitura"); else request.param("returnTo", "/edicoes?q=Leitura");
                var html = mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse()
                        .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
                assertThat(html).contains("role=\"switch\"", "aria-checked=\"" + read + "\"",
                        "aria-label=\"Edição lida: Leitura com interruptor\"",
                        "name=\"read\" value=\"" + !read + "\"", "name=\"_csrf\"", "reading-switch-track",
                        read ? "Já li" : "Não li", "/edicoes/" + edition.getId() + "/leitura")
                        .doesNotContain("Marcar como já lida", "Marcar como não lida");
                java.nio.file.Files.writeString(preview.resolve((catalog ? "catalog-" : "detail-") + (read ? "on" : "off") + ".html"), html);
            }
            var expectedRedirect = read ? "/edicoes/" + edition.getId() + "?returnTo=/edicoes?q%3DLeitura" : "/edicoes?q=Leitura";
            mockMvc.perform(post("/edicoes/{id}/leitura", edition.getId()).with(auth)
                    .with(SecurityMockMvcRequestPostProcessors.csrf()).param("read", Boolean.toString(!read))
                    .param("fromCatalog", Boolean.toString(!read)).param("returnTo", "/edicoes?q=Leitura"))
                    .andExpect(redirectedUrl(expectedRedirect));
            assertThat(editionService.viewOwned(edition.getId(), owner.getId()).read()).isEqualTo(!read);
        }
        mockMvc.perform(get("/edicoes/{id}", edition.getId()).with(auth)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("aria-checked=\"false\"")));
    }

    private void saveEditionFormPreview(String name, String html) throws java.io.IOException {
        var preview = java.nio.file.Path.of("target/ui-edition-form");
        java.nio.file.Files.createDirectories(preview.resolve("css"));
        java.nio.file.Files.writeString(preview.resolve(name), html);
        for (String css : List.of("main.css", "dashboard.css"))
            java.nio.file.Files.copy(java.nio.file.Path.of("src/main/resources/static/css", css),
                    preview.resolve("css").resolve(css), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    void rendersGoogleBooksSuggestionsAndSafeSourceWithoutSaving() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("google-isbn@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var found = new br.com.minhagibiteca.catalog.application.IsbnMetadata("9786559601325", "Sugestão <script>", 2021,
                List.of("Editora da fonte"), List.of("Autor da fonte"), true, br.com.minhagibiteca.catalog.application.IsbnMetadata.Source.GOOGLE_BOOKS);
        org.mockito.Mockito.when(isbnLookup.lookup("978-6559601325")).thenReturn(java.util.Optional.of(found));
        var result = mockMvc.perform(post("/edicoes/isbn").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("isbn", "978-6559601325").param("referencePaidPrice", "12,50").param("notes", "Meu rascunho"))
                .andExpect(redirectedUrl("/edicoes/nova")).andReturn();
        var rendered = mockMvc.perform(get("/edicoes/nova").with(auth).flashAttrs(result.getFlashMap()))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Sugestões — Google Books")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("https://books.google.com/books?vid=ISBN9786559601325")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sugestão &lt;script&gt;")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("12,50")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Meu rascunho"))).andReturn();
        assertThat(editionRepository.count()).isZero();
        var preview = java.nio.file.Path.of("target/ui-google-isbn"); java.nio.file.Files.createDirectories(preview.resolve("css"));
        java.nio.file.Files.writeString(preview.resolve("index.html"), rendered.getResponse().getContentAsString());
        for (String css : List.of("main.css", "dashboard.css")) java.nio.file.Files.copy(java.nio.file.Path.of("src/main/resources/static/css", css), preview.resolve("css").resolve(css), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    void preservesIsbnDraftWhenLookupFailsOrBindingIsInvalid() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("isbn-failure@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        org.mockito.Mockito.when(isbnLookup.lookup("123")).thenThrow(new br.com.minhagibiteca.catalog.application.IsbnLookupService.LookupException("ISBN inválido"));
        org.mockito.Mockito.when(isbnLookup.lookup("9780140328721")).thenReturn(java.util.Optional.empty());
        org.mockito.Mockito.when(isbnLookup.lookup("9780132350884")).thenThrow(new br.com.minhagibiteca.catalog.application.IsbnLookupService.LookupException("Serviço indisponível"));
        for (String isbn : List.of("123", "9780140328721", "9780132350884")) {
            var result = mockMvc.perform(post("/edicoes/isbn").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                            .param("isbn", isbn).param("title", "Rascunho").param("notes", "Não perder"))
                    .andExpect(redirectedUrl("/edicoes/nova")).andReturn();
            var draft = (br.com.minhagibiteca.catalog.presentation.EditionForm) result.getFlashMap().get("editionForm");
            assertThat(draft.getTitle()).isEqualTo("Rascunho"); assertThat(draft.getNotes()).isEqualTo("Não perder");
            assertThat(result.getFlashMap().containsKey("isbnMessage")).isTrue();
            mockMvc.perform(get("/edicoes/nova").with(auth).flashAttrs(result.getFlashMap())).andExpect(status().isOk());
        }
        var invalid = mockMvc.perform(post("/edicoes/isbn").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("isbn", "9780140328721").param("title", "Rascunho").param("referencePaidPrice", "abc"))
                .andExpect(redirectedUrl("/edicoes/nova")).andReturn();
        mockMvc.perform(get("/edicoes/nova").with(auth).flashAttrs(invalid.getFlashMap())).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("abc")));
        org.mockito.Mockito.verify(isbnLookup, org.mockito.Mockito.times(1)).lookup("9780140328721");
        assertThat(editionRepository.count()).isZero();
    }

    @Test
    void tracksReadingPerEditionSeriesAndOverallWithSecureForms() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("reading@example.com", "hash", UserRole.USER));
        var other = userAccountRepository.save(UserAccount.create("foreign-reading@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var foreignAuth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(other));
        var series = comicSeriesRepository.save(ComicSeries.create(owner.getId(), null, "Coleção de leitura", null, null, null, null));
        var empty = comicSeriesRepository.save(ComicSeries.create(owner.getId(), null, "Coleção vazia", null, null, null, null));
        assertThat(readingService.overall(owner.getId()).percentageLabel()).isEqualTo("0,0%");
        var e = editionRepository.save(Edition.create(owner.getId(), null, series.getId(), "Leitura 1", null, null, null, null, null, null));
        for (int i = 2; i <= 3; i++) editionRepository.save(Edition.create(owner.getId(), null, series.getId(), "Leitura " + i, null, null, null, null, null, null));
        var digital = Edition.create(owner.getId(), null, null, "Digital sem série", null, null, null, null, null, null);
        digital.updatePricingAndMedia(br.com.minhagibiteca.catalog.domain.MediaType.DIGITAL, null, null);
        digital = editionRepository.save(digital);
        copyService.create(e.getId(), owner.getId(), new CopyService.Command(null, CopyCondition.GOOD, null, null, null, null, null));
        copyService.create(e.getId(), owner.getId(), new CopyService.Command(null, CopyCondition.GOOD, null, null, null, null, null));
        checklistService.create(series.getId(), owner.getId(), new br.com.minhagibiteca.collection.application.ChecklistService.Command("Especial", null, false, null));
        assertThat(editionService.viewOwned(e.getId(), owner.getId()).read()).isFalse();
        mockMvc.perform(post("/edicoes/{id}/leitura", e.getId()).with(auth).param("read", "true")).andExpect(status().isForbidden());
        mockMvc.perform(post("/edicoes/{id}/leitura", e.getId()).with(foreignAuth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("read", "true"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/edicoes/{id}/leitura", e.getId()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest());
        for (int i = 0; i < 2; i++) mockMvc.perform(post("/edicoes/{id}/leitura", e.getId()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("read", "true").param("fromCatalog", "true").param("returnTo", "/edicoes?q=Leitura&page=0"))
                .andExpect(redirectedUrl("/edicoes?q=Leitura&page=0"));
        assertThat(readingService.series(series.getId(), owner.getId()).label()).isEqualTo("33,3% — 1 de 3 edições lidas");
        assertThat(readingService.series(empty.getId(), owner.getId()).percentageLabel()).isEqualTo("0,0%");
        assertThat(readingService.overall(owner.getId()).label()).isEqualTo("25,0% — 1 de 4 edições lidas");
        assertThatThrownBy(() -> readingService.series(series.getId(), other.getId())).isInstanceOf(br.com.minhagibiteca.shared.application.ResourceNotFoundException.class);
        var preview = java.nio.file.Path.of("target/ui-reading"); java.nio.file.Files.createDirectories(preview.resolve("css"));
        for (String css : List.of("main.css", "dashboard.css")) java.nio.file.Files.copy(java.nio.file.Path.of("src/main/resources/static/css", css), preview.resolve("css").resolve(css), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        for (String path : List.of("/dashboard", "/series", "/series/" + series.getId() + "/checklist", "/edicoes", "/edicoes/" + e.getId())) {
            var result = mockMvc.perform(get(path).with(auth)).andExpect(status().isOk()).andReturn();
            var dir = preview.resolve(path.substring(1)); java.nio.file.Files.createDirectories(dir);
            java.nio.file.Files.writeString(dir.resolve("index.html"), result.getResponse().getContentAsString());
        }
        mockMvc.perform(get("/series/{id}/faltantes", series.getId()).with(auth)).andExpect(content().string(org.hamcrest.Matchers.containsString("33,3%")));
        mockMvc.perform(post("/edicoes/{id}/editar", e.getId()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("title", "Leitura editada").param("seriesId", empty.getId().toString()))
                .andExpect(redirectedUrl("/edicoes/" + e.getId()));
        assertThat(editionService.viewOwned(e.getId(), owner.getId()).read()).isTrue();
        assertThat(readingService.series(series.getId(), owner.getId()).label()).isEqualTo("0,0% — 0 de 2 edições lidas");
        assertThat(readingService.series(empty.getId(), owner.getId()).percentageLabel()).isEqualTo("100,0%");
        mockMvc.perform(post("/edicoes/{id}/leitura", e.getId()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("read", "false").param("returnTo", "https://example.com"))
                .andExpect(redirectedUrl("/edicoes/" + e.getId()));
        readingService.mark(digital.getId(), owner.getId(), true);
        assertThat(readingService.overall(owner.getId()).percentageLabel()).isEqualTo("25,0%");
        editionService.delete(digital.getId(), owner.getId());
        assertThat(readingService.overall(owner.getId()).label()).isEqualTo("0,0% — 0 de 3 edições lidas");
    }

    @Test
    void aggregatesReadingBeyondCatalogAndSeriesPagination() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("reading-pages@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var series = comicSeriesRepository.save(ComicSeries.create(owner.getId(), null, "A série", null, null, null, null));
        for (int i = 0; i < 21; i++) {
            var e = Edition.create(owner.getId(), null, series.getId(), "HQ " + i, null, null, null, null, null, null);
            e.markRead(i < 7); editionRepository.save(e);
            if (i < 20) comicSeriesRepository.save(ComicSeries.create(owner.getId(), null, "Z série " + i, null, null, null, null));
        }
        var foreign = userAccountRepository.save(UserAccount.create("reading-pages-foreign@example.com", "hash", UserRole.USER));
        var e = Edition.create(foreign.getId(), null, null, "Privada", null, null, null, null, null, null); e.markRead(true); editionRepository.save(e);
        assertThat(readingService.overall(owner.getId()).label()).isEqualTo("33,3% — 7 de 21 edições lidas");
        mockMvc.perform(get("/series").with(auth)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("7 de 21 edições lidas")));
        mockMvc.perform(get("/series?page=1").with(auth)).andExpect(status().isOk());
        mockMvc.perform(get("/edicoes?page=1").with(auth)).andExpect(status().isOk());
    }

    @Test
    void summarizesCollectionWithoutDuplicatingPricesAndRefreshesAfterChanges() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("summary@example.com", "hash", UserRole.USER));
        var other = userAccountRepository.save(UserAccount.create("other-summary@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var empty = (br.com.minhagibiteca.dashboard.application.CollectionSummary) mockMvc.perform(get("/visao-geral").with(auth))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel().get("summary");
        assertThat(empty.copies()).isZero(); assertThat(empty.editions()).isZero();
        assertThat(empty.totalPaidLabel()).contains("0,00");
        var edition = Edition.create(owner.getId(), null, null, "Coleção resumo", "1", null, null, null, null, null);
        edition.markRead(true);
        edition.updatePricingAndMedia(br.com.minhagibiteca.catalog.domain.MediaType.PHYSICAL, new java.math.BigDecimal("100.00"), new java.math.BigDecimal("999.00"));
        editionRepository.save(edition);
        var digital = Edition.create(owner.getId(), null, null, "Digital resumo", "2", null, null, null, null, null);
        digital.updatePricingAndMedia(br.com.minhagibiteca.catalog.domain.MediaType.DIGITAL, null, new java.math.BigDecimal("500.00"));
        editionRepository.save(digital);
        editionRepository.save(Edition.create(owner.getId(), null, null, "Sem suporte e preço", null, null, null, null, null, null));
        var catalogOnly = (br.com.minhagibiteca.dashboard.application.CollectionSummary) mockMvc.perform(get("/visao-geral").with(auth))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("1.499,00")))
                .andReturn().getModelAndView().getModel().get("summary");
        assertThat(catalogOnly.copies()).isZero(); assertThat(catalogOnly.physicalEditions()).isEqualTo(1);
        assertThat(catalogOnly.digitalEditions()).isEqualTo(1); assertThat(catalogOnly.unknownMediaEditions()).isEqualTo(1);
        assertThat(catalogOnly.pricedEditions()).isEqualTo(2); assertThat(catalogOnly.unpricedEditions()).isEqualTo(1);
        var foreign = editionRepository.save(Edition.create(other.getId(), null, null, "Privado", null, null, null, null, null, null));
        copyService.create(foreign.getId(), other.getId(), new CopyService.Command(null, CopyCondition.GOOD, null, null, null, new java.math.BigDecimal("1000.00"), null));
        var unknown = copyService.create(edition.getId(), owner.getId(), new CopyService.Command(null, CopyCondition.GOOD, null, null, null, null, null));
        var allUnknown = (br.com.minhagibiteca.dashboard.application.CollectionSummary) mockMvc.perform(get("/visao-geral").with(auth))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Não informado")))
                .andReturn().getModelAndView().getModel().get("summary");
        assertThat(allUnknown.totalPaid()).isNull(); assertThat(allUnknown.unpricedCopies()).isEqualTo(1);
        var paid = copyService.create(edition.getId(), owner.getId(), new CopyService.Command(null, CopyCondition.GOOD, null, null, null, new java.math.BigDecimal("10.10"), null));
        copyService.create(edition.getId(), owner.getId(), new CopyService.Command(null, CopyCondition.GOOD, null, null, null, new java.math.BigDecimal("20.20"), null));
        copyService.create(edition.getId(), owner.getId(), new CopyService.Command(null, CopyCondition.GOOD, null, null, null, java.math.BigDecimal.ZERO, null));
        var result = mockMvc.perform(get("/visao-geral").with(auth)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("30,30")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("pode estar incompleto"))).andReturn();
        var summary = (br.com.minhagibiteca.dashboard.application.CollectionSummary) result.getModelAndView().getModel().get("summary");
        String overviewHtml = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(overviewHtml).contains("33,3%", "Suporte não informado", "Gerencie seu acervo", "Cadastros de apoio")
                .doesNotContain("Sua coleção começa com a primeira edição", "collection-hero");
        for (String id : List.of("summary-editions", "summary-physical", "summary-digital", "summary-copies", "summary-owned-editions", "summary-total", "summary-copy-total"))
            assertThat(overviewHtml).contains("id=\"" + id + "\"");
        assertThat(overviewHtml.indexOf("pode estar incompleto")).isLessThan(overviewHtml.indexOf("id=\"home-calculation\""));
        assertThat(summary.editions()).isEqualTo(3); assertThat(summary.copies()).isEqualTo(4);
        assertThat(summary.registeredPaid()).isEqualByComparingTo("1499.00");
        assertThat(summary.physicalEditions()).isEqualTo(1); assertThat(summary.digitalEditions()).isEqualTo(1);
        assertThat(summary.ownedEditions()).isEqualTo(1); assertThat(summary.pricedCopies()).isEqualTo(3);
        assertThat(summary.totalPaid()).isEqualByComparingTo("30.30");
        var preview = java.nio.file.Path.of("target/ui-home"); java.nio.file.Files.createDirectories(preview.resolve("css"));
        java.nio.file.Files.writeString(preview.resolve("index.html"), result.getResponse().getContentAsString());
        for (String css : List.of("main.css", "dashboard.css")) java.nio.file.Files.copy(java.nio.file.Path.of("src/main/resources/static/css", css), preview.resolve("css").resolve(css), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        copyService.update(paid, owner.getId(), new CopyService.Command(null, CopyCondition.GOOD, null, null, null, new java.math.BigDecimal("15.15"), null));
        copyService.delete(unknown, owner.getId());
        var refreshed = (br.com.minhagibiteca.dashboard.application.CollectionSummary) mockMvc.perform(get("/visao-geral").with(auth))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel().get("summary");
        assertThat(refreshed.copies()).isEqualTo(3); assertThat(refreshed.unpricedCopies()).isZero();
        assertThat(refreshed.totalPaid()).isEqualByComparingTo("35.35");
        assertThat(refreshed.registeredPaid()).isEqualByComparingTo("1499.00");
        mockMvc.perform(get("/visao-geral")).andExpect(status().is3xxRedirection());
    }

    @Test
    void refreshesRegisteredPricesAndMediaAfterEditionFormsWithoutCopies() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("edition-summary@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        mockMvc.perform(post("/edicoes").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("title", "HQ financeira").param("mediaType", "PHYSICAL").param("referencePaidPrice", "29,90").param("coverPrice", "99,00"))
                .andExpect(status().is3xxRedirection());
        var id = editionRepository.findAll().getFirst().getId();
        var initial = (br.com.minhagibiteca.dashboard.application.CollectionSummary) mockMvc.perform(get("/visao-geral").with(auth))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("29,90")))
                .andReturn().getModelAndView().getModel().get("summary");
        assertThat(initial.physicalEditions()).isEqualTo(1); assertThat(initial.digitalEditions()).isZero();
        assertThat(initial.copies()).isZero(); assertThat(initial.registeredPaid()).isEqualByComparingTo("29.90");
        mockMvc.perform(post("/edicoes/{id}/editar", id).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("title", "HQ financeira").param("mediaType", "DIGITAL").param("referencePaidPrice", "0,00"))
                .andExpect(redirectedUrl("/edicoes/" + id));
        var digital = (br.com.minhagibiteca.dashboard.application.CollectionSummary) mockMvc.perform(get("/visao-geral").with(auth))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel().get("summary");
        assertThat(digital.physicalEditions()).isZero(); assertThat(digital.digitalEditions()).isEqualTo(1);
        assertThat(digital.pricedEditions()).isEqualTo(1); assertThat(digital.registeredPaid()).isEqualByComparingTo("0");
        mockMvc.perform(post("/edicoes/{id}/editar", id).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("title", "HQ financeira").param("mediaType", "DIGITAL").param("referencePaidPrice", ""))
                .andExpect(redirectedUrl("/edicoes/" + id));
        var unknown = (br.com.minhagibiteca.dashboard.application.CollectionSummary) mockMvc.perform(get("/visao-geral").with(auth))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel().get("summary");
        assertThat(unknown.registeredPaid()).isNull(); assertThat(unknown.registeredPaidLabel()).isEqualTo("Não informado");
        assertThat(unknown.unpricedEditions()).isEqualTo(1);
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
            var summary = (br.com.minhagibiteca.dashboard.application.CollectionSummary) mockMvc.perform(get("/visao-geral").with(auth))
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
        var updated = (br.com.minhagibiteca.dashboard.application.CollectionSummary) mockMvc.perform(get("/visao-geral").with(auth))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("36,25")))
                .andReturn().getModelAndView().getModel().get("summary");
        assertThat(updated.copies()).isEqualTo(2); assertThat(updated.totalPaid()).isEqualByComparingTo("36.25");
        mockMvc.perform(post("/exemplares/{id}/editar", id).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("condition", "GOOD").param("acquisitionPrice", "-1,00"))
                .andExpect(status().isOk()).andExpect(view().name("exemplares/form"));
        var unchanged = (br.com.minhagibiteca.dashboard.application.CollectionSummary) mockMvc.perform(get("/visao-geral").with(auth))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel().get("summary");
        assertThat(unchanged).isEqualTo(updated);
        mockMvc.perform(post("/exemplares/{id}/editar", id).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("condition", "GOOD").param("acquisitionPrice", ""))
                .andExpect(redirectedUrl("/edicoes/" + edition.getId()));
        var removedPrice = (br.com.minhagibiteca.dashboard.application.CollectionSummary) mockMvc.perform(get("/visao-geral").with(auth))
                .andExpect(status().isOk()).andReturn().getModelAndView().getModel().get("summary");
        assertThat(removedPrice.copies()).isEqualTo(2); assertThat(removedPrice.pricedCopies()).isEqualTo(1);
        assertThat(removedPrice.unpricedCopies()).isEqualTo(1); assertThat(removedPrice.totalPaid()).isEqualByComparingTo("10.50");
    }

    @Test
    void rendersOverviewWithSearchModuleLinksAndSecureLogout() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("home-preview@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        String html = null;
        for (String path : List.of("/visao-geral")) {
            html = mockMvc.perform(get(path).with(auth)).andExpect(status().isOk()).andExpect(view().name("dashboard/index"))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("/css/dashboard.css")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Pular para o conteúdo")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Visão geral da coleção")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/logout\" method=\"post\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"q\"")))
                    .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            for (String destination : List.of("edicoes", "dashboard", "series", "localizacoes", "editoras", "obras", "criadores"))
                assertThat(html).contains("href=\"/" + destination + "\"");
            assertThat(html).contains("href=\"/edicoes/nova?returnTo=/visao-geral\"");
            assertThat(html).contains("Sua coleção começa com a primeira edição", "Entenda os números e os valores", "Cadastros de apoio")
                    .doesNotContain("collection-hero", "hero-art", "pode estar incompleto");
            assertThat(html.indexOf("id=\"home-actions\"")).isLessThan(html.indexOf("id=\"home-overview\""));
            assertThat(html.indexOf("id=\"home-overview\"")).isLessThan(html.indexOf("id=\"home-collection\""));
            assertThat(html.indexOf("id=\"home-collection\"")).isLessThan(html.indexOf("id=\"home-support\""));
            assertThat(html).contains("<details id=\"home-calculation\"", "<progress max=\"100\" value=\"0.0\"");
        }
        mockMvc.perform(get("/visao-geral")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(post("/logout").with(auth)).andExpect(status().isForbidden());
        mockMvc.perform(post("/logout").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(redirectedUrl("/login?logout"));
        // Prévia local do HTML realmente renderizado; só contém conteúdo estático e sessão de teste.
        var preview = java.nio.file.Path.of("target/ui-home");
        java.nio.file.Files.createDirectories(preview.resolve("css"));
        java.nio.file.Files.writeString(preview.resolve("empty.html"), html, java.nio.charset.StandardCharsets.UTF_8);
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
        var paths = List.of("/dashboard", "/visao-geral", "/edicoes", "/edicoes/nova", "/edicoes/" + edition.getId(), "/edicoes/" + edition.getId() + "/editar",
                "/edicoes/" + edition.getId() + "/exemplares/novo", "/exemplares/" + copy + "/editar", "/exemplares/" + copy + "/excluir",
                "/series", "/series/nova", "/series/" + series.getId() + "/editar", "/series/" + series.getId() + "/checklist", "/series/" + series.getId() + "/faltantes",
                "/checklist/itens/" + item + "/editar", "/checklist/itens/" + item + "/excluir", "/editoras", "/editoras/nova", "/editoras/" + publisher.getId() + "/editar",
                "/obras", "/obras/nova", "/obras/" + work.getId() + "/editar", "/criadores", "/criadores/novo", "/criadores/" + creator.getId() + "/editar",
                "/localizacoes", "/localizacoes/nova", "/localizacoes/" + location + "/editar", "/localizacoes/" + location + "/excluir", "/financeiro", "/personagens");
        var preview = java.nio.file.Path.of("target/ui-navigation");
        java.nio.file.Files.createDirectories(preview.resolve("css"));
        java.nio.file.Files.createDirectories(preview.resolve("js"));
        for (String js : List.of("sidebar.js", "home-banner.js")) java.nio.file.Files.copy(java.nio.file.Path.of("src/main/resources/static/js", js), preview.resolve("js").resolve(js), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        for (String css : List.of("main.css", "dashboard.css", "home-showcase.css")) java.nio.file.Files.copy(java.nio.file.Path.of("src/main/resources/static/css", css), preview.resolve("css").resolve(css), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        for (String path : paths) {
            String html = mockMvc.perform(get(path).with(auth)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            assertThat(html).contains("Navegação principal", "Pular para o conteúdo", "action=\"/logout\" method=\"post\"", "name=\"_csrf\"");
            assertThat(html).contains("id=\"sidebar-toggle\"", "type=\"button\" aria-controls=\"main-sidebar\" aria-expanded=\"true\">Ocultar menu",
                    "id=\"sidebar-controls\" class=\"sidebar-controls\" hidden", "src=\"/js/sidebar.js\"");
            assertThat(java.util.regex.Pattern.compile("<script[^>]*\\bdefer\\b[^>]*>").matcher(html).find()).isTrue();
            assertThat(html.split("id=\"main-sidebar\"", -1)).hasSize(2);
            assertThat(html.split("id=\"sidebar-toggle\"", -1)).hasSize(2);
            assertThat(html.indexOf("id=\"sidebar-controls\"")).isLessThan(html.indexOf("<aside id=\"main-sidebar\""));
            assertThat(html).contains("<aside id=\"main-sidebar\" class=\"dashboard-sidebar\" aria-label=\"Menu principal\">");
            assertThat(html.split("class=\"nav-current\"", -1)).hasSize(2);
            boolean inCatalog = List.of("/editoras", "/obras", "/criadores", "/personagens").stream().anyMatch(path::startsWith);
            boolean inOverview = List.of("/dashboard", "/visao-geral", "/financeiro").contains(path);
            var groups = List.of("nav-overview", "nav-collection", "nav-catalog");
            var labels = List.of("Painéis e análises", "Minha coleção", "Cadastros de apoio");
            var destinations = List.of(List.of("dashboard", "visao-geral", "financeiro"), List.of("edicoes", "series", "localizacoes"), List.of("obras", "criadores", "personagens", "editoras"));
            var navigation = java.util.regex.Pattern.compile("<nav class=\"dashboard-navigation\".*?</nav>", java.util.regex.Pattern.DOTALL).matcher(html);
            assertThat(navigation.find()).isTrue();
            assertThat(navigation.group().split("aria-current=\"page\"", -1)).hasSize(2);
            int previousGroup = -1;
            for (int i = 0; i < groups.size(); i++) {
                var group = java.util.regex.Pattern.compile("<details[^>]*id=\"" + groups.get(i) + "\"[^>]*>.*?</details>", java.util.regex.Pattern.DOTALL).matcher(navigation.group());
                assertThat(group.find()).as("%s em %s", groups.get(i), path).isTrue();
                assertThat(group.start()).isGreaterThan(previousGroup);
                previousGroup = group.start();
                assertThat(group.group().contains("open=\"open\"")).isEqualTo(i == 0 ? inOverview : i == 1 ? !inCatalog && !inOverview : inCatalog);
                assertThat(group.group()).contains("<summary class=\"nav-group-toggle\">" + labels.get(i) + "</summary>");
                int previousLink = -1;
                for (String destination : destinations.get(i)) {
                    String href = "href=\"/" + destination + "\"";
                    assertThat(navigation.group().split(java.util.regex.Pattern.quote(href), -1)).hasSize(2);
                    assertThat(group.group().indexOf(href)).isGreaterThan(previousLink);
                    previousLink = group.group().indexOf(href);
                }
            }
            if (!path.equals("/dashboard") && !path.equals("/visao-geral")) assertThat(html).contains("Você está aqui", "navigation-back");
            var directory = preview.resolve(path.substring(1)); java.nio.file.Files.createDirectories(directory);
            java.nio.file.Files.writeString(directory.resolve("index.html"), html, java.nio.charset.StandardCharsets.UTF_8);
        }
        mockMvc.perform(get("/edicoes/{id}/editar", edition.getId()).with(auth)).andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/edicoes/" + edition.getId() + "\">Cancelar")));
        mockMvc.perform(get("/exemplares/{id}/editar", copy).with(auth)).andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/edicoes/" + edition.getId() + "\">Cancelar")));
        mockMvc.perform(get("/login")).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Navegação principal"))));
        mockMvc.perform(get("/js/sidebar.js")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("toggle.setAttribute('aria-expanded'")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("localStorage.setItem")));
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
                var row = sql.executeQuery("select title, editorial_number, media_type, cover_price, reference_paid_price, is_read from pricing_upgrade_test.editions")) {
            assertThat(row.next()).isTrue();
            assertThat(row.getString("title")).isEqualTo("Anterior");
            assertThat(row.getString("editorial_number")).isEqualTo("1A");
            assertThat(row.getString("media_type")).isNull();
            assertThat(row.getBigDecimal("cover_price")).isNull();
            assertThat(row.getBigDecimal("reference_paid_price")).isNull();
            assertThat(row.getBoolean("is_read")).isFalse();
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
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Exibindo 1–20")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"edition-filter-details\" open")))
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
    void rendersCatalogCardsWithMetadataPricesReadingAndSafeActions() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("catalog-layout@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var physical = editionService.create(owner.getId(), new EditionService.Command(null, null,
                "A aventura do colecionador", "1A", "Capa alternativa", "9786559601325", 2026, "Encadernado", null,
                br.com.minhagibiteca.catalog.domain.MediaType.PHYSICAL, new BigDecimal("59.90"), new BigDecimal("39.50")));
        editionService.create(owner.getId(), new EditionService.Command(null, null,
                "Biblioteca digital", "Especial", null, null, 2025, "Digital", null,
                br.com.minhagibiteca.catalog.domain.MediaType.DIGITAL, null, BigDecimal.ZERO));
        editionService.create(owner.getId(), new EditionService.Command(null, null,
                "Coleção com título extenso para conferir a quebra de linhas em telas pequenas", null, null, null, null, null, null));
        var escaped = editionService.create(owner.getId(), new EditionService.Command(null, null,
                "<script>alert('teste')</script>", null, null, null, null, null, null));
        editionCoverService.replace(escaped.getId(), owner.getId(), new MockMultipartFile("cover", "capa.png", "image/png",
                Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=")));
        mockMvc.perform(post("/edicoes/{id}/leitura", physical.getId()).with(auth)
                .with(SecurityMockMvcRequestPostProcessors.csrf()).param("read", "true").param("fromCatalog", "true"))
                .andExpect(status().is3xxRedirection());
        var result = mockMvc.perform(get("/edicoes").with(auth))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("loading=\"lazy\"")))
                .andReturn();
        var html = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(html.split("class=\"edition-card\"", -1)).hasSize(5);
        assertThat(html.split("class=\"edition-delete\"", -1)).hasSize(5);
        assertThat(html.split("name=\"_csrf\"", -1)).hasSize(10); // logout + leitura/exclusão por cartão
        assertThat(html).doesNotContain("class=\"edition-filter-details\" open", "class=\"edition-delete\" open");
        assertThat(html).contains("4 edições encontradas", "Exibindo 1–4", "Nº 1A", "2026", "Encadernado",
                "Variante: Capa alternativa", "ISBN: 9786559601325", "39,50", "59,90", "0,00",
                "Não informado", "Já li", "Não li", "Sem<br>capa", "Valor pago da edição", "Preço de capa",
                "&lt;script&gt;", "/edicoes/" + escaped.getId() + "/capa")
                .doesNotContain("<script>alert(");
        var preview = java.nio.file.Path.of("target/ui-catalog");
        java.nio.file.Files.createDirectories(preview.resolve("css"));
        java.nio.file.Files.writeString(preview.resolve("index.html"), html);
        var coverDir = preview.resolve("edicoes").resolve(escaped.getId().toString());
        java.nio.file.Files.createDirectories(coverDir);
        try (var input = editionCoverService.load(escaped.getId(), owner.getId()).resource().getInputStream()) {
            java.nio.file.Files.copy(input, coverDir.resolve("capa"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        for (String css : List.of("main.css", "dashboard.css"))
            java.nio.file.Files.copy(java.nio.file.Path.of("src/main/resources/static/css", css),
                    preview.resolve("css").resolve(css), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    void distinguishesEmptyCatalogFromEmptySearchAndSelectedFilters() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("empty-catalog-layout@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var empty = mockMvc.perform(get("/edicoes").with(auth)).andExpect(status().isOk()).andReturn();
        assertThat(empty.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .contains("Seu catálogo começa aqui", "Cadastrar primeira edição", "0 edições encontradas");
        var filtered = mockMvc.perform(get("/edicoes").with(auth).param("q", "inexistente").param("mediaType", "DIGITAL"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"edition-filter-details\" open")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"DIGITAL\" selected")))
                .andReturn();
        assertThat(filtered.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .contains("Nenhuma edição nesta consulta", "Filtros ativos", "Ver todo o catálogo")
                .doesNotContain("Cadastrar primeira edição");
        var preview = java.nio.file.Path.of("target/ui-catalog");
        java.nio.file.Files.createDirectories(preview);
        java.nio.file.Files.writeString(preview.resolve("empty.html"), empty.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        java.nio.file.Files.writeString(preview.resolve("filtered.html"), filtered.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
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

    @Test
    void explainsReferencedCatalogDeletionAndPreservesEditions() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("delete-links@example.com", "hash", UserRole.USER));
        var other = userAccountRepository.save(UserAccount.create("delete-links-other@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var publisher = publisherRepository.save(Publisher.create(owner.getId(), "Mythos", null, null));
        var edition = editionService.create(owner.getId(), new EditionService.Command(publisher.getId(), null, "HQ preservada", null, null, null, null, null, null));
        var work = workService.create(owner.getId(), new WorkService.Command("Obra preservada", null, null));
        var creator = creatorService.create(owner.getId(), new CreatorService.Command("Criador preservado", null));
        editionAssociationService.replace(edition.getId(), owner.getId(), List.of(work.getId()), List.of(creator.getId()));
        var paths = List.of("/editoras", "/obras", "/criadores");
        var ids = List.of(publisher.getId(), work.getId(), creator.getId());
        for (int i = 0; i < paths.size(); i++) {
            mockMvc.perform(post(paths.get(i) + "/{id}/excluir", ids.get(i)).with(auth)).andExpect(status().isForbidden());
            mockMvc.perform(post(paths.get(i) + "/{id}/excluir", ids.get(i))
                    .with(SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(other)))
                    .with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isNotFound());
            var result = mockMvc.perform(post(paths.get(i) + "/{id}/excluir", ids.get(i)).with(auth)
                    .with(SecurityMockMvcRequestPostProcessors.csrf()))
                    .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(paths.get(i))).andReturn();
            assertThat(result.getFlashMap().get("errorMessage").toString()).contains("vinculad", "Nenhuma HQ foi apagada");
            mockMvc.perform(get(paths.get(i)).with(auth).flashAttrs(result.getFlashMap()))
                    .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("role=\"alert\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Nenhuma HQ foi apagada")));
        }
        assertThat(editionRepository.existsById(edition.getId())).isTrue();
        assertThat(publisherRepository.existsById(publisher.getId())).isTrue();
        assertThat(workRepository.existsById(work.getId())).isTrue();
        assertThat(creatorRepository.existsById(creator.getId())).isTrue();
        editionAssociationService.replace(edition.getId(), owner.getId(), List.of(), List.of());
        editionService.update(edition.getId(), owner.getId(), new EditionService.Command(null, null, "HQ preservada", null, null, null, null, null, null));
        for (int i = 0; i < paths.size(); i++) {
            var result = mockMvc.perform(post(paths.get(i) + "/{id}/excluir", ids.get(i)).with(auth)
                    .with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(redirectedUrl(paths.get(i))).andReturn();
            assertThat(result.getFlashMap().containsKey("successMessage")).isTrue();
            assertThat(result.getFlashMap().containsKey("errorMessage")).isFalse();
        }
        assertThat(publisherRepository.existsById(publisher.getId())).isFalse();
        assertThat(workRepository.existsById(work.getId())).isFalse();
        assertThat(creatorRepository.existsById(creator.getId())).isFalse();
        assertThat(editionRepository.existsById(edition.getId())).isTrue();
    }

    @Test
    void blocksPublisherReferencedOnlyBySeriesAndEnforcesDeletionSecurity() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("delete-series@example.com", "hash", UserRole.USER));
        var other = userAccountRepository.save(UserAccount.create("delete-other@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var publisher = publisherRepository.save(Publisher.create(owner.getId(), "Editora da série", null, null));
        var series = comicSeriesRepository.save(ComicSeries.create(owner.getId(), publisher.getId(), "Série preservada", null, null, null, null));
        mockMvc.perform(post("/editoras/{id}/excluir", publisher.getId()).with(auth)).andExpect(status().isForbidden());
        mockMvc.perform(post("/editoras/{id}/excluir", publisher.getId())
                .with(SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(other)))
                .with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isNotFound());
        var result = mockMvc.perform(post("/editoras/{id}/excluir", publisher.getId()).with(auth)
                .with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(redirectedUrl("/editoras")).andReturn();
        assertThat(result.getFlashMap().containsKey("errorMessage")).isTrue();
        assertThat(comicSeriesRepository.existsById(series.getId())).isTrue();
        assertThat(publisherRepository.existsById(publisher.getId())).isTrue();
        seriesService.update(series.getId(), owner.getId(), new br.com.minhagibiteca.catalog.application.ComicSeriesService.Command(null, "Série preservada", null, null, null, null));
        mockMvc.perform(post("/editoras/{id}/excluir", publisher.getId()).with(auth)
                .with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(redirectedUrl("/editoras"));
        assertThat(publisherRepository.existsById(publisher.getId())).isFalse();
        assertThat(comicSeriesRepository.existsById(series.getId())).isTrue();
    }

    @Test
    void rendersAllInterfaceLayoutsWithLongNamesAndValidationErrors() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("ui-review@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var pages = new java.util.LinkedHashMap<String, String>();
        // Include empty states before seeding fixtures. Never use the running application's database.
        for (var path : List.of("/", "/visao-geral", "/edicoes", "/editoras", "/series", "/obras", "/criadores", "/personagens", "/localizacoes", "/financeiro")) {
            pages.put((path.equals("/") ? "inicio" : path.substring(1)) + "-vazio", mockMvc.perform(get(path).with(auth)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        }
        var longName = "Coleção de aventuras brasileiras — edição especial de colecionador com descrição extensa";
        var publisher = publisherRepository.save(Publisher.create(owner.getId(), longName, "BR", "Observações da editora"));
        var series = comicSeriesRepository.save(ComicSeries.create(owner.getId(), publisher.getId(), longName, 2000, 2026, null, null));
        var work = workService.create(owner.getId(), new WorkService.Command(longName, 2000, "Resumo da obra"));
        var creator = creatorService.create(owner.getId(), new CreatorService.Command(longName, null));
        var character = characterService.save(owner.getId(), null, longName);
        var edition = editionService.create(owner.getId(), new EditionService.Command(publisher.getId(), series.getId(), longName, "1", "Especial", null, 2026, "Capa dura", "Observações da edição", br.com.minhagibiteca.catalog.domain.MediaType.PHYSICAL, new BigDecimal("129.90"), new BigDecimal("99.90"), java.time.LocalDate.now()));
        editionAssociationService.replace(edition.getId(), owner.getId(), List.of(work.getId()), List.of(creator.getId()));
        characterService.replace(owner.getId(), edition.getId(), List.of(character));
        var location = locationService.create(owner.getId(), new br.com.minhagibiteca.inventory.application.LocationService.Command(null, br.com.minhagibiteca.inventory.domain.LocationLevel.BOX, longName, null));
        var copy = copyService.create(edition.getId(), owner.getId(), new CopyService.Command(location, CopyCondition.values()[0], "Detalhes de conservação", "Loja de quadrinhos", java.time.LocalDate.now(), new BigDecimal("99.90"), "Observações do exemplar"));
        var item = checklistService.create(series.getId(), owner.getId(), new br.com.minhagibiteca.collection.application.ChecklistService.Command("1A", longName, false, "Observações do checklist"));
        var routes = new java.util.LinkedHashMap<String, String>();
        routes.put("inicio", "/");
        routes.put("visao-geral", "/visao-geral");
        routes.put("login", "/login");
        routes.put("login-erro", "/login?error");
        for (var entry : java.util.Map.of("edicoes", "nova", "editoras", "nova", "series", "nova", "obras", "nova", "criadores", "novo", "personagens", "novo", "localizacoes", "nova").entrySet()) {
            routes.put(entry.getKey(), "/" + entry.getKey());
            routes.put(entry.getKey() + "-novo", "/" + entry.getKey() + "/" + entry.getValue());
        }
        var ids = java.util.Map.of("edicoes", edition.getId(), "editoras", publisher.getId(), "series", series.getId(), "obras", work.getId(), "criadores", creator.getId(), "personagens", character, "localizacoes", location, "exemplares", copy);
        ids.forEach((name, id) -> routes.put(name + "-editar", "/" + name + "/" + id + "/editar"));
        routes.put("edicao-detalhe", "/edicoes/" + edition.getId());
        routes.put("exemplar-novo", "/edicoes/" + edition.getId() + "/exemplares/novo");
        routes.put("exemplar-excluir", "/exemplares/" + copy + "/excluir");
        routes.put("localizacao-excluir", "/localizacoes/" + location + "/excluir");
        routes.put("personagem-excluir", "/personagens/" + character + "/excluir");
        routes.put("checklist", "/series/" + series.getId() + "/checklist");
        routes.put("faltantes", "/series/" + series.getId() + "/faltantes");
        routes.put("checklist-editar", "/checklist/itens/" + item + "/editar");
        routes.put("checklist-excluir", "/checklist/itens/" + item + "/excluir");
        routes.put("financeiro", "/financeiro?start=" + java.time.LocalDate.now().minusDays(30) + "&end=" + java.time.LocalDate.now());
        for (var entry : routes.entrySet()) {
            var html = mockMvc.perform(get(entry.getValue()).with(auth)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(html).contains("lang=\"pt-BR\"", "name=\"viewport\"", "/css/main.css", "/css/dashboard.css");
            pages.put(entry.getKey(), html);
        }
        pages.put("edicao-erros", mockMvc.perform(post("/edicoes").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("title", "").param("referencePaidPrice", "inválido")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        pages.put("financeiro-invalido", mockMvc.perform(get("/financeiro?start=2026-01-01").with(auth)).andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString());
        pages.put("erro-404", mockMvc.perform(get("/edicoes/" + java.util.UUID.randomUUID()).with(auth)).andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString());
        pages.put("erro-400", mockMvc.perform(get("/edicoes/uuid-invalido").with(auth)).andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString());
        if (Boolean.getBoolean("ui.review.export")) {
            var root = java.nio.file.Path.of("target", "ui-review");
            java.nio.file.Files.createDirectories(root);
            for (var asset : List.of("css/main.css", "css/dashboard.css", "css/home-showcase.css", "js/sidebar.js", "js/home-banner.js")) {
                var target = root.resolve(asset);
                java.nio.file.Files.createDirectories(target.getParent());
                java.nio.file.Files.copy(java.nio.file.Path.of("src/main/resources/static").resolve(asset), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            var index = new StringBuilder("<!doctype html><html lang=\"pt-BR\"><meta charset=\"UTF-8\"><title>Revisão isolada da interface</title><h1>Páginas renderizadas com dados fictícios</h1><ul>");
            for (var entry : pages.entrySet()) {
                java.nio.file.Files.writeString(root.resolve(entry.getKey() + ".html"), entry.getValue());
                index.append("<li><a href=\"").append(entry.getKey()).append(".html\">").append(entry.getKey()).append("</a></li>");
            }
            java.nio.file.Files.writeString(root.resolve("index.html"), index.append("</ul></html>").toString());
        }
    }

    @Test
    void separatesVisualHomeFromOverviewAndShowsOnlyBoundedOwnedShelves() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("visual-home@example.com", "hash", UserRole.USER));
        var other = userAccountRepository.save(UserAccount.create("foreign-home@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        for (var path : List.of("/", "/dashboard", "/visao-geral")) {
            mockMvc.perform(get(path)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
        }
        var empty = mockMvc.perform(get("/").with(auth)).andExpect(view().name("dashboard/home")).andReturn();
        assertThat(((br.com.minhagibiteca.dashboard.application.CollectionSummary) empty.getModelAndView().getModel().get("bannerSummary")).editions()).isZero();
        assertThat(((br.com.minhagibiteca.collection.application.ReadingProgress) empty.getModelAndView().getModel().get("bannerReading")).total()).isZero();
        var previews = new java.util.LinkedHashMap<String, String>();
        previews.put("inicio-vazio", empty.getResponse().getContentAsString());
        assertThat(empty.getResponse().getContentAsString()).contains("Toda grande coleção começa com uma história.", "Visão geral").doesNotContain("id=\"home-overview\"", "class=\"showcase-rail\"");
        var foreign = Edition.create(other.getId(), null, null, "SEGREDO OUTRO USUÁRIO", null, null, null, null, null, null);
        foreign.updatePricingAndMedia(br.com.minhagibiteca.catalog.domain.MediaType.DIGITAL, null, new BigDecimal("9999.99"));
        foreign.markRead(true);
        editionRepository.save(foreign);
        var owned = new java.util.ArrayList<Edition>();
        for (int i = 0; i < 30; i++) {
            var title = i == 29 ? "A extraordinária jornada dos guardiões da biblioteca através dos universos dos quadrinhos ".repeat(2).strip() : "Minha história " + i;
            var edition = Edition.create(owner.getId(), null, null, title, Integer.toString(i), null, null, 2026, "Capa dura", null);
            edition.updatePricingAndMedia(i % 2 == 0 ? br.com.minhagibiteca.catalog.domain.MediaType.PHYSICAL : br.com.minhagibiteca.catalog.domain.MediaType.DIGITAL, null, i == 0 ? new BigDecimal("25.50") : null);
            edition.markRead(i % 3 == 0);
            if (i == 0) edition.updatePages(100, 100);
            if (i == 1) edition.updatePages(200, 40);
            owned.add(editionRepository.save(edition));
        }
        var jdbc = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        for (int i = 0; i < owned.size(); i++) jdbc.update("update editions set created_at = ? where id = ?", java.sql.Timestamp.from(java.time.Instant.parse("2026-01-01T00:00:00Z").plusSeconds(i)), owned.get(i).getId());
        var coverBytes = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
        editionCoverService.replace(owned.get(29).getId(), owner.getId(), new MockMultipartFile("cover", "capa.png", "image/png", coverBytes));
        for (var path : List.of("/", "/dashboard")) {
            var result = mockMvc.perform(get(path).with(auth)).andExpect(status().isOk()).andExpect(view().name("dashboard/home")).andReturn();
            var showcase = (br.com.minhagibiteca.dashboard.application.HomeShowcase) result.getModelAndView().getModel().get("showcase");
            assertThat(result.getModelAndView().getModel()).doesNotContainKeys("summary", "readingProgress");
            var bannerSummary = (br.com.minhagibiteca.dashboard.application.CollectionSummary) result.getModelAndView().getModel().get("bannerSummary");
            var bannerReading = (br.com.minhagibiteca.collection.application.ReadingProgress) result.getModelAndView().getModel().get("bannerReading");
            assertThat(bannerSummary.editions()).isEqualTo(30);
            assertThat(bannerSummary.physicalEditions()).isEqualTo(15);
            assertThat(bannerSummary.digitalEditions()).isEqualTo(15);
            assertThat(bannerSummary.registeredPaid()).isEqualByComparingTo("25.50");
            assertThat(bannerSummary.unpricedEditions()).isEqualTo(29);
            assertThat(bannerReading.total()).isEqualTo(30);
            assertThat(bannerReading.read()).isEqualTo(10);
            assertThat(bannerReading.totalPages()).isEqualTo(300);
            assertThat(bannerReading.pagesRead()).isEqualTo(140);
            assertThat(bannerReading.knownPageEditions()).isEqualTo(2);
            assertThat(showcase.featured().id()).isEqualTo(owned.get(29).getId());
            assertThat(showcase.featured().hasCover()).isTrue();
            assertThat(showcase.shelves()).hasSize(5);
            assertThat(showcase.shelves().getFirst().editions()).extracting(e -> e.id()).containsExactlyElementsOf(owned.subList(18, 30).reversed().stream().map(Edition::getId).toList());
            for (var shelf : showcase.shelves()) {
                assertThat(shelf.editions()).hasSizeLessThanOrEqualTo(12);
                assertThat(shelf.editions()).extracting(e -> e.id()).isSubsetOf(owned.stream().map(Edition::getId).toList());
                if (shelf.category() == br.com.minhagibiteca.dashboard.application.HomeShelf.UNREAD) assertThat(shelf.editions()).allMatch(e -> !e.read());
                if (shelf.category() == br.com.minhagibiteca.dashboard.application.HomeShelf.READ) assertThat(shelf.editions()).allMatch(e -> e.read());
                if (shelf.category() == br.com.minhagibiteca.dashboard.application.HomeShelf.PHYSICAL) assertThat(shelf.editions()).allMatch(e -> e.media() == br.com.minhagibiteca.catalog.domain.MediaType.PHYSICAL);
                if (shelf.category() == br.com.minhagibiteca.dashboard.application.HomeShelf.DIGITAL) assertThat(shelf.editions()).allMatch(e -> e.media() == br.com.minhagibiteca.catalog.domain.MediaType.DIGITAL);
            }
            var html = result.getResponse().getContentAsString();
            assertThat(html).contains("id=\"home-banner\"", "id=\"banner-reading\"", "id=\"banner-collection\"", "id=\"banner-finance\"", "Sua jornada de leitura", "33,3%", bannerSummary.registeredPaidLabel(), "O total pode estar incompleto.", "data-banner-play", "aria-live=\"polite\"", "/js/home-banner.js", "href=\"/financeiro\"").doesNotContain("9.999,99");
            previews.put("inicio-com-capa", html);
            assertThat(html).contains("Seu universo de histórias", "Adicionados recentemente", "Sua próxima leitura", "Biblioteca digital", "Já lidos para revisitar", "tabindex=\"0\"", "loading=\"lazy\"", "returnTo=/dashboard", "/css/home-showcase.css", "/edicoes/" + owned.get(29).getId() + "/capa").doesNotContain("SEGREDO OUTRO USUÁRIO", "Sua coleção em números", "cover_file_name");
        }
        var overview = mockMvc.perform(get("/visao-geral").with(auth)).andExpect(view().name("dashboard/index")).andReturn();
        var summary = (br.com.minhagibiteca.dashboard.application.CollectionSummary) overview.getModelAndView().getModel().get("summary");
        assertThat(summary.editions()).isEqualTo(30);
        assertThat(overview.getResponse().getContentAsString()).contains("Visão geral da coleção", "Sua coleção em números", "Entenda os números e os valores");
        previews.put("visao-geral", overview.getResponse().getContentAsString());
        mockMvc.perform(get("/edicoes/{id}/capa", owned.get(29).getId()).with(SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(other)))).andExpect(status().isNotFound());
        mockMvc.perform(get("/edicoes/{id}/capa", owned.get(29).getId()).with(auth)).andExpect(status().isOk());
        readingService.mark(owned.get(29).getId(), owner.getId(), true);
        var updatedReading = (br.com.minhagibiteca.collection.application.ReadingProgress) mockMvc.perform(get("/dashboard").with(auth)).andReturn().getModelAndView().getModel().get("bannerReading");
        assertThat(updatedReading.read()).isEqualTo(11);
        var afterReading = (br.com.minhagibiteca.dashboard.application.HomeShowcase) mockMvc.perform(get("/dashboard").with(auth)).andReturn().getModelAndView().getModel().get("showcase");
        assertThat(afterReading.featured().id()).isEqualTo(owned.get(28).getId());
        assertThat(afterReading.shelves().stream().filter(s -> s.category() == br.com.minhagibiteca.dashboard.application.HomeShelf.UNREAD).findFirst().orElseThrow().editions()).noneMatch(e -> e.id().equals(owned.get(29).getId()));
        readingService.mark(owned.get(29).getId(), owner.getId(), false);
        var detail = mockMvc.perform(get("/edicoes/{id}?returnTo=/dashboard", owned.get(29).getId()).with(auth)).andExpect(status().isOk()).andReturn();
        assertThat(detail.getResponse().getContentAsString()).contains("← Voltar para Início");
        mockMvc.perform(post("/edicoes").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("title", "Nova história <script>").param("returnTo", "/dashboard")).andExpect(redirectedUrl("/dashboard"));
        var refreshed = mockMvc.perform(get("/dashboard").with(auth)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(refreshed).contains("Nova história &lt;script&gt;").doesNotContain("Nova história <script>");
        var refreshedBanner = (br.com.minhagibiteca.dashboard.application.CollectionSummary) mockMvc.perform(get("/dashboard").with(auth)).andReturn().getModelAndView().getModel().get("bannerSummary");
        assertThat(refreshedBanner.editions()).isEqualTo(31);
        assertThat(refreshedBanner.registeredPaid()).isEqualByComparingTo("25.50");
        previews.put("inicio-sem-capa", refreshed);
        var refreshedSummary = (br.com.minhagibiteca.dashboard.application.CollectionSummary) mockMvc.perform(get("/visao-geral").with(auth)).andReturn().getModelAndView().getModel().get("summary");
        assertThat(refreshedSummary.editions()).isEqualTo(31);
        if (Boolean.getBoolean("ui.review.export")) {
            var root = java.nio.file.Path.of("target", "ui-showcase");
            java.nio.file.Files.createDirectories(root);
            for (var asset : List.of("css/main.css", "css/dashboard.css", "css/home-showcase.css", "js/sidebar.js", "js/home-banner.js")) {
                java.nio.file.Files.createDirectories(root.resolve(asset).getParent());
                java.nio.file.Files.copy(java.nio.file.Path.of("src/main/resources/static").resolve(asset), root.resolve(asset), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            var cover = root.resolve("edicoes/" + owned.get(29).getId() + "/capa");
            java.nio.file.Files.createDirectories(cover.getParent());
            java.nio.file.Files.write(cover, coverBytes);
            var index = new StringBuilder("<!doctype html><html lang=\"pt-BR\"><meta charset=\"UTF-8\"><title>Prévia da vitrine</title><h1>Vitrine com dados fictícios</h1><ul>");
            for (var entry : previews.entrySet()) {
                java.nio.file.Files.writeString(root.resolve(entry.getKey() + ".html"), entry.getValue());
                index.append("<li><a href=\"").append(entry.getKey()).append(".html\">").append(entry.getKey()).append("</a></li>");
            }
            java.nio.file.Files.writeString(root.resolve("index.html"), index.append("</ul></html>").toString());
        }
    }

    @Test
    void registersPartialPageReadingWithValidationOwnershipAndUpdatedTotals() throws Exception {
        var owner = userAccountRepository.save(UserAccount.create("pages@example.com", "hash", UserRole.USER));
        var other = userAccountRepository.save(UserAccount.create("pages-other@example.com", "hash", UserRole.USER));
        var auth = SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        var series = comicSeriesRepository.save(ComicSeries.create(owner.getId(), null, "Páginas", null, null, null, null));
        mockMvc.perform(post("/edicoes").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                .param("title", "Leitura parcial").param("seriesId", series.getId().toString()).param("totalPages", "120").param("pagesRead", "30"))
                .andExpect(status().is3xxRedirection());
        var edition = editionRepository.findByOwnerUserIdAndNormalizedTitleContaining(owner.getId(), "leitura parcial", org.springframework.data.domain.Pageable.unpaged()).getContent().getFirst();
        assertThat(edition.getTotalPages()).isEqualTo(120); assertThat(edition.getPagesRead()).isEqualTo(30); assertThat(edition.isRead()).isFalse();
        var id = edition.getId();
        for (var input : List.of(new String[]{"10", "11"}, new String[]{"", "1"}, new String[]{"0", "0"}, new String[]{"100001", "0"}, new String[]{"100", "-1"}, new String[]{"100", "1.5"})) {
            mockMvc.perform(post("/edicoes").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())
                    .param("title", "Inválida").param("totalPages", input[0]).param("pagesRead", input[1])).andExpect(view().name("edicoes/form"));
        }
        assertThat(editionRepository.count()).isEqualTo(1);
        mockMvc.perform(get("/edicoes/{id}/editar", id).with(auth)).andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"120\"")));
        mockMvc.perform(post("/edicoes/{id}/paginas", id).with(auth).param("pagesRead", "60")).andExpect(status().isForbidden());
        mockMvc.perform(post("/edicoes/{id}/paginas", id).with(SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(other))).with(SecurityMockMvcRequestPostProcessors.csrf()).param("pagesRead", "60")).andExpect(status().isNotFound());
        for (var value : List.of("121", "-1", "", "1.5", "abc")) {
            mockMvc.perform(post("/edicoes/{id}/paginas", id).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("pagesRead", value))
                    .andExpect(redirectedUrl("/edicoes/" + id)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attributeExists("errorMessage"));
        }
        assertThat(editionService.viewOwned(id, owner.getId()).pagesRead()).isEqualTo(30);
        mockMvc.perform(post("/edicoes/{id}/paginas", id).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("pagesRead", "60").param("returnTo", "https://example.com"))
                .andExpect(redirectedUrl("/edicoes/" + id));
        assertThat(readingService.overall(owner.getId()).pagePercentageLabel()).isEqualTo("50,0%");
        assertThat(readingService.series(series.getId(), owner.getId()).pagesRead()).isEqualTo(60);
        assertThat(readingService.overall(other.getId()).totalPages()).isZero();
        var pagesDetail = mockMvc.perform(get("/edicoes/{id}", id).with(auth)).andExpect(content().string(org.hamcrest.Matchers.containsString("Salvar páginas lidas"))).andReturn().getResponse().getContentAsString();
        if (Boolean.getBoolean("ui.review.export")) {
            var root = java.nio.file.Path.of("target", "ui-showcase");
            java.nio.file.Files.createDirectories(root);
            for (var asset : List.of("css/main.css", "css/dashboard.css", "css/home-showcase.css", "js/sidebar.js", "js/home-banner.js")) {
                java.nio.file.Files.createDirectories(root.resolve(asset).getParent());
                java.nio.file.Files.copy(java.nio.file.Path.of("src/main/resources/static").resolve(asset), root.resolve(asset), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            java.nio.file.Files.writeString(root.resolve("hq-paginas.html"), pagesDetail);
            java.nio.file.Files.writeString(root.resolve("hq-editar.html"), mockMvc.perform(get("/edicoes/{id}/editar", id).with(auth)).andReturn().getResponse().getContentAsString());
        }
        mockMvc.perform(post("/edicoes/{id}/paginas", id).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("pagesRead", "120")).andExpect(status().is3xxRedirection());
        assertThat(editionService.viewOwned(id, owner.getId()).read()).isTrue();
        readingService.mark(id, owner.getId(), false); assertThat(readingService.overall(owner.getId()).pagesRead()).isZero();
        readingService.mark(id, owner.getId(), true); assertThat(readingService.overall(owner.getId()).pagesRead()).isEqualTo(120);
        mockMvc.perform(post("/edicoes/{id}/editar", id).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("title", "Leitura parcial").param("totalPages", "150").param("pagesRead", "120"))
                .andExpect(status().is3xxRedirection());
        assertThat(readingService.overall(owner.getId()).pagePercentageLabel()).isEqualTo("80,0%");
        assertThat(editionService.viewOwned(id, owner.getId()).read()).isFalse();
        mockMvc.perform(get("/dashboard").with(auth)).andExpect(content().string(org.hamcrest.Matchers.containsString("80,0%")));
        mockMvc.perform(get("/visao-geral").with(auth)).andExpect(content().string(org.hamcrest.Matchers.containsString("80,0%")));
        editionService.delete(id, owner.getId()); assertThat(readingService.overall(owner.getId()).totalPages()).isZero();
    }

    @Test
    void upgradesLegacyPageReadingWithoutInventingCountsAndEnforcesDatabaseConstraints() throws Exception {
        var schema = "page_reading_upgrade";
        org.flywaydb.core.Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(schema).defaultSchema(schema).target("16").load().migrate();
        var jdbc = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        var owner = java.util.UUID.randomUUID(); var edition = java.util.UUID.randomUUID();
        jdbc.update("insert into page_reading_upgrade.users (id,email,password_hash,role) values (?,?,?,'USER')", owner, "legacy-pages@example.com", "hash");
        jdbc.update("insert into page_reading_upgrade.editions (id,owner_user_id,title,normalized_title,is_read) values (?,?,?, ?,true)", edition, owner, "Legado", "legado");
        org.flywaydb.core.Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(schema).defaultSchema(schema).load().migrate();
        var row = jdbc.queryForMap("select total_pages,pages_read,is_read from page_reading_upgrade.editions where id = ?", edition);
        assertThat(row.get("total_pages")).isNull(); assertThat(row.get("pages_read")).isEqualTo(0); assertThat(row.get("is_read")).isEqualTo(true);
        for (var sql : List.of("pages_read=1", "total_pages=0", "total_pages=100001", "total_pages=10,pages_read=11", "total_pages=10,pages_read=-1", "total_pages=10,pages_read=0,is_read=true")) {
            assertThatThrownBy(() -> jdbc.update("update page_reading_upgrade.editions set " + sql + " where id = ?", edition))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        }
        jdbc.update("update page_reading_upgrade.editions set total_pages=10,pages_read=10,is_read=true where id = ?", edition);
    }
}
