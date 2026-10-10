package br.com.minhagibiteca.reports;

import br.com.minhagibiteca.auth.application.UserPrincipal;
import br.com.minhagibiteca.auth.domain.UserAccount;
import br.com.minhagibiteca.auth.domain.UserRole;
import br.com.minhagibiteca.auth.infrastructure.UserAccountRepository;
import br.com.minhagibiteca.reports.application.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Testcontainers
class ReportIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",POSTGRES::getJdbcUrl);r.add("spring.datasource.username",POSTGRES::getUsername);r.add("spring.datasource.password",POSTGRES::getPassword);
    }
    @Autowired JdbcTemplate jdbc; @Autowired MockMvc mvc; @Autowired UserAccountRepository users; @Autowired ReportService reports;
    UserAccount owner,other;UUID series,publisher,creator,artist,character,physical;
    @BeforeEach void setup() {
        for(String table:List.of("financial_purchases","edition_characters","comic_characters","physical_copies","series_checklist_items","edition_creators","edition_works","editions","creators","works","storage_locations","series","publishers","users")) jdbc.update("delete from "+table);
        owner=users.save(UserAccount.create("reports@example.com","hash",UserRole.USER));other=users.save(UserAccount.create("reports-other@example.com","hash",UserRole.USER));
        series=UUID.randomUUID();publisher=UUID.randomUUID();creator=UUID.randomUUID();artist=UUID.randomUUID();character=UUID.randomUUID();
        jdbc.update("insert into publishers(id,owner_user_id,name,normalized_name) values (?,?,'Editora do relatório','editora do relatório')",publisher,owner.getId());
        jdbc.update("insert into series(id,owner_user_id,name,normalized_name,status) values (?,?,'Coleção do relatório','coleção do relatório','ONGOING')",series,owner.getId());
        for(var id:List.of(creator,artist)) jdbc.update("insert into creators(id,owner_user_id,name,normalized_name) values (?,?,?,?)",id,owner.getId(),id.equals(creator)?"João":"Maria",id.toString());
        jdbc.update("insert into creator_functions values (?, 'ROTEIRISTA'), (?, 'DESENHISTA')",creator,artist);
        jdbc.update("insert into comic_characters(id,owner_user_id,name,normalized_name) values (?,?,'Herói','herói')",character,owner.getId());
        physical=edition(owner.getId(),"Relatório físico","PHYSICAL",new BigDecimal("15.50"),40,10,false,"2026-01-01");
        edition(owner.getId(),"Relatório digital","DIGITAL",BigDecimal.ZERO,20,20,true,"2026-01-02");
        edition(owner.getId(),"Relatório incompleto",null,null,null,0,false,null);
        edition(other.getId(),"SEGREDO DE OUTRO USUÁRIO","DIGITAL",new BigDecimal("999"),null,0,false,null);
        jdbc.update("insert into edition_creators(edition_id,creator_id,creator_role) values (?,?,'OUTRO'), (?,?,'ROTEIRISTA'), (?,?,'OUTRO')",physical,creator,physical,creator,physical,artist);
        jdbc.update("insert into edition_characters values (?,?,?)",owner.getId(),physical,character);
        for(int i=0;i<2;i++) jdbc.update("insert into physical_copies(id,owner_user_id,edition_id,condition,acquisition_price,created_at,updated_at) values (?,?,?,'NEW',500,now(),now())",UUID.randomUUID(),owner.getId(),physical);
    }
    UUID edition(UUID user,String title,String media,BigDecimal paid,Integer pages,int read,boolean isRead,String date) {
        var id=UUID.randomUUID();
        jdbc.update("insert into editions(id,owner_user_id,title,normalized_title,media_type,reference_paid_price,total_pages,pages_read,is_read,purchase_date,series_id,publisher_id) values (?,?,?,?,?,?,?,?,?,cast(? as date),?,?)",
                id,user,title,title.toLowerCase(),media,paid,pages,read,isRead,date,user.equals(owner.getId())?series:null,user.equals(owner.getId())?publisher:null);
        return id;
    }
    ReportFilter filter(String q,UUID c,br.com.minhagibiteca.catalog.domain.CreatorFunction f) {return new ReportFilter(q,null,null,c,f,null,"ALL","ALL",null,null);}
    @Test void calculatesSummaryWithoutDoubleCountingAndRendersPrivatePreview() throws Exception {
        var data=reports.preview(owner.getId(),filter("Relatório",null,null),0);var s=data.summary();
        assertThat(s.editions()).isEqualTo(3);assertThat(s.copies()).isEqualTo(2);assertThat(s.physical()).isEqualTo(1);assertThat(s.digital()).isEqualTo(1);
        assertThat(s.paid()).isEqualByComparingTo("15.50");assertThat(s.unpriced()).isEqualTo(1);assertThat(s.totalPages()).isEqualTo(60);
        assertThat(s.pagesRead()).isEqualTo(30);assertThat(s.pagesUnread()).isEqualTo(30);assertThat(s.unpaged()).isEqualTo(1);
        assertThat(reports.preview(owner.getId(),filter("Relatório incompleto",null,null),0).summary().paidLabel()).isEqualTo("Não informado");
        assertThat(reports.preview(owner.getId(),filter("Relatório digital",null,null),0).summary().paidLabel()).contains("0,00");
        var result=mvc.perform(get("/relatorios").with(SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner))))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("Baixar PDF","Baixar CSV","15,50","Função geral do criador","Maria (Desenhista)")
                .doesNotContain("SEGREDO DE OUTRO USUÁRIO");
        preview("filled",result.getResponse().getContentAsString());
        var empty=mvc.perform(get("/relatorios").param("q","Nada encontrado").with(SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner))))
                .andExpect(status().isOk()).andReturn();
        preview("empty",empty.getResponse().getContentAsString());
    }
    @Test void combinesCreatorFunctionsAndAllBibliographicAndPeriodFilters() throws Exception {
        assertThat(reports.preview(owner.getId(),filter("",creator,br.com.minhagibiteca.catalog.domain.CreatorFunction.DESENHISTA),0).summary().editions()).isZero();
        assertThat(reports.preview(owner.getId(),filter("",null,br.com.minhagibiteca.catalog.domain.CreatorFunction.DESENHISTA),0).summary().editions()).isEqualTo(1);
        var dated=new ReportFilter("",null,null,null,null,null,"ALL","ALL",java.time.LocalDate.of(2026,1,1),java.time.LocalDate.of(2026,1,2));
        assertThat(reports.preview(owner.getId(),dated,0).summary().editions()).isEqualTo(2);
        assertThat(reports.preview(owner.getId(),new ReportFilter("",null,null,null,null,null,"ALL","READ",null,null),0).summary().editions()).isEqualTo(1);
        var result=mvc.perform(get("/relatorios").with(SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner)))
                .param("type","CATALOG").param("q","Relatório").param("seriesId",series.toString()).param("publisherId",publisher.toString())
                .param("creatorId",creator.toString()).param("creatorFunction","ROTEIRISTA").param("characterId",character.toString())
                .param("mediaType","PHYSICAL").param("reading","UNREAD").param("start","2026-01-01").param("end","2026-01-01"))
                .andExpect(status().isOk()).andReturn();
        var data=(ReportData)result.getModelAndView().getModel().get("report");assertThat(data.summary().editions()).isEqualTo(1);
        assertThat(data.filters()).contains("Coleção: Coleção do relatório","Criador: João","Função geral: Roteirista");
        assertThat(result.getResponse().getContentAsString()).contains("creatorFunction=ROTEIRISTA","type=CATALOG","returnTo=");
        preview("filtered",result.getResponse().getContentAsString());
        assertThat(reports.preview(owner.getId(),filter("%",null,null),0).summary().editions()).isZero();
        assertThat(reports.preview(owner.getId(),new ReportFilter("",null,null,null,null,null,"UNSPECIFIED","ALL",null,null),0).summary().editions()).isEqualTo(1);
    }
    @Test void exportsAllPagesAndProtectsDownloadsAndInvalidFilters() throws Exception {
        for(int i=0;i<22;i++) edition(owner.getId(),"Acervo "+i,"DIGITAL",BigDecimal.ONE,null,0,false,null);
        var auth=SecurityMockMvcRequestPostProcessors.user(UserPrincipal.from(owner));
        assertThat(reports.preview(owner.getId(),filter("",null,null),0).entries().getContent()).hasSize(20);
        assertThat(reports.preview(owner.getId(),filter("",null,null),999).entries().getNumber()).isEqualTo(1);
        var csv=mvc.perform(get("/relatorios/csv").with(auth).param("type","CATALOG").param("page","1"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(header().string("Content-Disposition","attachment; filename=\"catalogo-acervo.csv\"")).andReturn();
        assertThat(csv.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).contains("Acervo 21","Relatório físico").doesNotContain("SEGREDO DE OUTRO USUÁRIO");
        var pdf=mvc.perform(get("/relatorios/pdf").with(auth).param("q","Relatório").param("type","CATALOG"))
                .andExpect(status().isOk()).andExpect(content().contentType("application/pdf")).andReturn();
        try(var doc=Loader.loadPDF(pdf.getResponse().getContentAsByteArray())) {assertThat(new PDFTextStripper().getText(doc)).contains("Relatório físico","João (Roteirista)","Não informado").doesNotContain("SEGREDO DE OUTRO USUÁRIO");}
        for(var path:List.of("/relatorios","/relatorios/csv","/relatorios/pdf")) {
            mvc.perform(get(path)).andExpect(status().is3xxRedirection());
            mvc.perform(get(path).with(auth).param("seriesId",UUID.randomUUID().toString())).andExpect(status().isNotFound());
            mvc.perform(get(path).with(auth).param("mediaType","INVALID")).andExpect(status().isBadRequest());
            mvc.perform(get(path).with(auth).param("creatorFunction","INVALID")).andExpect(status().isBadRequest());
            mvc.perform(get(path).with(auth).param("start","2026-01-02")).andExpect(status().isBadRequest());
        }
        assertThat(reports.preview(other.getId(),filter("",null,null),0).summary().paid()).isEqualByComparingTo("999");
    }
    void preview(String name,String html) throws Exception {
        var root=Path.of("target/ui-reports");Files.createDirectories(root.resolve("css"));Files.createDirectories(root.resolve("js"));
        Files.writeString(root.resolve(name+".html"),html);
        for(var css:List.of("main","dashboard","home-showcase")) Files.copy(Path.of("src/main/resources/static/css/"+css+".css"),root.resolve("css/"+css+".css"),StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Path.of("src/main/resources/static/js/sidebar.js"),root.resolve("js/sidebar.js"),StandardCopyOption.REPLACE_EXISTING);
    }
}
