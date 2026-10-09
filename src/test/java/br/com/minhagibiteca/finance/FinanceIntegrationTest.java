package br.com.minhagibiteca.finance;
import br.com.minhagibiteca.auth.domain.*;
import br.com.minhagibiteca.auth.application.UserPrincipal;
import br.com.minhagibiteca.auth.infrastructure.UserAccountRepository;
import br.com.minhagibiteca.catalog.application.*;
import br.com.minhagibiteca.catalog.domain.*;
import br.com.minhagibiteca.catalog.infrastructure.*;
import br.com.minhagibiteca.finance.application.*;
import br.com.minhagibiteca.finance.application.FinanceView.*;
import br.com.minhagibiteca.inventory.application.CopyService;
import br.com.minhagibiteca.inventory.domain.CopyCondition;
import br.com.minhagibiteca.shared.application.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.containers.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @Testcontainers
class FinanceIntegrationTest {
 @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:16-alpine");
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("spring.datasource.url",POSTGRES::getJdbcUrl);r.add("spring.datasource.username",POSTGRES::getUsername);r.add("spring.datasource.password",POSTGRES::getPassword);r.add("app.cover-storage-path",()->"target/finance-test-covers");}
 @Autowired JdbcTemplate jdbc;@Autowired MockMvc mvc;@Autowired UserAccountRepository users;
 @Autowired EditionService editions;@Autowired PublisherRepository publishers;@Autowired ComicSeriesRepository series;
 @Autowired CreatorService creators;@Autowired EditionAssociationService associations;@Autowired CharacterService characters;
 @Autowired FinanceService finance;@Autowired CopyService copies;
 @Autowired br.com.minhagibiteca.collection.application.ReadingService reading;
 @BeforeEach void clean(){for(String table:List.of("financial_purchases","edition_characters","comic_characters","physical_copies","series_checklist_items","edition_creators","edition_works","editions","creators","works","storage_locations","series","publishers","users"))jdbc.update("delete from "+table);}
 private UserAccount owner(String email){return users.save(UserAccount.create(email,"hash",UserRole.USER));}
 private Edition edition(UUID owner,String title,MediaType media,UUID publisher,UUID s){return editions.create(owner,new EditionService.Command(publisher,s,title,null,null,null,null,"Encadernado",null,media,new BigDecimal("999"),new BigDecimal("777")));}
 private UUID buy(UUID owner,UUID edition,String amount,LocalDate date){return finance.save(owner,null,new FinanceService.Command(edition,new BigDecimal(amount),date,"Loja","Nota"));}
 private FinanceFilter all(){return new FinanceFilter(null,null,null,null,null,null,null);}
 @Test void sumsOnlyPurchasesWithoutJoinMultiplicationAndFiltersEveryDimension()throws Exception{
  var a=owner("finance-a@example.com");var b=owner("finance-b@example.com");
  var pub=publishers.save(Publisher.create(a.getId(),"Editora Financeiro",null,null));var s=series.save(ComicSeries.create(a.getId(),pub.getId(),"Coleção Financeiro",null,null,null,null));
  var e=edition(a.getId(),"Batman — compra física",MediaType.PHYSICAL,pub.getId(),s.getId());
  var d=edition(a.getId(),"Superman — compra digital",MediaType.DIGITAL,pub.getId(),null);
  var ca=creators.create(a.getId(),new CreatorService.Command("Autor A",null));var cb=creators.create(a.getId(),new CreatorService.Command("Artista B",null));
  associations.replace(e.getId(),a.getId(),List.of(),List.of(ca.getId(),cb.getId()));associations.replace(d.getId(),a.getId(),List.of(),List.of(ca.getId()));
  var bat=characters.save(a.getId(),null,"Batman");var sup=characters.save(a.getId(),null,"Superman");
  characters.replace(a.getId(),e.getId(),List.of(bat,sup));characters.replace(a.getId(),d.getId(),List.of(bat));
  copies.create(e.getId(),a.getId(),new CopyService.Command(null,CopyCondition.GOOD,null,null,null,new BigDecimal("333"),null));
  buy(a.getId(),e.getId(),"100",LocalDate.of(2026,6,1));buy(a.getId(),e.getId(),"50",LocalDate.of(2026,6,10));buy(a.getId(),e.getId(),"20",null);
  buy(a.getId(),d.getId(),"200",LocalDate.of(2026,6,1));buy(a.getId(),d.getId(),"0",LocalDate.of(2026,5,22));
  var foreign=edition(b.getId(),"Título alheio",MediaType.PHYSICAL,null,null);buy(b.getId(),foreign.getId(),"999",LocalDate.of(2026,6,1));
  var report=finance.report(a.getId(),all(),0);assertThat(report.summary().count()).isEqualTo(5);assertThat(report.summary().total()).isEqualByComparingTo("370");assertThat(report.coverage().undatedAmount()).isEqualByComparingTo("20");
  var characterRows=report.charts().stream().filter(x->x.parameter().equals("characterId")).findFirst().orElseThrow().items();
  assertThat(characterRows).hasSize(2);assertThat(characterRows.stream().map(x->x.bucket().amount()).reduce(BigDecimal.ZERO,BigDecimal::add)).isEqualByComparingTo("540");
  var f=new FinanceFilter(LocalDate.of(2026,6,1),LocalDate.of(2026,6,10),s.getId(),ca.getId(),sup,pub.getId(),MediaType.PHYSICAL);
  var filtered=finance.report(a.getId(),f,0,true);assertThat(filtered.summary().total()).isEqualByComparingTo("150");assertThat(filtered.summary().count()).isEqualTo(2);assertThat(filtered.comparison().previous().total()).isZero();assertThat(filtered.comparison().change()).isEqualTo("Sem base para percentual");assertThat(filtered.monthly().items()).hasSize(1);
  assertThat(finance.report(a.getId(),new FinanceFilter(LocalDate.of(2026,6,1),LocalDate.of(2026,6,10),null,null,null,null,null),0).summary().total()).isEqualByComparingTo("350");
  var auth=SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(a));
  var response=mvc.perform(get("/financeiro").with(auth).param("start","2026-06-01").param("end","2026-06-10")).andExpect(status().isOk()).andReturn();
  var html=response.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
  assertThat(html).contains("350,00","Evolução mensal","Personagens","Sem base para percentual","Atribuição compartilhada").doesNotContain("Título alheio","999,00");
  fixture("index.html",html);
  fixture("empty.html",mvc.perform(get("/financeiro").with(SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner("empty@example.com"))))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
 }
 @Test void createsEditsDeletesPurchasesWithCsrfOwnerAndContextAndUpdatesReport()throws Exception{
  var a=owner("purchase-mvc@example.com");var b=owner("purchase-other@example.com");var e=edition(a.getId(),"Compra MVC <script>",MediaType.DIGITAL,null,null);
  var auth=SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(a));var other=SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(b));
  mvc.perform(post("/financeiro/compras").with(auth).param("editionId",e.getId().toString()).param("amount","19,50")).andExpect(status().isForbidden());
  mvc.perform(post("/financeiro/compras").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("editionId",e.getId().toString()).param("amount","19,50").param("purchaseDate","2026-06-01").param("returnTo","/financeiro?start=2026-06-01&end=2026-06-10")).andExpect(redirectedUrl("/financeiro?start=2026-06-01&end=2026-06-10"));
  var purchase=finance.report(a.getId(),all(),0).purchases().getContent().getFirst();assertThat(purchase.amount()).isEqualByComparingTo("19.50");
  var form=mvc.perform(get("/financeiro/compras/{id}/editar",purchase.id()).with(auth)).andExpect(status().isOk()).andReturn();assertThat(form.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).contains("&lt;script&gt;","19,50","name=\"_csrf\"");fixture("form.html",form.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
  mvc.perform(get("/financeiro/compras/{id}/editar",purchase.id()).with(other)).andExpect(status().isNotFound());
  mvc.perform(post("/financeiro/compras/{id}/editar",purchase.id()).with(other).with(SecurityMockMvcRequestPostProcessors.csrf()).param("amount","-1")).andExpect(status().isNotFound());
  mvc.perform(post("/financeiro/compras/{id}/editar",purchase.id()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("editionId",e.getId().toString()).param("amount","0").param("returnTo","https://example.com")).andExpect(redirectedUrl("/financeiro"));
  assertThat(finance.report(a.getId(),all(),0).summary().total()).isZero();assertThat(finance.report(a.getId(),all(),0).coverage().undatedCount()).isEqualTo(1);
  assertThatThrownBy(()->editions.delete(e.getId(),a.getId())).isInstanceOf(EditionService.ReferencedEditionException.class);
  mvc.perform(post("/financeiro/compras/{id}/excluir",purchase.id()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().is3xxRedirection());assertThat(finance.find(a.getId(),purchase.id())).isNotNull();
  mvc.perform(post("/financeiro/compras/{id}/excluir",purchase.id()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("confirmed","true")).andExpect(redirectedUrl("/financeiro"));assertThat(finance.report(a.getId(),all(),0).summary().count()).isZero();editions.delete(e.getId(),a.getId());
 }
 @Test void rejectsInvalidDraftForeignFiltersAndForeignPurchases()throws Exception{
  var a=owner("invalid-finance@example.com");var b=owner("invalid-other@example.com");var e=edition(a.getId(),"Minha edição",MediaType.PHYSICAL,null,null);var f=edition(b.getId(),"Segredo",MediaType.DIGITAL,null,null);
  var auth=SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(a));
  var invalid=mvc.perform(post("/financeiro/compras").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("editionId",e.getId().toString()).param("amount","valor").param("purchaseDate","não-data").param("vendor","Meu rascunho")).andExpect(status().isOk()).andReturn();assertThat(invalid.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).contains("Meu rascunho","Informe um valor válido","Informe uma data").doesNotContain("Failed to convert");fixture("errors.html",invalid.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
  mvc.perform(post("/financeiro/compras").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("editionId",f.getId().toString()).param("amount","10")).andExpect(status().isNotFound());
  mvc.perform(get("/financeiro/compras/nova").with(auth).param("editionId",f.getId().toString())).andExpect(status().isNotFound());
  var foreignCharacter=characters.save(b.getId(),null,"Personagem privado");
  mvc.perform(get("/financeiro").with(auth).param("characterId",foreignCharacter.toString())).andExpect(status().isNotFound());
  mvc.perform(get("/financeiro").with(auth).param("start","2026-06-01")).andExpect(status().isBadRequest());
  mvc.perform(get("/financeiro").with(auth).param("ranking","OTHER")).andExpect(status().isBadRequest());
  mvc.perform(get("/financeiro").with(auth).param("creatorId","invalid")).andExpect(status().isBadRequest());
  mvc.perform(get("/financeiro").with(auth).param("start","invalid")).andExpect(status().isBadRequest());
  mvc.perform(get("/financeiro").with(auth).param("mediaType","UNSPECIFIED")).andExpect(status().isOk());
  assertThat(finance.report(a.getId(),all(),0).summary().count()).isZero();mvc.perform(get("/financeiro")).andExpect(status().is3xxRedirection());
 }
 @Test void managesCharactersAndEditionBindingsAtomicallyWithDeletionProtection()throws Exception{
  var a=owner("character-mvc@example.com");var b=owner("character-other@example.com");var auth=SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(a));
  mvc.perform(post("/personagens").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("name","Batman")).andExpect(redirectedUrl("/personagens"));
  var character=characters.options(a.getId()).getFirst();assertThatThrownBy(()->characters.save(a.getId(),null," batman ")).isInstanceOf(CharacterService.CharacterRule.class);
  var foreign=characters.save(b.getId(),null,"Privado");
  mvc.perform(post("/edicoes").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("title","Não pode salvar").param("characterIds",foreign.toString())).andExpect(status().isNotFound());assertThat(jdbc.queryForObject("select count(*) from editions",Long.class)).isZero();
  mvc.perform(post("/edicoes").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("title","Edição com personagem").param("characterIds",character.id().toString())).andExpect(redirectedUrl("/edicoes"));
  UUID id=jdbc.queryForObject("select id from editions",UUID.class);assertThat(characters.ids(a.getId(),id)).containsExactly(character.id());
  mvc.perform(get("/edicoes/{id}/editar",id).with(auth)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("checked=\"checked\"")));
  assertThatThrownBy(()->characters.delete(a.getId(),character.id())).isInstanceOf(CharacterService.CharacterRule.class);
  mvc.perform(post("/edicoes/{id}/editar",id).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("title","Edição com personagem").param("_characterIds","on")).andExpect(status().is3xxRedirection());assertThat(characters.ids(a.getId(),id)).isEmpty();
  mvc.perform(get("/personagens/{id}/editar",foreign).with(auth)).andExpect(status().isNotFound());
  mvc.perform(post("/personagens/{id}/excluir",character.id()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("confirmed","true")).andExpect(redirectedUrl("/personagens"));assertThat(characters.options(a.getId())).isEmpty();
 }
 @Test void paginatesPurchasesAndRanksWithoutPageLimitedTotals()throws Exception{
  var a=owner("finance-pages@example.com");var auth=SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(a));
  for(int i=0;i<12;i++){var p=publishers.save(Publisher.create(a.getId(),"Editora "+String.format("%02d",i),null,null));var e=edition(a.getId(),"Edição "+i,MediaType.PHYSICAL,p.getId(),null);for(int j=0;j<2;j++)buy(a.getId(),e.getId(),"1",LocalDate.of(2026,6,1));}
  var r=finance.report(a.getId(),all(),1);assertThat(r.summary().total()).isEqualByComparingTo("24");assertThat(r.purchases().getContent()).hasSize(4);assertThat(r.purchases().getTotalElements()).isEqualTo(24);
  var chart=r.charts().stream().filter(x->x.parameter().equals("publisherId")).findFirst().orElseThrow();assertThat(chart.truncated()).isTrue();assertThat(chart.items().getLast().bucket().label()).isEqualTo("Outras editoras");assertThat(chart.items().stream().map(x->x.bucket().amount()).reduce(BigDecimal.ZERO,BigDecimal::add)).isEqualByComparingTo("24");
  mvc.perform(get("/financeiro").with(auth).param("ranking","QUANTITY").param("start","2026-06-01").param("end","2026-06-10")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("ranking=QUANTITY&amp;page=1")));
 }
 @Test void comparesEqualPeriodsFillsMissingMonthsAndRefreshesReadingAndSupport(){
  var a=owner("finance-comparison@example.com");var e=edition(a.getId(),"Sem suporte informado",null,null,null);
  buy(a.getId(),e.getId(),"100",LocalDate.of(2026,2,1));
  buy(a.getId(),e.getId(),"150",LocalDate.of(2026,3,1));
  var march=new FinanceFilter(LocalDate.of(2026,3,1),LocalDate.of(2026,3,31),null,null,null,null,null);
  var r=finance.report(a.getId(),march,0);
  assertThat(r.comparison().previous().total()).isEqualByComparingTo("100");
  assertThat(r.comparison().difference()).isEqualByComparingTo("50");
  assertThat(r.comparison().change()).isEqualTo("50,0%");
  var quarter=finance.report(a.getId(),new FinanceFilter(LocalDate.of(2026,1,1),LocalDate.of(2026,3,31),null,null,null,null,null),0);
  assertThat(quarter.monthly().items()).hasSize(3);
  assertThat(quarter.monthly().items().getFirst().amount()).isZero();
  assertThat(quarter.summary().unreadAmount()).isEqualByComparingTo("250");
  reading.mark(e.getId(),a.getId(),true);
  assertThat(finance.report(a.getId(),all(),0).summary().unreadAmount()).isZero();
  assertThat(finance.report(a.getId(),new FinanceFilter(null,null,null,null,null,null,null,true),0).summary().count()).isEqualTo(2);
  assertThat(finance.report(a.getId(),new FinanceFilter(null,null,null,null,null,null,MediaType.DIGITAL),0).summary().count()).isZero();
 }
 private void fixture(String name,String html)throws Exception{
  var path=java.nio.file.Path.of("target/ui-finance");java.nio.file.Files.createDirectories(path.resolve("css"));java.nio.file.Files.writeString(path.resolve(name),html);
  for(String css:List.of("main.css","dashboard.css"))java.nio.file.Files.copy(java.nio.file.Path.of("src/main/resources/static/css",css),path.resolve("css").resolve(css),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
 }
}
