package br.com.minhagibiteca.backup.infrastructure;

import br.com.minhagibiteca.backup.application.InvalidBackupException;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.Instant;
import java.util.*;
import java.util.zip.*;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** ZIP é transportado como dados, nunca extraído em caminhos fornecidos pelo usuário. */
@Component
public class BackupArchive {
    public static final long MAX_ZIP=250L*1024*1024, MAX_TOTAL=512L*1024*1024, MAX_JSON=25L*1024*1024, MAX_COVER=5L*1024*1024;
    public static final int MAX_ENTRIES=20_000, MAX_ROWS=100_000;
    private static final String UUID_RE="[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    private final Path storage;
    private final ObjectMapper mapper;
    public BackupArchive(@Value("${app.cover-storage-path}") String storage) {
        this.storage=Path.of(storage).toAbsolutePath().normalize();
        var factory=JsonFactory.builder().streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(64).maxStringLength(100_000).maxNumberLength(128).build())
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
        this.mapper=new ObjectMapper(factory).enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    }
    public record Package(Path root, ObjectNode data, ObjectNode manifest, Map<String,Path> files) implements AutoCloseable {
        @Override public void close() { cleanup(root); }
    }
    public record Download(Path root, Path zip) implements AutoCloseable {
        @Override public void close() { cleanup(root); }
    }
    public Download generate(UUID owner, String schema, ObjectNode data) {
        Path root=temp();
        try {
            var files=new LinkedHashMap<String,Path>();
            byte[] json=mapper.writeValueAsBytes(data);
            if (json.length>MAX_JSON) throw invalid("Os dados excedem 25 MB.");
            files.put("dados.json",write(root,json));
            byte[] instructions=guide().getBytes(StandardCharsets.UTF_8);
            files.put("LEIA-ME.txt",write(root,instructions));long total=json.length+instructions.length;
            for (JsonNode edition:data.withArray("editions")) {
                JsonNode name=edition.get("cover_file_name");
                if (name==null || name.isNull()) continue;
                if (!name.isTextual() || !coverName(name.asText())) throw invalid("Nome de capa inválido. Corrija o cadastro antes de gerar o backup.");
                Path source=storage.resolve(name.asText());
                if (!Files.isRegularFile(source,LinkOption.NOFOLLOW_LINKS) || Files.size(source)>MAX_COVER)
                    throw invalid("Uma capa está ausente ou inválida. Corrija o cadastro e gere o backup novamente.");
                String entry="capas/"+name.asText();
                if (files.containsKey(entry)) throw invalid("Há capas com nomes duplicados.");
                if (files.size()+2>MAX_ENTRIES) throw invalid("O backup excede 20.000 arquivos.");
                Path copy=Files.createTempFile(root,"entry-",".tmp");
                try (var input=Files.newInputStream(source,LinkOption.NOFOLLOW_LINKS);var output=Files.newOutputStream(copy)) { total+=boundedCopy(input,output,Math.min(MAX_COVER,MAX_TOTAL-total)); }
                validateImage(copy,edition.path("cover_content_type").asText(),name.asText());
                files.put(entry,copy);
            }
            if (files.size()+1>MAX_ENTRIES) throw invalid("O backup excede 20.000 arquivos.");
            var manifest=mapper.createObjectNode(); manifest.put("formatVersion",1);manifest.put("schemaVersion",schema);
            manifest.put("applicationVersion","0.1.0-SNAPSHOT");manifest.put("generatedAt",Instant.now().toString());manifest.put("sourceOwner",owner.toString());
            var hashes=manifest.putObject("files");
            for (var entry:files.entrySet()) {
                long size=Files.size(entry.getValue());
                hashes.putObject(entry.getKey()).put("size",size).put("sha256",hash(entry.getValue()));
            }
            byte[] metadata=mapper.writeValueAsBytes(manifest);
            if (total+metadata.length>MAX_TOTAL) throw invalid("O backup excede 512 MB descompactados.");
            files.put("manifesto.json",write(root,metadata));
            Path zip=root.resolve("minha-gibiteca-backup.zip");
            try(var out=new ZipOutputStream(Files.newOutputStream(zip),StandardCharsets.UTF_8)) {
                for(var entry:files.entrySet()) {out.putNextEntry(new ZipEntry(entry.getKey()));Files.copy(entry.getValue(),out);out.closeEntry();}
            }
            if (Files.size(zip)>MAX_ZIP) throw invalid("O arquivo ZIP excede 250 MB.");
            return new Download(root,zip);
        } catch (InvalidBackupException e) { cleanup(root);throw e; }
        catch (IOException | RuntimeException e) { cleanup(root);throw new InvalidBackupException("Não foi possível gerar o backup. Verifique o armazenamento e tente novamente.",e); }
    }
    public Package read(InputStream input) {
        Path root=temp();
        try {
            var files=new LinkedHashMap<String,Path>(); long total=0;
            try(var zip=new ZipInputStream(new LimitedInput(input,MAX_ZIP),StandardCharsets.UTF_8)) {
                ZipEntry entry;
                while((entry=zip.getNextEntry())!=null) {
                    String name=entry.getName();
                    if (entry.isDirectory() || !allowed(name) || files.containsKey(name) || files.size()>=MAX_ENTRIES)
                        throw invalid("O ZIP contém arquivos não permitidos, repetidos ou em excesso.");
                    long cap=entryLimit(name);
                    Path target=Files.createTempFile(root,"entry-",".tmp");
                    try(var out=Files.newOutputStream(target)) {total+=boundedCopy(zip,out,Math.min(cap,MAX_TOTAL-total));}
                    files.put(name,target);zip.closeEntry();
                }
            }
            if (!files.keySet().containsAll(Set.of("manifesto.json","dados.json","LEIA-ME.txt"))) throw invalid("O arquivo não é um backup completo da Minha Gibiteca.");
            var manifest=object(files.get("manifesto.json"));var hashes=manifest.get("files");
            if (!(hashes instanceof ObjectNode) || !fields(hashes).equals(withoutManifest(files.keySet()))) throw invalid("Manifesto de arquivos inválido.");
            for(var entry:files.entrySet()) if (!entry.getKey().equals("manifesto.json")) {
                JsonNode info=hashes.get(entry.getKey());
                if (!info.isObject() || !fields(info).equals(Set.of("size","sha256")) || !info.path("size").isIntegralNumber()
                        || !info.path("size").canConvertToLong() || info.path("size").longValue()!=Files.size(entry.getValue())
                        || !info.path("sha256").isTextual() || !hash(entry.getValue()).equals(info.path("sha256").asText())) throw invalid("Backup corrompido: tamanho ou assinatura de integridade incorretos.");
            }
            return new Package(root,object(files.get("dados.json")),manifest,Map.copyOf(files));
        } catch (InvalidBackupException e) {cleanup(root);throw e;}
        catch (IOException | RuntimeException e) {cleanup(root);throw new InvalidBackupException("Não foi possível ler o ZIP. Envie um backup válido e completo.",e);}
    }
    public void validateImage(Path file,String mime,String name) throws IOException {
        if (!Set.of("image/png","image/jpeg").contains(mime) || !(name.endsWith(mime.equals("image/png")?".png":".jpg"))) throw invalid("Formato de capa incompatível.");
        try(var stream=ImageIO.createImageInputStream(file.toFile())) {
            var readers=ImageIO.getImageReaders(stream); if (!readers.hasNext()) throw invalid("Capa inválida.");
            var reader=readers.next();
            try {
                reader.setInput(stream,true,true);String format=reader.getFormatName();
                if (!format.equalsIgnoreCase(mime.equals("image/png")?"png":"jpeg")) throw invalid("Conteúdo da capa incompatível.");
                int width=reader.getWidth(0),height=reader.getHeight(0);
                if (width<1 || height<1 || (long)width*height>40_000_000) throw invalid("Dimensões da capa excedem o limite.");
                BufferedImage decoded=reader.read(0);if(decoded==null) throw invalid("Capa ilegível.");decoded.flush();
            } finally {reader.dispose();}
        }
    }
    public Path storage() { return storage; }
    public static boolean coverName(String name) {return name.matches(UUID_RE+"-"+UUID_RE+"\\.(png|jpg)");}
    public static Set<String> fields(JsonNode node) {var result=new HashSet<String>();node.fieldNames().forEachRemaining(result::add);return result;}
    private static Set<String> withoutManifest(Set<String> keys) {var set=new HashSet<>(keys);set.remove("manifesto.json");return set;}
    private ObjectNode object(Path file) throws IOException {
        try(var in=Files.newInputStream(file);var parser=mapper.getFactory().createParser(in)) {
            JsonNode node=mapper.readTree(parser);
            if (!(node instanceof ObjectNode object) || parser.nextToken()!=null) throw invalid("JSON inválido.");return object;
        }
    }
    private static boolean allowed(String name) {return Set.of("manifesto.json","dados.json","LEIA-ME.txt").contains(name) || (name.startsWith("capas/") && coverName(name.substring(6)));}
    private static long entryLimit(String name) {return switch(name) {case "dados.json"->MAX_JSON;case "manifesto.json"->8L*1024*1024;case "LEIA-ME.txt"->64*1024;default->MAX_COVER;};}
    static long boundedCopy(InputStream in,OutputStream out,long limit) throws IOException {
        long count=0;byte[] buffer=new byte[8192];int size;
        while((size=in.read(buffer))!=-1) {count+=size;if(count>limit) throw invalid("Backup excede o limite permitido de tamanho.");out.write(buffer,0,size);}return count;
    }
    private static String hash(Path file) throws IOException {
        try {var digest=MessageDigest.getInstance("SHA-256");try(var in=new DigestInputStream(Files.newInputStream(file),digest)) {in.transferTo(OutputStream.nullOutputStream());}return HexFormat.of().formatHex(digest.digest());}
        catch(NoSuchAlgorithmException e) {throw new IllegalStateException(e);}
    }
    private static Path write(Path root,byte[] content) throws IOException {var path=Files.createTempFile(root,"entry-",".tmp");Files.write(path,content);return path;}
    private static Path temp() {try{return Files.createTempDirectory("gibiteca-backup-");}catch(IOException e){throw new InvalidBackupException("Não foi possível preparar os arquivos temporários.",e);}}
    private static void cleanup(Path root) {
        try(var entries=Files.newDirectoryStream(root)) {for(var file:entries) Files.deleteIfExists(file);}catch(IOException e){throw new IllegalStateException("Não foi possível limpar os arquivos temporários do backup",e);}
        try {Files.deleteIfExists(root);}catch(IOException e){throw new IllegalStateException("Não foi possível limpar a pasta temporária do backup",e);}
    }
    private static InvalidBackupException invalid(String message) {return new InvalidBackupException(message);}
    private static class LimitedInput extends FilterInputStream {
        private long count;private final long limit;LimitedInput(InputStream in,long limit){super(in);this.limit=limit;}
        @Override public int read() throws IOException {int b=in.read();if(b!=-1 && ++count>limit)throw invalid("ZIP excede 250 MB.");return b;}
        @Override public int read(byte[] b,int off,int len) throws IOException {int n=in.read(b,off,len);if(n>0 && (count+=n)>limit)throw invalid("ZIP excede 250 MB.");return n;}
    }
    private static String guide() {return """
            MINHA GIBITECA — BACKUP PORTÁTIL (formato 1)

            Este ZIP contém os cadastros e capas da sua conta; não contém a aplicação,
            senhas, configurações de banco nem chaves de API. Ele não é criptografado.
            Guarde em um local privado e use somente backups de origem confiável.

            PARA MUDAR DE MÁQUINA / SISTEMA OPERACIONAL
            1. Guarde este ZIP e uma cópia da mesma versão do projeto ou JAR da aplicação.
            2. Na nova máquina, instale Java 21 e PostgreSQL, ou utilize Docker Compose.
               Siga docs/installation.md da versão preservada do projeto.
            3. Configure o novo banco e as credenciais localmente; não reutilize arquivos
               de outra instalação sem revisar os caminhos e proteger suas senhas.
            4. Inicie o sistema, entre em uma conta sem cadastros e abra Backup e migração.
            5. Selecione este ZIP, confirme que a conta está vazia e clique em Restaurar coleção.
            6. Confira edições, capas, valores, leitura, exemplares, checklist e relatórios.
               Só desative a instalação antiga depois de conferir a nova.

            A versão do formato e o esquema Flyway precisam ser compatíveis.
            IDs internos são recriados, preservando os vínculos. Nenhum dado é mesclado
            ou substituído. Contas e sessões de login não são transportadas.
            Não descompacte o ZIP na pasta do aplicativo: utilize a função Restaurar.
            Limites: ZIP 250 MB; expansão 512 MB; dados 25 MB; capa 5 MB / 40 milhões
            de pixels; 20.000 arquivos e 100.000 registros. Backup não é relatório PDF/CSV.
            """;}
}
