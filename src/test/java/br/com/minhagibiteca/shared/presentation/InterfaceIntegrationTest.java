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
@SpringBootTest @AutoConfigureMockMvc @Testcontainers
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
        for(var js:List.of("sidebar","home-banner"))Files.copy(Path.of("src/main/resources/static/js/"+js+".js"),PREVIEW.resolve("js/"+js+".js"),StandardCopyOption.REPLACE_EXISTING);
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
        if(!name.startsWith("login"))assertThat(html).as(name).contains("Pular para o conteúdo","aria-label=\"Menu principal\"");
    }
    static int count(String html,String regex){var m=Pattern.compile(regex).matcher(html);int n=0;while(m.find())n++;return n;}
}
