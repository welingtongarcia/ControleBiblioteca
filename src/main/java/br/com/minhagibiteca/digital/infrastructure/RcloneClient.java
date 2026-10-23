package br.com.minhagibiteca.digital.infrastructure;

import br.com.minhagibiteca.digital.domain.OneDriveException;
import com.fasterxml.jackson.databind.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class RcloneClient {
    public static final String PROVIDER = "rclone";
    private final RcloneSettings settings;
    private final RcloneCommandRunner runner;
    private final ObjectMapper mapper = new ObjectMapper();
    public RcloneClient(RcloneSettings settings, RcloneCommandRunner runner) { this.settings = settings; this.runner = runner; }
    public boolean enabled() { return settings.isEnabled(); }

    public void verify(UUID owner) {
        if (!enabled()) throw new OneDriveException("O modo rclone está desativado nesta instalação.");
        String remotes = runner.run(owner, List.of("listremotes", "--long"));
        if (remotes.lines().noneMatch(line -> line.trim().matches("onedrive:\\s+onedrive(?:\\s+.*)?")))
            throw new OneDriveException("Configure um remote chamado onedrive, do tipo OneDrive, no arquivo exclusivo desta conta.");
        folder(owner, "root");
    }

    public MicrosoftGraphClient.Item folder(UUID owner, String id) {
        String path = path(id);
        JsonNode n = json(runner.run(owner, List.of("lsjson", "onedrive:" + path, "--stat", "--no-mimetype")));
        if (!n.isObject() || !n.path("IsDir").asBoolean()) throw invalid();
        return new MicrosoftGraphClient.Item(id, path.isEmpty() ? "Raiz do OneDrive" : name(path.substring(path.lastIndexOf('/') + 1)), true, false, 0, null, null);
    }

    public MicrosoftGraphClient.Items browse(UUID owner, String id, String next) {
        var items = children(owner, id).items();
        int offset;
        try { offset = next == null ? 0 : Integer.parseInt(next); } catch (NumberFormatException e) { throw invalid(); }
        if (offset < 0 || offset > items.size()) throw invalid();
        int end = Math.min(offset + 100, items.size());
        return new MicrosoftGraphClient.Items(items.subList(offset, end), end < items.size() ? Integer.toString(end) : null);
    }

    public MicrosoftGraphClient.Items children(UUID owner, String id) {
        String parent = path(id);
        JsonNode array = json(runner.run(owner, List.of("lsjson", "onedrive:" + parent, "--no-mimetype")));
        if (!array.isArray() || array.size() > 10_000) throw invalid();
        var items = new ArrayList<MicrosoftGraphClient.Item>();
        for (JsonNode n : array) {
            String child = name(n.path("Name").asText());
            String childPath = parent.isEmpty() ? child : parent + "/" + child;
            boolean directory = n.path("IsDir").asBoolean();
            String remoteId = directory ? "r:" + childPath : stableId(n.path("ID").asText(), childPath);
            if (directory) path(remoteId);
            long size = directory ? 0 : n.path("Size").asLong(-1);
            if (size < 0) throw invalid();
            Instant modified = null;
            try { if (!n.path("ModTime").asText().isBlank()) modified = Instant.parse(n.path("ModTime").asText()); }
            catch (java.time.DateTimeException e) { throw invalid(); }
            items.add(new MicrosoftGraphClient.Item(remoteId, child, directory, false, size, modified, null));
        }
        items.sort(Comparator.comparing(MicrosoftGraphClient.Item::folder).reversed().thenComparing(MicrosoftGraphClient.Item::name, String.CASE_INSENSITIVE_ORDER));
        return new MicrosoftGraphClient.Items(List.copyOf(items), null);
    }

    private JsonNode json(String value) {
        try { var json = mapper.readTree(value); if (json == null) throw invalid(); return json; }
        catch (com.fasterxml.jackson.core.JsonProcessingException e) { throw invalid(); }
    }
    private static String stableId(String id, String path) {
        try { return "rc:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((id.isBlank() ? "path:" + path : "id:" + id).getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static String name(String name) {
        if (name.isBlank() || name.length() > 300 || name.contains("/") || name.contains("\\") || name.contains(":")
                || name.equals(".") || name.equals("..") || name.chars().anyMatch(Character::isISOControl)) throw invalid();
        return name;
    }
    private static String path(String id) {
        if ("root".equals(id)) return "";
        if (id == null || !id.startsWith("r:") || id.length() > 256) throw invalid();
        String path = id.substring(2);
        if (path.isEmpty()) throw invalid();
        for (String segment : path.split("/", -1)) name(segment);
        return path;
    }
    private static OneDriveException invalid() {
        return new OneDriveException("O rclone retornou dados inválidos, uma pasta acima de 10.000 itens ou um caminho não suportado (máximo de 254 caracteres). Selecione pastas menores.");
    }
}
