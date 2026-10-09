package br.com.minhagibiteca.catalog.application;

import br.com.minhagibiteca.shared.application.ResourceNotFoundException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EditionCoverService {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Map<String, String> ALLOWED_TYPES = Map.of("image/jpeg", "jpg", "image/png", "png");

    private final EditionService editions;
    private final Path storagePath;

    public EditionCoverService(EditionService editions, @Value("${app.cover-storage-path}") String storagePath) {
        this.editions = editions;
        this.storagePath = Path.of(storagePath).toAbsolutePath().normalize();
        try { Files.createDirectories(this.storagePath); }
        catch (IOException exception) { throw new IllegalStateException("Não foi possível preparar o armazenamento de capas.", exception); }
    }

    public void replace(UUID editionId, UUID ownerId, MultipartFile file) {
        editions.findOwned(editionId, ownerId);
        if (file == null || file.isEmpty()) throw new InvalidCoverException("Selecione uma imagem de capa.");
        if (file.getSize() > MAX_BYTES) throw new InvalidCoverException("A imagem de capa deve ter no máximo 5 MB.");
        String extension = ALLOWED_TYPES.get(file.getContentType());
        if (extension == null) throw new InvalidCoverException("Envie uma imagem PNG ou JPEG válida.");

        String fileName = editionId + "-" + UUID.randomUUID() + "." + extension;
        Path target = storagePath.resolve(fileName).normalize();
        if (!target.getParent().equals(storagePath)) throw new InvalidCoverException("Nome de arquivo inválido.");
        Path temporary = null;
        try {
            temporary = Files.createTempFile(storagePath, "cover-", ".upload");
            file.transferTo(temporary);
            BufferedImage image = ImageIO.read(temporary.toFile());
            if (image == null || image.getWidth() < 1 || image.getHeight() < 1) throw new InvalidCoverException("O arquivo enviado não é uma imagem válida.");
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException exception) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
            String previous = editions.updateCover(editionId, ownerId, fileName, file.getContentType());
            deleteQuietly(previous);
        } catch (InvalidCoverException exception) { throw exception;
        } catch (IOException exception) { throw new InvalidCoverException("Não foi possível salvar a imagem de capa.");
        } finally { if (temporary != null) deleteQuietly(temporary); }
    }

    public Cover load(UUID editionId, UUID ownerId) {
        var edition = editions.findOwned(editionId, ownerId);
        if (edition.getCoverFileName() == null || edition.getCoverContentType() == null) throw new ResourceNotFoundException();
        Path file = storagePath.resolve(edition.getCoverFileName()).normalize();
        if (!file.getParent().equals(storagePath) || !Files.isRegularFile(file)) throw new ResourceNotFoundException();
        return new Cover(new FileSystemResource(file), edition.getCoverContentType());
    }

    private void deleteQuietly(String fileName) { if (fileName != null) deleteQuietly(storagePath.resolve(fileName).normalize()); }
    private void deleteQuietly(Path file) { try { Files.deleteIfExists(file); } catch (IOException ignored) { } }
    public record Cover(Resource resource, String contentType) { }
    public static class InvalidCoverException extends RuntimeException { public InvalidCoverException(String message) { super(message); } }
}
