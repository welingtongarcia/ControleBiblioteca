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
 private Edition edition(UUID owner,String title,MediaType media,UUID publisher,UUID series,String amount,LocalDate date){
  return editions.create(owner,new EditionService.Command(publisher,series,title,null,null,null,null,"Encadernado",null,media,new BigDecimal("999"),amount==null?null:new BigDecimal(amount),date));
 }
 private FinanceFilter all(){return new FinanceFilter(null,null,null,null,null,null,null);}
 private void legacy(UUID owner,UUID edition,String amount){
  jdbc.update("insert into financial_purchases(id,owner_user_id,edition_id,amount,purchase_date,vendor,notes) values(?,?,?,?,?,?,?)",UUID.randomUUID(),owner,edition,new BigDecimal(amount),LocalDate.of(2026,6,1),"Loja antiga","Histórico preservado");
 }
 @Test void usesEditionPricesOnlyWithCoverageAndNoJoinMultiplication()throws Exception{
  var a=owner("finance-a@example.com");var b=owner("finance-b@example.com");
  var pub=publishers.save(Publisher.create(a.getId(),"Editora Financeiro",null,null));var s=series.save(ComicSeries.create(a.getId(),pub.getId(),"Coleção Financeiro",null,null,null,null));
  var e=edition(a.getId(),"Batman — físico",MediaType.PHYSICAL,pub.getId(),s.getId(),"100",LocalDate.of(2026,6,1));
  var d=edition(a.getId(),"Superman — digital",MediaType.DIGITAL,pub.getId(),null,"200",LocalDate.of(2026,6,10));
  edition(a.getId(),"Sem preço",null,null,null,null,LocalDate.of(2026,6,1));
  edition(a.getId(),"Gratuita",MediaType.DIGITAL,null,null,"0",LocalDate.of(2026,6,1));
  edition(a.getId(),"Sem data",MediaType.PHYSICAL,null,null,"50",null);
  edition(b.getId(),"Título alheio",MediaType.PHYSICAL,null,null,"12345",LocalDate.of(2026,6,1));
  var ca=creators.create(a.getId(),new CreatorService.Command("Autor A",null));var cb=creators.create(a.getId(),new CreatorService.Command("Artista B",null));
  associations.replace(e.getId(),a.getId(),List.of(),List.of(ca.getId(),cb.getId()));associations.replace(d.getId(),a.getId(),List.of(),List.of(ca.getId()));
  var bat=characters.save(a.getId(),null,"Batman");var sup=characters.save(a.getId(),null,"Superman");
  characters.replace(a.getId(),e.getId(),List.of(bat,sup));characters.replace(a.getId(),d.getId(),List.of(bat));
  copies.create(e.getId(),a.getId(),new CopyService.Command(null,CopyCondition.GOOD,null,null,null,new BigDecimal("333"),null));
  legacy(a.getId(),e.getId(),"7777");
  var r=finance.report(a.getId(),all(),0);
  assertThat(r.summary().count()).isEqualTo(5);assertThat(r.summary().total()).isEqualByComparingTo("350");
  assertThat(r.summary().pricedCount()).isEqualTo(4);assertThat(r.summary().getUnknownCount()).isEqualTo(1);
  assertThat(r.summary().getAverage()).contains("87,50");assertThat(r.coverage().undatedAmount()).isEqualByComparingTo("50");
  var chart=r.charts().stream().filter(x->x.parameter().equals("characterId")).findFirst().orElseThrow();
  assertThat(chart.items().stream().map(x->x.bucket().amount()).reduce(BigDecimal.ZERO,BigDecimal::add)).isEqualByComparingTo("400");
  var f=new FinanceFilter(LocalDate.of(2026,6,1),LocalDate.of(2026,6,10),s.getId(),ca.getId(),sup,pub.getId(),MediaType.PHYSICAL);
  assertThat(finance.report(a.getId(),f,0,true).summary().total()).isEqualByComparingTo("100");
  assertThat(finance.report(a.getId(),f,0).summary().count()).isEqualTo(1);
  assertThat(finance.report(a.getId(),new FinanceFilter(null,null,null,null,null,null,null,true),0).summary().count()).isEqualTo(1);
  var html=mvc.perform(get("/financeiro").with(SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(a)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
  assertThat(html).contains("350,00","87,50","Edições sem valor pago: 1","Cobertura de valores: 4 de 5","Não informado","Editar edição").doesNotContain("7777","Título alheio","Registrar compra","/financeiro/compras");
  fixture("index.html",html);
  fixture("empty.html",mvc.perform(get("/financeiro").with(SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner("empty@example.com"))))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
 }
 @Test void createsAndEditsPriceAndDateInsideEditionWithFinanceReturn()throws Exception{
  var a=owner("edition-finance@example.com");var b=owner("foreign-edit@example.com");
  var auth=SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(a));
  String context="/financeiro?start=2026-06-01&end=2026-06-10";
  mvc.perform(post("/edicoes").with(auth).param("title","HQ")).andExpect(status().isForbidden());
  mvc.perform(post("/edicoes").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("title","HQ <script>").param("referencePaidPrice","19,50").param("purchaseDate","2026-06-01").param("returnTo",context)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern("/edicoes/*?returnTo=/**"));
  var entry=finance.report(a.getId(),all(),0).entries().getContent().getFirst();
  assertThat(entry.amount()).isEqualByComparingTo("19.50");assertThat(entry.purchaseDate()).isEqualTo(LocalDate.of(2026,6,1));
  var form=mvc.perform(get("/edicoes/{id}/editar",entry.id()).with(auth).param("returnTo",context)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
  assertThat(form).contains("value=\"2026-06-01\"","19,50","&lt;script&gt;","Data da compra");fixture("form.html",form);
  mvc.perform(get("/edicoes/{id}",entry.id()).with(auth)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("01/06/2026")));
  mvc.perform(post("/edicoes/{id}/editar",entry.id()).with(SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(b))).with(SecurityMockMvcRequestPostProcessors.csrf()).param("purchaseDate","invalid")).andExpect(status().isNotFound());
  mvc.perform(post("/edicoes/{id}/editar",entry.id()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("title","HQ").param("referencePaidPrice","40,00").param("purchaseDate","2026-05-01").param("returnTo",context)).andExpect(redirectedUrl(context));
  assertThat(finance.report(a.getId(),new FinanceFilter(LocalDate.of(2026,6,1),LocalDate.of(2026,6,10),null,null,null,null,null),0).summary().count()).isZero();
  assertThat(finance.report(a.getId(),all(),0).summary().total()).isEqualByComparingTo("40");
  mvc.perform(post("/edicoes/{id}/editar",entry.id()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("title","HQ").param("referencePaidPrice","").param("purchaseDate","")).andExpect(status().is3xxRedirection());
  var r=finance.report(a.getId(),all(),0);assertThat(r.summary().pricedCount()).isZero();assertThat(r.summary().getAverage()).isEqualTo("Não informado");assertThat(r.coverage().undatedCount()).isEqualTo(1);
  mvc.perform(post("/edicoes/{id}/excluir",entry.id()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().is3xxRedirection());
  assertThat(finance.report(a.getId(),all(),0).summary().count()).isZero();
 }
 @Test void rejectsInvalidDatesAndPreservesDraftAndStoredAmounts()throws Exception{
  var a=owner("invalid-dates@example.com");var auth=SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(a));
  var e=edition(a.getId(),"Minha HQ",MediaType.PHYSICAL,null,null,"25",null);
  for(String date:List.of("1899-12-31",LocalDate.now().plusDays(1).toString(),"não-data")){
   var response=mvc.perform(post("/edicoes/{id}/editar",e.getId()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("title","Rascunho conservado").param("referencePaidPrice","40").param("purchaseDate",date)).andExpect(status().isOk()).andReturn();
   assertThat(response.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).contains("Rascunho conservado","field-error").doesNotContain("Failed to convert");
   assertThat(finance.report(a.getId(),all(),0).summary().total()).isEqualByComparingTo("25");
   fixture("errors.html",response.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
  }
  mvc.perform(post("/edicoes").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("title","Negativa").param("referencePaidPrice","-1")).andExpect(status().isOk());
  assertThat(finance.report(a.getId(),all(),0).summary().count()).isEqualTo(1);
  mvc.perform(get("/financeiro").with(auth).param("start","invalid")).andExpect(status().isBadRequest());
  mvc.perform(get("/financeiro").with(auth).param("start","2026-06-01")).andExpect(status().isBadRequest());
  mvc.perform(get("/financeiro").with(auth).param("ranking","OTHER")).andExpect(status().isBadRequest());
  mvc.perform(get("/financeiro").with(auth).param("creatorId","invalid")).andExpect(status().isBadRequest());
  var b=owner("private-filter@example.com");var c=characters.save(b.getId(),null,"Privado");
  mvc.perform(get("/financeiro").with(auth).param("characterId",c.toString())).andExpect(status().isNotFound());
  mvc.perform(get("/financeiro")).andExpect(status().is3xxRedirection());
 }
 @Test void legacyIsNotCountedAndRemainsIntactAfterEditionDeletionAndRoutesAreRemoved()throws Exception{
  var a=owner("legacy@example.com");var e=edition(a.getId(),"HQ antiga",null,null,null,"30",null);legacy(a.getId(),e.getId(),"500");
  assertThat(finance.report(a.getId(),all(),0).summary().total()).isEqualByComparingTo("30");
  assertThat(editions.viewOwned(e.getId(),a.getId()).purchaseDate()).isNull();
  var auth=SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(a));
  mvc.perform(get("/financeiro/compras/nova").with(auth)).andExpect(status().isNotFound());
  mvc.perform(post("/financeiro/compras").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isNotFound());
  editions.delete(e.getId(),a.getId());
  assertThat(jdbc.queryForObject("select count(*) from financial_purchases where owner_user_id=? and edition_id is null",Long.class,a.getId())).isEqualTo(1);
  assertThat(jdbc.queryForObject("select amount from financial_purchases where owner_user_id=?",BigDecimal.class,a.getId())).isEqualByComparingTo("500");
  assertThat(jdbc.queryForObject("select notes from financial_purchases where owner_user_id=?",String.class,a.getId())).isEqualTo("Histórico preservado");
  assertThat(finance.report(a.getId(),all(),0).summary().count()).isZero();
 }
 @Test void comparesEqualPeriodsFillsMonthsAndReflectsReadingAndUnknownPrices()throws Exception{
  var a=owner("comparison@example.com");
  var e=edition(a.getId(),"Março",null,null,null,"150",LocalDate.of(2026,3,1));
  edition(a.getId(),"Fevereiro",null,null,null,"100",LocalDate.of(2026,2,1));
  edition(a.getId(),"Só data",null,null,null,null,LocalDate.of(2026,1,10));
  var r=finance.report(a.getId(),new FinanceFilter(LocalDate.of(2026,3,1),LocalDate.of(2026,3,31),null,null,null,null,null),0);
  assertThat(r.comparison().previous().total()).isEqualByComparingTo("100");assertThat(r.comparison().change()).isEqualTo("50,0%");
  var quarter=finance.report(a.getId(),new FinanceFilter(LocalDate.of(2026,1,1),LocalDate.of(2026,3,31),null,null,null,null,null),0);
  assertThat(quarter.monthly().items()).hasSize(3);assertThat(quarter.monthly().items().getFirst().amount()).isZero();
  assertThat(quarter.summary().unreadAmount()).isEqualByComparingTo("250");
  reading.mark(e.getId(),a.getId(),true);
  assertThat(finance.report(a.getId(),all(),0).summary().unreadAmount()).isEqualByComparingTo("100");
 }
 @Test void paginatesEditionsWithoutLimitingChartsOrTotals()throws Exception{
  var a=owner("pages@example.com");var auth=SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(a));
  for(int i=0;i<12;i++){var p=publishers.save(Publisher.create(a.getId(),"Editora "+String.format("%02d",i),null,null));for(int j=0;j<2;j++)edition(a.getId(),"HQ "+i+"-"+j,MediaType.PHYSICAL,p.getId(),null,"1",LocalDate.of(2026,6,1));}
  var r=finance.report(a.getId(),all(),1);assertThat(r.summary().total()).isEqualByComparingTo("24");assertThat(r.entries().getContent()).hasSize(4);assertThat(r.entries().getTotalElements()).isEqualTo(24);
  var chart=r.charts().stream().filter(x->x.parameter().equals("publisherId")).findFirst().orElseThrow();
  assertThat(chart.truncated()).isTrue();assertThat(chart.items().getLast().bucket().label()).isEqualTo("Outras editoras");assertThat(chart.items().stream().map(x->x.bucket().amount()).reduce(BigDecimal.ZERO,BigDecimal::add)).isEqualByComparingTo("24");
  mvc.perform(get("/financeiro").with(auth).param("ranking","QUANTITY").param("start","2026-06-01").param("end","2026-06-10")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("ranking=QUANTITY&amp;page=1")));
 }
 @Test void managesCharactersAndEditionBindingsAtomicallyWithDeletionProtection()throws Exception{
  var a=owner("character-mvc@example.com");var b=owner("character-other@example.com");var auth=SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(a));
  mvc.perform(post("/personagens").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("name","Batman")).andExpect(redirectedUrl("/personagens"));
  var character=characters.options(a.getId()).getFirst();assertThatThrownBy(()->characters.save(a.getId(),null," batman ")).isInstanceOf(CharacterService.CharacterRule.class);
  var foreign=characters.save(b.getId(),null,"Privado");
  mvc.perform(post("/edicoes").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("title","Não pode salvar").param("characterIds",foreign.toString())).andExpect(status().isNotFound());assertThat(jdbc.queryForObject("select count(*) from editions",Long.class)).isZero();
  mvc.perform(post("/edicoes").with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("title","Edição com personagem").param("characterIds",character.id().toString())).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern("/edicoes/*"));
  UUID id=jdbc.queryForObject("select id from editions",UUID.class);assertThat(characters.ids(a.getId(),id)).containsExactly(character.id());
  mvc.perform(get("/edicoes/{id}/editar",id).with(auth)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("checked=\"checked\"")));
  assertThatThrownBy(()->characters.delete(a.getId(),character.id())).isInstanceOf(CharacterService.CharacterRule.class);
  mvc.perform(post("/edicoes/{id}/editar",id).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("title","Edição com personagem").param("_characterIds","on")).andExpect(status().is3xxRedirection());assertThat(characters.ids(a.getId(),id)).isEmpty();
  mvc.perform(get("/personagens/{id}/editar",foreign).with(auth)).andExpect(status().isNotFound());
  mvc.perform(post("/personagens/{id}/excluir",character.id()).with(auth).with(SecurityMockMvcRequestPostProcessors.csrf()).param("confirmed","true")).andExpect(redirectedUrl("/personagens"));assertThat(characters.options(a.getId())).isEmpty();
 }
 private void fixture(String name,String html)throws Exception{
  var path=java.nio.file.Path.of("target/ui-finance");java.nio.file.Files.createDirectories(path.resolve("css"));java.nio.file.Files.writeString(path.resolve(name),html);
  for(String css:List.of("main.css","dashboard.css"))java.nio.file.Files.copy(java.nio.file.Path.of("src/main/resources/static/css",css),path.resolve("css").resolve(css),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
 }
 @Test void upgradesFromV14WithoutCopyingLegacyDatesOrChangingPreviousPrices(){
  String schema="edition_finance_upgrade_test";
  org.flywaydb.core.Flyway.configure().dataSource(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword()).schemas(schema).defaultSchema(schema).target("14").load().migrate();
  UUID owner=UUID.randomUUID(),edition=UUID.randomUUID(),purchase=UUID.randomUUID();
  jdbc.update("insert into "+schema+".users(id,email,password_hash,role) values(?,?,?,?)",owner,"upgrade@example.com","hash","USER");
  jdbc.update("insert into "+schema+".editions(id,owner_user_id,title,normalized_title,reference_paid_price,cover_price,media_type) values(?,?,?,?,?,?,?)",edition,owner,"HQ anterior","hq anterior",new BigDecimal("19.50"),new BigDecimal("99"),"DIGITAL");
  jdbc.update("insert into "+schema+".financial_purchases(id,owner_user_id,edition_id,amount,purchase_date,vendor,notes) values(?,?,?,?,?,?,?)",purchase,owner,edition,new BigDecimal("500"),LocalDate.of(2026,6,1),"Loja antiga","Anotação antiga");
  org.flywaydb.core.Flyway.configure().dataSource(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword()).schemas(schema).defaultSchema(schema).load().migrate();
  assertThat(jdbc.queryForObject("select reference_paid_price from "+schema+".editions where id=?",BigDecimal.class,edition)).isEqualByComparingTo("19.50");
  assertThat(jdbc.queryForObject("select purchase_date from "+schema+".editions where id=?",LocalDate.class,edition)).isNull();
  assertThat(jdbc.queryForObject("select amount from "+schema+".financial_purchases where id=?",BigDecimal.class,purchase)).isEqualByComparingTo("500");
  assertThat(jdbc.queryForObject("select purchase_date from "+schema+".financial_purchases where id=?",LocalDate.class,purchase)).isEqualTo(LocalDate.of(2026,6,1));
  assertThat(jdbc.queryForObject("select notes from "+schema+".financial_purchases where id=?",String.class,purchase)).isEqualTo("Anotação antiga");
 }
}
