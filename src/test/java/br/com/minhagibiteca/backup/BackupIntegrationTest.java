package br.com.minhagibiteca.backup;

import br.com.minhagibiteca.auth.application.UserPrincipal;
import br.com.minhagibiteca.auth.domain.*;
import br.com.minhagibiteca.auth.infrastructure.UserAccountRepository;
import br.com.minhagibiteca.backup.application.*;
import br.com.minhagibiteca.backup.domain.BackupTables;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Testcontainers
class BackupIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:16-alpine");
    static final Path COVERS=Path.of("target/test-backup-covers").toAbsolutePath();
    static final ObjectMapper JSON=new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",POSTGRES::getJdbcUrl);r.add("spring.datasource.username",POSTGRES::getUsername);r.add("spring.datasource.password",POSTGRES::getPassword);
        r.add("app.cover-storage-path",COVERS::toString);
    }
    @Autowired JdbcTemplate jdbc;@Autowired MockMvc mvc;@Autowired UserAccountRepository users;@Autowired BackupService service;
    UserAccount owner,other,destination;UUID publisher,series,creator,character,work,room,box,physical,digital;String cover;byte[] image;
    @BeforeEach void setup() throws Exception {
        for(String table:List.of("financial_purchases","edition_characters","physical_copies","series_checklist_items","edition_creators","edition_works","editions","creator_functions","creators","works","comic_characters","storage_locations","series","publishers","users"))jdbc.update("delete from "+table);
        Files.createDirectories(COVERS);try(var entries=Files.newDirectoryStream(COVERS)){for(var file:entries)Files.delete(file);}
        owner=users.save(UserAccount.create("backup@example.com","HASH-SECRETO",UserRole.USER));other=users.save(UserAccount.create("other@example.com","hash",UserRole.USER));destination=users.save(UserAccount.create("destination@example.com","hash",UserRole.USER));
        publisher=UUID.randomUUID();series=UUID.randomUUID();creator=UUID.randomUUID();character=UUID.randomUUID();work=UUID.randomUUID();room=UUID.randomUUID();box=UUID.randomUUID();physical=UUID.randomUUID();digital=UUID.randomUUID();
        jdbc.update("insert into publishers(id,owner_user_id,name,normalized_name,country_code,notes) values (?,?,'Editora brasileira','editora brasileira','BR','Observação: ação')",publisher,owner.getId());
        jdbc.update("insert into series(id,owner_user_id,publisher_id,name,normalized_name,status,start_year) values (?,?,?,'Coleção','coleção','ONGOING',2024)",series,owner.getId(),publisher);
        jdbc.update("insert into creators(id,owner_user_id,name,normalized_name,sort_name) values (?,?,'João','joão','João')",creator,owner.getId());
        jdbc.update("insert into creator_functions values (?,'ROTEIRISTA'), (?,'DESENHISTA')",creator,creator);
        jdbc.update("insert into works(id,owner_user_id,title,normalized_title,summary) values (?,?,'Obra','obra','Resumo')",work,owner.getId());
        jdbc.update("insert into comic_characters(id,owner_user_id,name,normalized_name) values (?,?,'Herói','herói')",character,owner.getId());
        jdbc.update("insert into storage_locations(id,owner_user_id,level,name,normalized_name) values (?,?,'ROOM','Quarto','quarto')",room,owner.getId());
        jdbc.update("insert into storage_locations(id,owner_user_id,parent_id,level,name,normalized_name) values (?,?,?,'BOX','Caixa','caixa')",box,owner.getId(),room);
        cover=physical+"-"+UUID.randomUUID()+".png";
        var png=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",png);image=png.toByteArray();Files.write(COVERS.resolve(cover),image);
        jdbc.update("insert into editions(id,owner_user_id,publisher_id,series_id,title,normalized_title,editorial_number,isbn,media_type,cover_price,reference_paid_price,purchase_date,total_pages,pages_read,is_read,cover_file_name,cover_content_type,cover_updated_at,notes) values (?,?,?,?,'HQ física','hq física','1','9786559601325','PHYSICAL',40.90,29.95,'2026-02-03',100,35,false,?,'image/png',now(),'Minha anotação')",physical,owner.getId(),publisher,series,cover);
        jdbc.update("insert into editions(id,owner_user_id,series_id,title,normalized_title,media_type,reference_paid_price,total_pages,pages_read,is_read) values (?,?,?,'HQ digital','hq digital','DIGITAL',0,20,20,true)",digital,owner.getId(),series);
        jdbc.update("insert into edition_works values (?,?,1)",physical,work);
        jdbc.update("insert into edition_creators values (?,?,'ROTEIRISTA',1), (?,?,'DESENHISTA',2)",physical,creator,physical,creator);
        jdbc.update("insert into edition_characters values (?,?,?)",owner.getId(),physical,character);
        jdbc.update("insert into physical_copies(id,owner_user_id,edition_id,storage_location_id,condition,acquisition_price,acquisition_date,created_at,updated_at) values (?,?,?,?,'GOOD',12.34,'2026-02-03',now(),now())",UUID.randomUUID(),owner.getId(),physical,box);
        jdbc.update("insert into series_checklist_items(id,owner_user_id,series_id,editorial_number,normalized_number,label,owned,notes,created_at,updated_at) values (?,?,?,'1','1','Primeira',true,'Completa',now(),now())",UUID.randomUUID(),owner.getId(),series);
        jdbc.update("insert into financial_purchases(id,owner_user_id,edition_id,amount,notes) values (?,?,?,9.87,'Compra legada'), (?,?,null,8.76,'Compra sem edição')",UUID.randomUUID(),owner.getId(),physical,UUID.randomUUID(),owner.getId());
        jdbc.update("insert into editions(id,owner_user_id,title,normalized_title) values (?,?,'SEGREDO DE OUTRA CONTA','segredo')",UUID.randomUUID(),other.getId());
    }
    byte[] export() throws Exception {try(var file=service.generate(owner.getId())){return Files.readAllBytes(file.zip());}}
    @Test void restoresCompletePortableCollectionWithoutCredentialsOrOtherOwners() throws Exception {
        var original=export();var unpacked=unzip(original);assertThat(unpacked.keySet()).containsExactlyInAnyOrder("manifesto.json","dados.json","LEIA-ME.txt","capas/"+cover);
        String json=new String(unpacked.get("dados.json"),java.nio.charset.StandardCharsets.UTF_8);
        assertThat(json).contains("Minha anotação","29.95","Compra sem edição").doesNotContain("SEGREDO","HASH-SECRETO","backup@example.com","users","api_key");
        var source=JSON.readTree(json);
        assertThat(service.restore(destination.getId(),new ByteArrayInputStream(original))).isEqualTo(13);
        for(var table:BackupTables.OWNED)assertThat(jdbc.queryForObject("select count(*) from "+table+" where owner_user_id=?",Integer.class,destination.getId())).isEqualTo(source.get(table).size());
        UUID edition=jdbc.queryForObject("select id from editions where owner_user_id=? and media_type='PHYSICAL'",UUID.class,destination.getId());assertThat(edition).isNotEqualTo(physical);
        var restored=jdbc.queryForMap("select * from editions where id=?",edition);
        assertThat(restored.get("reference_paid_price").toString()).isEqualTo("29.95");assertThat(restored.get("purchase_date").toString()).isEqualTo("2026-02-03");
        assertThat(restored.get("total_pages")).isEqualTo(100);assertThat(restored.get("pages_read")).isEqualTo(35);assertThat(restored.get("is_read")).isEqualTo(false);
        assertThat(restored.get("created_at").toString()).isEqualTo(jdbc.queryForMap("select created_at from editions where id=?",physical).get("created_at").toString());
        assertThat(Files.readAllBytes(COVERS.resolve((String)restored.get("cover_file_name")))).isEqualTo(image);assertThat(restored.get("cover_file_name")).isNotEqualTo(cover);
        assertThat(jdbc.queryForObject("select count(*) from edition_creators where edition_id=?",Integer.class,edition)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from creator_functions f join creators c on c.id=f.creator_id where c.owner_user_id=?",Integer.class,destination.getId())).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from edition_works where edition_id=?",Integer.class,edition)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from edition_characters where edition_id=? and owner_user_id=?",Integer.class,edition,destination.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from physical_copies p join storage_locations l on l.id=p.storage_location_id join storage_locations r on r.id=l.parent_id where p.edition_id=? and r.level='ROOM'",Integer.class,edition)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from series_checklist_items where owner_user_id=? and owned",Integer.class,destination.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select sum(amount) from financial_purchases where owner_user_id=?",java.math.BigDecimal.class,destination.getId())).isEqualByComparingTo("18.63");
        assertThat(jdbc.queryForObject("select count(*) from editions where owner_user_id=?",Integer.class,other.getId())).isEqualTo(1);
    }
    @Test void mvcProtectsActionsWithCsrfAndRendersBothAccountStates() throws Exception {
        mvc.perform(get("/backup")).andExpect(status().is3xxRedirection());mvc.perform(post("/backup/gerar").with(user(UserPrincipal.from(owner)))).andExpect(status().isForbidden());
        mvc.perform(multipart("/backup/restaurar").file(new MockMultipartFile("arquivo","backup.zip","application/zip",export())).with(user(UserPrincipal.from(destination)))).andExpect(status().isForbidden());
        var result=mvc.perform(get("/backup").with(user(UserPrincipal.from(destination)))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("Backup e migração","Baixar backup completo","name=\"_csrf\"","name=\"confirm\"");preview("empty",result.getResponse().getContentAsString());
        var filled=mvc.perform(get("/backup").with(user(UserPrincipal.from(owner)))).andExpect(status().isOk()).andReturn();assertThat(filled.getResponse().getContentAsString()).contains("a restauração está bloqueada","disabled");preview("filled",filled.getResponse().getContentAsString());
        var zip=mvc.perform(post("/backup/gerar").with(user(UserPrincipal.from(owner))).with(csrf())).andExpect(status().isOk()).andExpect(content().contentType("application/zip"))
                .andExpect(header().string("Cache-Control","no-store")).andExpect(header().string("Content-Disposition","attachment; filename=\"minha-gibiteca-backup.zip\"")).andReturn().getResponse().getContentAsByteArray();
        mvc.perform(multipart("/backup/restaurar").file(new MockMultipartFile("arquivo","backup.zip","application/zip",zip)).param("confirm","true").with(user(UserPrincipal.from(destination))).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/backup")).andExpect(flash().attributeExists("backupSuccess"));
    }
    @Test void rejectsOccupiedAccountAndMissingConfirmationWithoutMutation() throws Exception {
        assertThatThrownBy(()->service.restore(owner.getId(),new ByteArrayInputStream(export()))).isInstanceOf(InvalidBackupException.class).hasMessageContaining("conta vazia");
        mvc.perform(multipart("/backup/restaurar").file(new MockMultipartFile("arquivo","backup.zip","application/zip",export())).with(user(UserPrincipal.from(destination))).with(csrf()))
                .andExpect(redirectedUrl("/backup")).andExpect(flash().attribute("backupError","Confirme que deseja restaurar em uma conta vazia."));
        assertEmpty();assertThat(files()).hasSize(1);
    }
    @Test void rejectsCorruptionIncompatibleVersionsAndUnsafeEntries() throws Exception {
        var entries=unzip(export());entries.put("dados.json","{}".getBytes());reject(zip(entries),"corrompido");
        entries=unzip(export());var manifest=(ObjectNode)JSON.readTree(entries.get("manifesto.json"));manifest.put("schemaVersion","999");entries.put("manifesto.json",JSON.writeValueAsBytes(manifest));reject(zip(entries),"incompatível");
        entries=unzip(export());entries.put("../escape.txt",new byte[]{1});reject(zip(entries),"não permitidos");
        entries=unzip(export());entries.remove("capas/"+cover);reject(zip(entries),"Manifesto");reject(new byte[]{1,2,3},"backup completo");assertEmpty();
    }
    @Test void rejectsExternalReferencesMixedOwnersAndUnknownColumns() throws Exception {
        reject(modify(data->((ObjectNode)data.withArray("editions").get(0)).put("publisher_id",UUID.randomUUID().toString())),"fora do backup");
        reject(modify(data->((ObjectNode)data.withArray("publishers").get(0)).put("owner_user_id",other.getId().toString())),"proprietários");
        reject(modify(data->((ObjectNode)data.withArray("creators").get(0)).put("sql","drop table users")),"Campos");assertEmpty();
    }
    @Test void rejectsCyclesDuplicateIdsInvalidImagesAndPartialJson() throws Exception {
        reject(modify(data->{for(var row:data.withArray("storage_locations"))if(row.path("level").asText().equals("ROOM"))((ObjectNode)row).put("parent_id",box.toString());}),"Hierarquia");
        reject(modify(data->{var rows=data.withArray("editions");rows.add(rows.get(0).deepCopy());}),"duplicados");
        var entries=unzip(export());entries.put("capas/"+cover,new byte[]{1,2,3});sign(entries);reject(zip(entries),"Capa");
        entries=unzip(export());entries.put("dados.json",(new String(entries.get("dados.json"),java.nio.charset.StandardCharsets.UTF_8)+" {}").getBytes(java.nio.charset.StandardCharsets.UTF_8));sign(entries);reject(zip(entries),"JSON");assertEmpty();
    }
    @Test void rollsBackEveryRowAndNewCoverWhenDatabaseConstraintFails() throws Exception {
        byte[] invalid=modify(data->((ObjectNode)data.withArray("financial_purchases").get(0)).put("amount",-1));
        reject(invalid,"Nenhum dado");assertEmpty();assertThat(files()).containsExactly(cover);
        assertThat(service.restore(destination.getId(),new ByteArrayInputStream(export()))).isPositive();
    }
    @Test void refusesIncompleteExportAndDoesNotFollowCoverSymlinks() throws Exception {
        Files.delete(COVERS.resolve(cover));assertThatThrownBy(()->service.generate(owner.getId())).isInstanceOf(InvalidBackupException.class).hasMessageContaining("capa");
        mvc.perform(post("/backup/gerar").with(user(UserPrincipal.from(owner))).with(csrf())).andExpect(redirectedUrl("/backup")).andExpect(flash().attributeExists("backupError"));
        Path target=Files.createTempFile("gibiteca-image-test-",".png");try{Files.write(target,image);Files.createSymbolicLink(COVERS.resolve(cover),target);
            assertThatThrownBy(()->service.generate(owner.getId())).isInstanceOf(InvalidBackupException.class).hasMessageContaining("capa");
        }finally{Files.deleteIfExists(COVERS.resolve(cover));Files.deleteIfExists(target);}
    }
    @Test void concurrentRestoresAreSerializedAndOnlyOneCanPopulateDestination() throws Exception {
        var bytes=export();var start=new java.util.concurrent.CountDownLatch(1);
        try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<String> task=()->{start.await();try{service.restore(destination.getId(),new ByteArrayInputStream(bytes));return "success";}catch(InvalidBackupException e){return e.getMessage();}};
            var first=executor.submit(task);var second=executor.submit(task);start.countDown();
            assertThat(List.of(first.get(20,java.util.concurrent.TimeUnit.SECONDS),second.get(20,java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("success","A conta já contém cadastros. A restauração só é permitida em uma conta vazia.");
        }
        assertThat(jdbc.queryForObject("select count(*) from editions where owner_user_id=?",Integer.class,destination.getId())).isEqualTo(2);assertThat(files()).hasSize(2);
    }
    @Test void rejectsTooManyRowsAndDuplicateLinksWithRollback() throws Exception {
        reject(modify(data->{var rows=data.putArray("publishers");for(int i=0;i<=100_000;i++)rows.addObject();}),"100.000");
        reject(modify(data->{var rows=data.withArray("edition_creators");rows.add(rows.get(0).deepCopy());}),"Nenhum dado");assertEmpty();assertThat(files()).containsExactly(cover);
    }
    void reject(byte[] bytes,String text){assertThatThrownBy(()->service.restore(destination.getId(),new ByteArrayInputStream(bytes))).isInstanceOf(InvalidBackupException.class).hasMessageContaining(text);}
    void assertEmpty(){assertThat(service.empty(destination.getId())).isTrue();}
    List<String> files() throws IOException {try(var entries=Files.list(COVERS)){return entries.map(p->p.getFileName().toString()).sorted().toList();}}
    byte[] modify(Consumer<ObjectNode> action) throws Exception {var entries=unzip(export());var data=(ObjectNode)JSON.readTree(entries.get("dados.json"));action.accept(data);entries.put("dados.json",JSON.writeValueAsBytes(data));sign(entries);return zip(entries);}
    static Map<String,byte[]> unzip(byte[] bytes) throws IOException {var entries=new LinkedHashMap<String,byte[]>();try(var zip=new ZipInputStream(new ByteArrayInputStream(bytes))){ZipEntry entry;while((entry=zip.getNextEntry())!=null)entries.put(entry.getName(),zip.readAllBytes());}return entries;}
    static byte[] zip(Map<String,byte[]> entries) throws IOException {var out=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(out)){for(var entry:entries.entrySet()){zip.putNextEntry(new ZipEntry(entry.getKey()));zip.write(entry.getValue());zip.closeEntry();}}return out.toByteArray();}
    static void sign(Map<String,byte[]> entries) throws Exception {var manifest=(ObjectNode)JSON.readTree(entries.get("manifesto.json"));var hashes=(ObjectNode)manifest.get("files");for(var entry:entries.entrySet())if(!entry.getKey().equals("manifesto.json"))hashes.putObject(entry.getKey()).put("size",entry.getValue().length).put("sha256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(entry.getValue())));entries.put("manifesto.json",JSON.writeValueAsBytes(manifest));}
    void preview(String name,String html) throws Exception {
        var root=Path.of("target/ui-backup");Files.createDirectories(root.resolve("css"));Files.createDirectories(root.resolve("js"));Files.writeString(root.resolve(name+".html"),html);
        for(var css:List.of("main","dashboard","home-showcase"))Files.copy(Path.of("src/main/resources/static/css/"+css+".css"),root.resolve("css/"+css+".css"),StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Path.of("src/main/resources/static/js/sidebar.js"),root.resolve("js/sidebar.js"),StandardCopyOption.REPLACE_EXISTING);
    }
}
