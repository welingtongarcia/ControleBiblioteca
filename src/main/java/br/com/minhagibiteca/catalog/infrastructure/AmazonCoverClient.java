package br.com.minhagibiteca.catalog.infrastructure;

import br.com.minhagibiteca.catalog.application.AmazonCoverProvider;
import br.com.minhagibiteca.catalog.application.AmazonImportService.ImportException;
import br.com.minhagibiteca.catalog.application.EditionCoverService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Duration;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AmazonCoverClient implements AmazonCoverProvider {
    private static final long BODY_TIMEOUT_NANOS = Duration.ofSeconds(10).toNanos();
    private final AmazonPageClient.ConnectionFactory connections;

    public AmazonCoverClient() {
        this(uri -> (HttpURLConnection) uri.toURL().openConnection());
    }

    AmazonCoverClient(AmazonPageClient.ConnectionFactory connections) {
        this.connections = connections;
    }

    @Override
    public Image download(String url) {
        String safeUrl = AmazonPageClient.safeCover(url);
        if (safeUrl == null) throw unavailable();
        HttpURLConnection connection = null;
        try {
            connection = connections.open(URI.create(safeUrl));
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setRequestProperty("User-Agent", "MinhaBiblioteca/0.1 (capa solicitada pelo usuario)");
            connection.setRequestProperty("Accept", "image/jpeg, image/png");
            if (connection.getResponseCode() != 200) throw unavailable();
            String type = connection.getContentType();
            if (type == null || !Set.of("image/jpeg", "image/png").contains(type)
                    || connection.getContentLengthLong() > EditionCoverService.MAX_BYTES) throw unavailable();
            long start = System.nanoTime();
            try (var stream = connection.getInputStream()) {
                var body = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int size;
                while ((size = stream.read(buffer)) != -1) {
                    if (body.size() + size > EditionCoverService.MAX_BYTES
                            || System.nanoTime() - start > BODY_TIMEOUT_NANOS) throw unavailable();
                    body.write(buffer, 0, size);
                }
                return new Image(body.toByteArray(), type);
            }
        } catch (IOException e) {
            throw unavailable();
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static ImportException unavailable() {
        return new ImportException("A imagem da Amazon não pôde ser baixada.");
    }
}
