package br.com.minhagibiteca.shared.presentation;

import br.com.minhagibiteca.auth.application.UserPrincipal;
import br.com.minhagibiteca.auth.domain.*;
import br.com.minhagibiteca.auth.infrastructure.UserAccountRepository;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Renderiza todas as famílias de telas com acervo sintético e conta vazia. */
@SpringBootTest(properties={"app.rclone.enabled=false","app.onedrive.enabled=false"}) @AutoConfigureMockMvc @Testcontainers
class InterfaceIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",POSTGRES::getJdbcUrl);r.add("spring.datasource.username",POSTGRES::getUsername);r.add("spring.datasource.password",POSTGRES::getPassword);r.add("app.cover-storage-path",()->"target/ui-review-covers");}
    @Autowired MockMvc mvc;@Autowired JdbcTemplate jdbc;@Autowired UserAccountRepository users;
    UserAccount owner,empty;UUID publisher,series,work,creator,character,location,edition,copy,item;
    static final Path PREVIEW=Path.of("target/ui-review");
    @BeforeEach void setup() throws Exception {
        for(var table:List.of("financial_purchases","edition_characters","physical_copies","series_checklist_items","edition_creators","edition_works","editions","creator_functions","creators","works","comic_characters","storage_locations","series","publishers","users"))jdbc.update("delete from "+table);
        owner=users.save(UserAccount.create("ui-review@example.com","hash",UserRole.USER));empty=users.save(UserAccount.create("ui-empty@example.com","hash",UserRole.USER));
        publisher=UUID.randomUUID();series=UUID.randomUUID();work=UUID.randomUUID();creator=UUID.randomUUID();character=UUID.randomUUID();location=UUID.randomUUID();edition=UUID.randomUUID();copy=UUID.randomUUID();item=UUID.randomUUID();
        jdbc.update("insert into publishers(id,owner_user_id,name,normalized_name) values (?,?,?,?)",publisher,owner.getId(),"Editora "+"Extraordinária".repeat(10),"editora");
        jdbc.update("insert into series(id,owner_user_id,publisher_id,name,normalized_name,status) values (?,?,?,?,'serie','ONGOING')",series,owner.getId(),publisher,"A grande coleção "+"Aventuras".repeat(14));
        jdbc.update("insert into works(id,owner_user_id,title,normalized_title) values (?,?,?,'obra')",work,owner.getId(),"Uma obra "+"Superespecial".repeat(15));
        jdbc.update("insert into creators(id,owner_user_id,name,normalized_name) values (?,?,?,'criador')",creator,owner.getId(),"Criador "+"Sobrenome".repeat(15));
        jdbc.update("insert into creator_functions values (?,'ROTEIRISTA'), (?,'DESENHISTA')",creator,creator);
        jdbc.update("insert into comic_characters(id,owner_user_id,name,normalized_name) values (?,?,?,'heroi')",character,owner.getId(),"Herói "+"Superpoderoso".repeat(12));
        jdbc.update("insert into storage_locations(id,owner_user_id,level,name,normalized_name) values (?,?,'BOX',?,'caixa')",location,owner.getId(),"Caixa "+"Especial".repeat(13));
        jdbc.update("insert into editions(id,owner_user_id,publisher_id,series_id,title,normalized_title,media_type,reference_paid_price,purchase_date,total_pages,pages_read) values (?,?,?,?,?,'hq','PHYSICAL',39.90,'2026-10-10',144,72)",edition,owner.getId(),publisher,series,"HQ "+"UmaAventuraMuitoLonga".repeat(10));
        jdbc.update("insert into edition_works values (?,?,1)",edition,work);jdbc.update("insert into edition_creators values (?,?,'ROTEIRISTA',1)",edition,creator);jdbc.update("insert into edition_characters values (?,?,?)",owner.getId(),edition,character);
        jdbc.update("insert into physical_copies(id,owner_user_id,edition_id,storage_location_id,condition,created_at,updated_at) values (?,?,?,?,'GOOD',now(),now())",copy,owner.getId(),edition,location);
        jdbc.update("insert into series_checklist_items(id,owner_user_id,series_id,editorial_number,normalized_number,label,owned,created_at,updated_at) values (?,?,?,'1A','1a',?,false,now(),now())",item,owner.getId(),series,"Edição "+"Especial".repeat(18));
        Files.createDirectories(PREVIEW.resolve("css"));Files.createDirectories(PREVIEW.resolve("js"));
        for(var css:List.of("main","dashboard","home-showcase"))Files.copy(Path.of("src/main/resources/static/css/"+css+".css"),PREVIEW.resolve("css/"+css+".css"),StandardCopyOption.REPLACE_EXISTING);
        for(var js:List.of("sidebar","home-banner","association-picker"))Files.copy(Path.of("src/main/resources/static/js/"+js+".js"),PREVIEW.resolve("js/"+js+".js"),StandardCopyOption.REPLACE_EXISTING);
    }
    @Test void rendersCreatorPhotoInListAndEditForm() throws Exception {
        byte[] png=Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
        var auth=user(UserPrincipal.from(owner));
        mvc.perform(multipart("/criadores/"+creator+"/editar").file(new org.springframework.mock.web.MockMultipartFile("photo","foto.png","image/png",png)).with(auth).with(csrf()).param("name","Criador com foto"))
                .andExpect(status().is3xxRedirection());
        for(var entry:Map.of("criador-foto","/criadores/"+creator+"/editar","criadores-fotos","/criadores","criador-novo-foto","/criadores/novo").entrySet()) {
            var html=mvc.perform(get(entry.getValue()).with(auth)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            audit(entry.getKey(),html);preview(entry.getKey(),html);
        }
        var invalid=mvc.perform(multipart("/criadores/"+creator+"/editar").file(new org.springframework.mock.web.MockMultipartFile("photo","errada.png","image/png",new byte[]{1,2,3})).with(auth).with(csrf()).param("name","Nome mantido no formulário"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(invalid).contains("Nome mantido no formulário","photo-error","aria-invalid=\"true\"");audit("criador-foto-erro",invalid);preview("criador-foto-erro",invalid);
        Files.createDirectories(PREVIEW.resolve("criadores/"+creator));Files.write(PREVIEW.resolve("criadores/"+creator+"/foto"),png);
    }
    @Test void rendersOneDriveSetupWithoutRequiringCredentials() throws Exception {
        String html=mvc.perform(get("/biblioteca-digital").with(user(UserPrincipal.from(owner)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        audit("onedrive-configurar",html);assertThat(html).contains("A integração ainda não foi configurada", "docs/onedrive.md").doesNotContain("ONEDRIVE_CLIENT_SECRET=");preview("onedrive-configurar",html);
        mvc.perform(post("/biblioteca-digital/onedrive/conectar").with(user(UserPrincipal.from(owner))).with(csrf())).andExpect(redirectedUrl("/biblioteca-digital"));
    }

    @Test void rendersWishlistAndValidatedFormWithLongContent() throws Exception {
        jdbc.update("insert into wishlist_items(id,owner_user_id,title,publication_type,priority,estimated_price,notes) values(?,? ,?,'BOOK','HIGH',49.90,?)",UUID.randomUUID(),owner.getId(),"Título desejado "+"MuitoLongo".repeat(20),"Observações "+"TextoExtenso".repeat(90));
        var auth=user(UserPrincipal.from(owner));
        for(var path:Map.of("desejos","/desejos","desejo-novo","/desejos/novo").entrySet()){
            String html=mvc.perform(get(path.getValue()).with(auth)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            audit(path.getKey(),html);preview(path.getKey(),html);
        }
        String invalid=mvc.perform(post("/desejos").with(auth).with(csrf()).param("title","").param("estimatedPrice","-1")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        audit("desejo-erros",invalid);assertThat(invalid).contains("Informe o título.","O preço não pode ser negativo.");preview("desejo-erros",invalid);
    }

    @Test void rendersBookFormsDetailsAndFilteredCatalogWithNewBrand() throws Exception {
        jdbc.update("update editions set publication_type='BOOK',title='Um livro da minha biblioteca' where id=?",edition);
        var auth=user(UserPrincipal.from(owner));
        for(var entry:Map.of("livro-cadastro","/edicoes/nova","livro-editar","/edicoes/"+edition+"/editar","livro-detalhe","/edicoes/"+edition,"livros-catalogo","/edicoes?publicationType=BOOK").entrySet()) {
            String html=mvc.perform(get(entry.getValue()).with(auth)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            audit(entry.getKey(),html);assertThat(html).contains("Minha Biblioteca");preview(entry.getKey(),html);
            if(entry.getKey().equals("livro-editar"))assertThat(html).contains("value=\"BOOK\" selected=\"selected\"");
        }
    }

    @Test void rendersPublicRegistrationWithAccessibleFieldsAndSafeErrors() throws Exception {
        for(String route:List.of("/login","/cadastro")) {
            var html=mvc.perform(get(route)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            audit(route.substring(1),html);
            Files.writeString(PREVIEW.resolve(route.equals("/login")?"login-publico.html":"cadastro-publico.html"),html);
        }
        String secret="senha-sintetica-123";
        var html=mvc.perform(post("/cadastro").with(csrf()).param("email","email-inválido")
                        .param("password",secret).param("passwordConfirmation","confirmação-diferente"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        audit("cadastro-erros",html);
        assertThat(html).contains("Informe um e-mail válido.","As senhas devem ser iguais.","aria-invalid=\"true\"")
                .doesNotContain(secret,"confirmação-diferente");
        Files.writeString(PREVIEW.resolve("cadastro-erros.html"),html);
        String success=mvc.perform(get("/login").flashAttr("registrationSuccess","Conta criada com sucesso! Entre com seu e-mail e senha para começar sua coleção."))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        Files.writeString(PREVIEW.resolve("login-conta-criada.html"),success);
    }

    @Test void organizesEditionDetailsAndKeepsSecondaryDataOutsideCatalog() throws Exception {
        jdbc.update("update editions set title='Batman: uma aventura',editorial_number='10',isbn='9786559601325',variant='Capa alternativa',publication_year=2026,format='Encadernado',cover_price=59.90,rating=4,notes=? where id=?", "Observação com <script> e duas linhas\nSegunda linha", edition);
        var auth=user(UserPrincipal.from(owner));
        String detail="/edicoes/"+edition;
        String html=mvc.perform(get(detail).with(auth).param("returnTo","/edicoes?q=Batman&page=1"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        int previous=-1;
        for(String heading:List.of("cover-title","page-title","edition-reading-title","rating-title","edition-data-title","edition-finance-title","edition-content-title","copies-title","edition-notes-title","edition-delete-title")) {
            int current=html.indexOf("id=\""+heading+"\"");
            assertThat(current).as("ordem de %s",heading).isGreaterThan(previous);previous=current;
        }
        assertThat(html).contains("Dados da edição","Compra e valores","Conteúdo e créditos","Observações",
                "9786559601325","Capa alternativa","59,90","39,90","4 de 5 estrelas","144","72",
                "class=\"edition-cover-upload\" open", "class=\"action-delete\"", "Confirmar exclusão da edição",
                "name=\"_csrf\"", "&lt;script&gt;").doesNotContain("class=\"action-delete\" open", "<script> e duas");
        assertThat(html.indexOf("action=\""+detail+"/excluir\"")).isGreaterThan(html.indexOf("class=\"action-delete\""));
        audit("edicao-organizada",html);Files.writeString(PREVIEW.resolve("edicao-organizada.html"),html);
        String catalog=mvc.perform(get("/edicoes").with(auth)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(catalog).contains("Nº 10","4 de 5 estrelas","39,90","Ver edição","Editar")
                .doesNotContain("9786559601325","Capa alternativa","59,90");
        assertThat(catalog.indexOf("class=\"edition-card-personal\"")).isLessThan(catalog.indexOf("class=\"edition-card-prices\""));
        Files.writeString(PREVIEW.resolve("catalogo-organizado.html"),catalog);
        byte[] png=Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
        mvc.perform(multipart(detail+"/capa").file(new org.springframework.mock.web.MockMultipartFile("cover","capa.png","image/png",png)).with(auth).with(csrf()))
                .andExpect(status().is3xxRedirection());
        String withCover=mvc.perform(get(detail).with(auth)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(withCover).contains("Alterar capa").doesNotContain("class=\"edition-cover-upload\" open");
        Files.writeString(PREVIEW.resolve("edicao-com-capa.html"),withCover);
        Files.createDirectories(PREVIEW.resolve("edicoes/"+edition));Files.write(PREVIEW.resolve("edicoes/"+edition+"/capa"),png);
        jdbc.update("delete from physical_copies where id=?",copy);
        jdbc.update("update editions set media_type='DIGITAL',total_pages=null,pages_read=0,rating=0 where id=?",edition);
        String digital=mvc.perform(get(detail).with(auth)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(digital).contains("0 de 5 estrelas","Edição digital: não possui exemplares físicos.","Total de páginas não informado").doesNotContain(">Novo exemplar</a>");
        Files.writeString(PREVIEW.resolve("edicao-digital.html"),digital);
    }

    Map<String,String> routes(){var routes=new LinkedHashMap<String,String>();
        for(var path:List.of("dashboard","visao-geral","edicoes","series","editoras","obras","criadores","personagens","localizacoes","financeiro","relatorios","backup"))routes.put(path,"/"+path);
        routes.put("edicao-nova","/edicoes/nova");routes.put("edicao-editar","/edicoes/"+edition+"/editar");routes.put("edicao-detalhe","/edicoes/"+edition);
        var ids=Map.of("editoras",publisher,"series",series,"obras",work,"criadores",creator,"personagens",character,"localizacoes",location);
        for(var section:List.of("editoras","series","obras","criadores","personagens","localizacoes")){routes.put(section+"-novo","/"+section+"/"+(Set.of("criadores","personagens").contains(section)?"novo":"nova"));routes.put(section+"-editar","/"+section+"/"+ids.get(section)+"/editar");}
        routes.put("exemplar-novo","/edicoes/"+edition+"/exemplares/novo");routes.put("exemplar-editar","/exemplares/"+copy+"/editar");routes.put("exemplar-excluir","/exemplares/"+copy+"/excluir");
        routes.put("local-excluir","/localizacoes/"+location+"/excluir");routes.put("personagem-excluir","/personagens/"+character+"/excluir");
        routes.put("checklist","/series/"+series+"/checklist");routes.put("faltantes","/series/"+series+"/faltantes");routes.put("item-editar","/checklist/itens/"+item+"/editar");routes.put("item-excluir","/checklist/itens/"+item+"/excluir");return routes;
    }
    @Test void rendersEveryScreenWithLongTextAndConsistentAccessibleMarkup() throws Exception {
        for(var route:routes().entrySet()) {String html=mvc.perform(get(route.getValue()).with(user(UserPrincipal.from(owner)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();audit(route.getKey(),html);preview(route.getKey(),html);}
        for(var route:List.of("login","login?error","login?logout")){String html=mvc.perform(get("/"+route)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();audit(route,html);preview(route.replace('?','-'),html);}
    }
    @Test void rendersEmptyAndFilteredStatesWithoutLosingNavigation() throws Exception {
        for(var path:List.of("dashboard","visao-geral","edicoes","series","editoras","obras","criadores","personagens","localizacoes","financeiro","relatorios","backup")){
            String html=mvc.perform(get("/"+path).with(user(UserPrincipal.from(empty)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();audit("empty-"+path,html);preview("empty-"+path,html);
        }
        for(var path:List.of("edicoes","series","editoras","obras","criadores","personagens","localizacoes","relatorios")){
            String html=mvc.perform(get("/"+path).param("q","Nada corresponde à pesquisa").with(user(UserPrincipal.from(owner)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();audit("filtered-"+path,html);preview("filtered-"+path,html);
        }
    }
    @Test void invalidFormsKeepEnteredValuesAndExposeAssociatedErrors() throws Exception {
        for(var section:List.of("editoras","series","obras","criadores","personagens","localizacoes","edicoes")){
            String html=mvc.perform(post("/"+section).param("notes","Anotação preservada").with(csrf()).with(user(UserPrincipal.from(owner)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();audit("invalid-"+section,html);
            assertThat(html).as(section).contains("Revise os campos indicados","aria-invalid=\"true\"");preview("invalid-"+section,html);
            if(Set.of("editoras","series","localizacoes","edicoes").contains(section))assertThat(org.springframework.web.util.HtmlUtils.htmlUnescape(html)).contains("Anotação preservada");
        }
        for(var path:List.of("/series/"+series+"/checklist","/edicoes/"+edition+"/exemplares")){
            String html=mvc.perform(post(path).param("acquisitionPrice","valor inválido").with(csrf()).with(user(UserPrincipal.from(owner)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();audit(path,html);assertThat(html).contains("Revise os campos indicados","aria-invalid=\"true\"");preview(path.contains("checklist")?"invalid-checklist":"invalid-exemplar",html);
        }
    }
    @Test void deleteActionsHaveExplicitConfirmationWithoutChangingCsrfProtection() throws Exception {
        for(var section:List.of("editoras","series","obras","criadores")){
            String html=mvc.perform(get("/"+section).with(user(UserPrincipal.from(owner)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(html).contains("class=\"action-delete\"","Confirmar exclusão","Esta ação é permanente","name=\"_csrf\"");
        }
        mvc.perform(post("/editoras/"+publisher+"/excluir").with(user(UserPrincipal.from(owner)))).andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("select count(*) from publishers",Integer.class)).isEqualTo(1);
    }
    @Test void errorScreensHaveClearWayBackAndAccessibleHeading() throws Exception {
        var paths=Map.of("error-404","/edicoes/"+UUID.randomUUID(),"error-400","/edicoes?publisherId=INVALID","error-finance","/financeiro?start=2026-10-10","error-reports","/relatorios?start=2026-10-10");
        for(var path:paths.entrySet()){
            String html=mvc.perform(get(path.getValue()).with(user(UserPrincipal.from(owner)))).andExpect(status().is4xxClientError()).andReturn().getResponse().getContentAsString();audit(path.getKey(),html);preview(path.getKey(),html);
        }
    }
    static void preview(String name,String html) throws Exception {Files.writeString(PREVIEW.resolve(name+".html"),html);}
    @Test void associationSearchIsPaginatedAccentInsensitiveLiteralAndPrivate() throws Exception {
        for (var kind : List.of("WORK", "CREATOR", "CHARACTER")) {
            String table = kind.equals("WORK") ? "works" : kind.equals("CREATOR") ? "creators" : "comic_characters";
            String name = kind.equals("WORK") ? "title" : "name";
            for (int i = 0; i < 25; i++) jdbc.update("insert into " + table + "(id,owner_user_id," + name + ",normalized_" + name + ") values (?,?,?,?)",
                    UUID.randomUUID(), owner.getId(), "A opção " + String.format("%02d", i), "opcao" + i);
            jdbc.update("insert into " + table + "(id,owner_user_id," + name + ",normalized_" + name + ") values (?,?,?,?)", UUID.randomUUID(), empty.getId(), "A opção privada", "privada");
            mvc.perform(get("/edicoes/vinculos/" + kind).with(user(UserPrincipal.from(owner))).param("q", "OPCAO"))
                    .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(jsonPath("$.items.length()").value(20)).andExpect(jsonPath("$.hasNext").value(true))
                    .andExpect(jsonPath("$.items[0].label").value("A opção 00"));
            mvc.perform(get("/edicoes/vinculos/" + kind).with(user(UserPrincipal.from(owner))).param("q", "opção").param("page", "1"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(5)).andExpect(jsonPath("$.hasNext").value(false));
            for (var literal : List.of("%", "_", "\\")) mvc.perform(get("/edicoes/vinculos/" + kind).with(user(UserPrincipal.from(owner))).param("q", literal))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
            mvc.perform(get("/edicoes/vinculos/" + kind).with(user(UserPrincipal.from(owner))).param("q", "privada"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper(); var combined = mapper.createArrayNode();
            for (int page = 0; page < 2; page++) {
                var response = mvc.perform(get("/edicoes/vinculos/" + kind).with(user(UserPrincipal.from(owner))).param("page", "" + page))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
                combined.addAll((com.fasterxml.jackson.databind.node.ArrayNode) mapper.readTree(response).get("items"));
            }
            Files.writeString(PREVIEW.resolve("options-" + kind + ".json"), combined.toString());
        }
        preview("picker-new", mvc.perform(get("/edicoes/nova").with(user(UserPrincipal.from(owner)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    @Test void associationSearchFiltersCreatorFunctionsAndValidatesParameters() throws Exception {
        mvc.perform(get("/edicoes/vinculos/CREATOR").with(user(UserPrincipal.from(owner))).param("function", "DESENHISTA"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].description").value("Roteirista, Desenhista"));
        mvc.perform(get("/edicoes/vinculos/CREATOR").with(user(UserPrincipal.from(owner))).param("function", "COLORISTA"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
        for (String query : List.of("page=-1", "page=501", "q=" + "x".repeat(181), "function=INVALID", "function=DESENHISTA"))
            mvc.perform(get("/edicoes/vinculos/WORK?" + query).with(user(UserPrincipal.from(owner)))).andExpect(status().isBadRequest());
        mvc.perform(get("/edicoes/vinculos/WORK")).andExpect(status().is3xxRedirection());
    }
    @Test void associationSelectionsOutsideFirstPageSurviveEditAndValidation() throws Exception {
        UUID selectedWork = UUID.randomUUID();
        for (int i = 0; i < 25; i++) jdbc.update("insert into works(id,owner_user_id,title,normalized_title) values (?,?,?,?)", UUID.randomUUID(), owner.getId(), "A obra " + i, "a obra " + i);
        jdbc.update("insert into works(id,owner_user_id,title,normalized_title) values (?,?,'Z obra escolhida','z obra escolhida')", selectedWork, owner.getId());
        jdbc.update("insert into edition_works values (?,?,2)", edition, selectedWork);
        String html = mvc.perform(get("/edicoes/" + edition + "/editar").with(user(UserPrincipal.from(owner)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        audit("picker-edit", html); assertThat(html).contains("Z obra escolhida", "data-field=\"workIds\"", "association-picker.js");
        assertThat(count(html, "data-id=")).isLessThan(30); preview("picker-edit", html);
        String invalid = mvc.perform(post("/edicoes/" + edition + "/editar").with(user(UserPrincipal.from(owner))).with(csrf()).param("title", "")
                .param("workIds", selectedWork.toString()).param("creatorIds", creator.toString()).param("characterIds", character.toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        audit("picker-invalid", invalid); assertThat(invalid).contains("Z obra escolhida", "Revise os campos indicados", "checked=\"checked\""); preview("picker-invalid", invalid);
        mvc.perform(post("/edicoes/" + edition + "/editar").with(user(UserPrincipal.from(owner))).with(csrf()).param("title", "Seleções preservadas")
                .param("workIds", selectedWork.toString()).param("creatorIds", creator.toString()).param("characterIds", character.toString()))
                .andExpect(status().is3xxRedirection());
        assertThat(jdbc.queryForList("select work_id from edition_works where edition_id=?", UUID.class, edition)).containsExactly(selectedWork);
        assertThat(jdbc.queryForList("select creator_id from edition_creators where edition_id=?", UUID.class, edition)).containsExactly(creator);
        assertThat(jdbc.queryForList("select character_id from edition_characters where edition_id=?", UUID.class, edition)).containsExactly(character);
    }
    static void audit(String name,String html) {
        assertThat(html).as(name).contains("lang=\"pt-BR\"","<title>");assertThat(count(html,"<h1\\b")).as(name+" h1").isEqualTo(1);
        var ids=new HashSet<String>();var labels=new HashSet<String>();var references=new ArrayList<String>();
        var tags=Pattern.compile("<([a-zA-Z][a-zA-Z0-9-]*)\\b([^>]*)>").matcher(html);
        var fields=new ArrayList<Map<String,String>>();
        while(tags.find()){
            var attributes=new HashMap<String,String>();var matcher=Pattern.compile("([a-zA-Z_:][a-zA-Z0-9_:.-]*)=\"([^\"]*)\"").matcher(tags.group(2));while(matcher.find())attributes.put(matcher.group(1),matcher.group(2));
            if(attributes.containsKey("id"))assertThat(ids.add(attributes.get("id"))).as(name+" duplicate id "+attributes.get("id")).isTrue();
            if(tags.group(1).equals("label") && attributes.containsKey("for"))labels.add(attributes.get("for"));
            for(var attr:List.of("aria-describedby","aria-labelledby"))if(attributes.containsKey(attr))for(var ref:attributes.get(attr).trim().split("\\s+"))if(!ref.isBlank())references.add(ref);
            if(Set.of("input","select","textarea").contains(tags.group(1)) && !"hidden".equals(attributes.get("type"))){attributes.put("offset",String.valueOf(tags.start()));fields.add(attributes);}
        }
        for(var ref:references)assertThat(ids).as(name+" aria reference "+ref).contains(ref);
        for(var field:fields){String id=field.get("id");int offset=Integer.parseInt(field.get("offset"));boolean implicit=html.lastIndexOf("<label",offset)>html.lastIndexOf("</label>",offset);
            assertThat(labels.contains(id)||implicit||field.containsKey("aria-label")||field.containsKey("aria-labelledby")).as(name+" unlabeled field "+id).isTrue();
        }
        if(!name.startsWith("login") && !name.startsWith("cadastro"))assertThat(html).as(name).contains("Pular para o conteúdo","aria-label=\"Menu principal\"");
    }
    static int count(String html,String regex){var m=Pattern.compile(regex).matcher(html);int n=0;while(m.find())n++;return n;}
}
