package br.com.minhagibiteca.digital.infrastructure;

import br.com.minhagibiteca.digital.domain.OneDriveException;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RcloneSettings {
    private final boolean enabled;
    private final String executable;
    private final Path directory;

    public RcloneSettings(@Value("${app.rclone.enabled:false}") boolean enabled,
            @Value("${app.rclone.executable:rclone}") String executable,
            @Value("${app.rclone.config-directory:./data/rclone}") String directory) {
        this.enabled = enabled;
        this.executable = executable;
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public boolean isEnabled() { return enabled; }
    public String executable() { return executable; }

    public Path config(UUID owner) {
        if (!enabled) throw new OneDriveException("O modo rclone está desativado nesta instalação. Consulte docs/onedrive-rclone.md.");
        Path config = directory.resolve(owner + ".conf");
        try {
            if (!Files.isRegularFile(config, LinkOption.NOFOLLOW_LINKS) || !Files.isReadable(config)
                    || !config.toRealPath().getParent().equals(directory.toRealPath())) throw new IOException();
            return config;
        } catch (IOException e) {
            throw new OneDriveException("A autorização rclone desta conta ainda não está disponível. Siga as instruções da biblioteca digital.");
        }
    }
}
