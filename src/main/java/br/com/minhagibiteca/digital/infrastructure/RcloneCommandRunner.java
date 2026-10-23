package br.com.minhagibiteca.digital.infrastructure;

import br.com.minhagibiteca.digital.domain.OneDriveException;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.stereotype.Component;

/** Executes only fixed metadata operations. Credentials never enter arguments or logs. */
@Component
public class RcloneCommandRunner {
    private static final int MAX_BYTES = 16 * 1024 * 1024;
    private final RcloneSettings settings;
    public RcloneCommandRunner(RcloneSettings settings) { this.settings = settings; }

    public String run(UUID owner, List<String> operation) {
        if (operation.isEmpty() || !Set.of("lsjson", "listremotes").contains(operation.getFirst()))
            throw new IllegalArgumentException("Operação rclone não permitida");
        var args = new ArrayList<String>();
        args.add(settings.executable());
        args.addAll(operation);
        args.addAll(List.of("--config", settings.config(owner).toString(), "--dry-run",
                "--onedrive-access-scopes", "Files.Read offline_access", "--onedrive-disable-site-permission=false",
                "--retries", "1", "--low-level-retries", "1", "--contimeout", "5s", "--timeout", "15s",
                "--log-level", "ERROR"));
        Process process = null;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var builder = processBuilder(args).redirectError(ProcessBuilder.Redirect.DISCARD);
            builder.environment().keySet().removeIf(k -> k.toUpperCase(Locale.ROOT).startsWith("RCLONE_"));
            process = builder.start();
            Process started = process;
            started.getOutputStream().close();
            Future<byte[]> output = executor.submit(() -> {
                try (var stream = started.getInputStream()) {
                    byte[] bytes = stream.readNBytes(MAX_BYTES + 1);
                    if (bytes.length > MAX_BYTES) { started.destroyForcibly(); throw new IOException(); }
                    return bytes;
                }
            });
            try {
                byte[] bytes = output.get(20, TimeUnit.SECONDS);
                if (!started.waitFor(1, TimeUnit.SECONDS) || started.exitValue() != 0) throw failure();
                return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            } finally {
                started.destroyForcibly();
                output.cancel(true);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw failure();
        } catch (IOException | ExecutionException | TimeoutException e) {
            throw failure();
        } finally {
            if (process != null) process.destroyForcibly();
        }
    }

    ProcessBuilder processBuilder(List<String> args) { return new ProcessBuilder(args); }

    private static OneDriveException failure() {
        return new OneDriveException("Não foi possível consultar o rclone. Confira a instalação, a autorização desta conta e a conexão. A consulta pode ter excedido 20 segundos ou 16 MiB; selecione pastas menores. Nenhum resultado parcial foi salvo.");
    }
}
