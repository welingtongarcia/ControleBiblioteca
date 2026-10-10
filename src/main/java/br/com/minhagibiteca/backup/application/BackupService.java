package br.com.minhagibiteca.backup.application;

import br.com.minhagibiteca.backup.domain.BackupTables;
import br.com.minhagibiteca.backup.infrastructure.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;

@Service
public class BackupService {
    private final BackupRepository repository;
    private final BackupArchive archive;
    private final TransactionTemplate read,write;
    public BackupService(BackupRepository repository,BackupArchive archive,PlatformTransactionManager transactions) {
        this.repository=repository;this.archive=archive;
        read=new TransactionTemplate(transactions);read.setReadOnly(true);read.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        write=new TransactionTemplate(transactions);
    }
    public boolean empty(UUID owner) {return repository.empty(owner);}
    public BackupArchive.Download generate(UUID owner) {
        var generated=new java.util.concurrent.atomic.AtomicReference<BackupArchive.Download>();
        try {
            return read.execute(status->{
                var download=archive.generate(owner,repository.schemaVersion(),repository.snapshot(owner));generated.set(download);return download;
            });
        }catch(RuntimeException | Error failure) {
            if(generated.get()!=null)try{generated.get().close();}catch(RuntimeException cleanup){failure.addSuppressed(cleanup);}
            throw failure;
        }
    }
    public int restore(UUID destination,InputStream stream) {
        try(var pack=archive.read(stream)) {
            var data=validate(pack);int count=BackupTables.OWNED.stream().mapToInt(table->data.withArray(table).size()).sum();
            try {
                write.executeWithoutResult(status->{
                    repository.lock(destination);
                    if (!repository.empty(destination)) throw invalid("A conta já contém cadastros. A restauração só é permitida em uma conta vazia.");
                    var created=new ArrayList<Path>();
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override public void afterCompletion(int completion) {
                            if (completion==STATUS_ROLLED_BACK) for(var file:created) try {Files.deleteIfExists(file);} catch(IOException e) {throw new IllegalStateException("Não foi possível remover capa de restauração cancelada",e);}
                        }
                    });
                    rekey(data,destination);
                    // Parents have strictly earlier levels; sorting therefore also prevents FK ordering issues.
                    var locations=new ArrayList<JsonNode>();data.withArray("storage_locations").forEach(locations::add);
                    locations.sort(Comparator.comparingInt(row->level(row.path("level").asText())));
                    data.set("storage_locations",new ObjectMapper().valueToTree(locations));
                    for (JsonNode row:data.withArray("editions")) {
                        var edition=(ObjectNode)row;
                        if (!edition.path("cover_file_name").isNull()) {
                            String old=edition.path("cover_file_name").asText();String extension=old.substring(old.lastIndexOf('.'));
                            String name=edition.path("id").asText()+"-"+UUID.randomUUID()+extension;
                            Path target=archive.storage().resolve(name);
                            try {
                                Files.createDirectories(archive.storage());
                                // CREATE_NEW ensures that a restore never replaces an existing file.
                                try(var out=Files.newOutputStream(target,StandardOpenOption.CREATE_NEW)) {
                                    created.add(target);Files.copy(pack.files().get("capas/"+old),out);
                                }
                            } catch(IOException e) {throw new InvalidBackupException("Não foi possível gravar as capas. Nenhum cadastro foi restaurado.",e);}
                            edition.put("cover_file_name",name);
                        }
                    }
                    for(var table:BackupTables.all()) repository.insertAll(table,data.withArray(table));
                });
            } catch(DataAccessException e) {throw new InvalidBackupException("Os cadastros do backup são inválidos ou incompatíveis. Nenhum dado foi restaurado.",e);}
            return count;
        }
    }
    private ObjectNode validate(BackupArchive.Package pack) {
        var manifest=pack.manifest();
        if (!BackupArchive.fields(manifest).equals(Set.of("formatVersion","schemaVersion","applicationVersion","generatedAt","sourceOwner","files"))
                || !manifest.path("formatVersion").isInt() || manifest.path("formatVersion").intValue()!=1
                || !manifest.path("schemaVersion").isTextual() || !repository.schemaVersion().equals(manifest.path("schemaVersion").asText())
                || !manifest.path("applicationVersion").isTextual()) throw invalid("Versão do backup incompatível. Instale a mesma versão do sistema usada na geração.");
        try {Instant.parse(manifest.path("generatedAt").asText());}catch(RuntimeException e){throw invalid("Data do manifesto inválida.");}
        UUID source=uuid(manifest.path("sourceOwner"));var data=pack.data();
        if (!BackupArchive.fields(data).equals(new HashSet<>(BackupTables.all()))) throw invalid("Tabelas do backup inválidas ou incompletas.");
        var ids=new HashMap<String,Set<UUID>>();int count=0;
        for(var table:BackupTables.all()) {
            JsonNode rows=data.get(table);if (!rows.isArray())throw invalid("Lista de cadastros inválida.");
            count+=rows.size();if(count>BackupArchive.MAX_ROWS)throw invalid("Backup excede 100.000 registros.");
            Set<String> columns=repository.columns(table);var seen=new HashSet<UUID>();ids.put(table,seen);
            for(var row:rows) {
                if (!row.isObject() || !BackupArchive.fields(row).equals(columns)) throw invalid("Campos do backup inválidos ou incompatíveis.");
                if (columns.contains("owner_user_id") && !uuid(row.get("owner_user_id")).equals(source)) throw invalid("O pacote mistura proprietários de cadastros.");
                if (BackupTables.OWNED.contains(table) && !seen.add(uuid(row.get("id"))))throw invalid("Há identificadores duplicados no backup.");
                for (var cell:row) if(cell.isContainerNode()) throw invalid("Campo de cadastro inválido.");
            }
        }
        for(var table:BackupTables.all()) for(var row:data.withArray(table)) for(var ref:BackupTables.references(table).entrySet()) {
            var value=row.get(ref.getKey());
            if (!value.isNull() && !ids.get(ref.getValue()).contains(uuid(value))) throw invalid("Um vínculo aponta para um cadastro fora do backup.");
        }
        var locations=new HashMap<UUID,JsonNode>();for(var row:data.withArray("storage_locations"))locations.put(uuid(row.get("id")),row);
        for(var row:locations.values()) {
            int child=level(row.path("level").asText());JsonNode parent=row.get("parent_id");
            if (!parent.isNull() && level(locations.get(uuid(parent)).path("level").asText())>=child) throw invalid("Hierarquia de localizações inválida.");
        }
        var covers=new HashSet<String>();
        for(var row:data.withArray("editions")) {
            var name=row.get("cover_file_name");
            if (name.isNull()) {
                if (!row.get("cover_content_type").isNull() || !row.get("cover_updated_at").isNull()) throw invalid("Metadados de capa incompletos.");
            } else {
                if (!name.isTextual() || !BackupArchive.coverName(name.asText()) || !covers.add("capas/"+name.asText()))throw invalid("Nome de capa inválido ou repetido.");
                Path file=pack.files().get("capas/"+name.asText());if(file==null)throw invalid("Uma capa do cadastro está ausente no backup.");
                try {archive.validateImage(file,row.path("cover_content_type").asText(),name.asText());}
                catch(IOException e){throw new InvalidBackupException("Capa inválida no backup.",e);}
            }
        }
        var actual=new HashSet<String>(pack.files().keySet());actual.removeAll(Set.of("manifesto.json","dados.json","LEIA-ME.txt"));
        if(!actual.equals(covers))throw invalid("O pacote contém capas sem cadastro correspondente.");
        return data;
    }
    private void rekey(ObjectNode data,UUID owner) {
        var ids=new HashMap<String,Map<UUID,UUID>>();
        for(var table:BackupTables.OWNED) {
            var map=new HashMap<UUID,UUID>();ids.put(table,map);
            for(var row:data.withArray(table))map.put(uuid(row.get("id")),UUID.randomUUID());
        }
        for(var table:BackupTables.all())for(var node:data.withArray(table)) {
            var row=(ObjectNode)node;
            if(row.has("owner_user_id"))row.put("owner_user_id",owner.toString());
            if(BackupTables.OWNED.contains(table))row.put("id",ids.get(table).get(uuid(row.get("id"))).toString());
            for(var ref:BackupTables.references(table).entrySet()) if(!row.get(ref.getKey()).isNull())row.put(ref.getKey(),ids.get(ref.getValue()).get(uuid(row.get(ref.getKey()))).toString());
        }
    }
    private static UUID uuid(JsonNode node) {
        if(node==null || !node.isTextual() || !node.asText().matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))throw invalid("Identificador de cadastro inválido.");
        return UUID.fromString(node.asText());
    }
    private static int level(String name) {return switch(name){case "ROOM"->0;case "SHELF_UNIT"->1;case "SHELF"->2;case "BOX"->3;default->throw invalid("Nível de localização inválido.");};}
    private static InvalidBackupException invalid(String message){return new InvalidBackupException(message);}
}
