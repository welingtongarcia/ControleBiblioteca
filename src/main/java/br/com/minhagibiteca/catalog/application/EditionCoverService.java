package br.com.minhagibiteca.catalog.application;

import br.com.minhagibiteca.shared.application.ResourceNotFoundException;
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
    public static final long MAX_BYTES = 5L * 1024 * 1024;
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
        if (file == null || file.isEmpty()) {
            editions.findOwned(editionId, ownerId);
            throw new InvalidCoverException("Selecione uma imagem de capa.");
        }
        replace(editionId, ownerId, file.getSize(), file.getContentType(), file::transferTo);
    }

    public void replace(UUID editionId, UUID ownerId, byte[] bytes, String contentType) {
        replace(editionId, ownerId, bytes.length, contentType, path -> Files.write(path, bytes));
    }

    private void replace(UUID editionId, UUID ownerId, long size, String contentType, CoverWriter writer) {
        editions.findOwned(editionId, ownerId);
        if (size > MAX_BYTES) throw new InvalidCoverException("A imagem de capa deve ter no máximo 5 MB.");
        String extension = contentType == null ? null : ALLOWED_TYPES.get(contentType);
        if (extension == null) throw new InvalidCoverException("Envie uma imagem PNG ou JPEG válida.");

        String fileName = editionId + "-" + UUID.randomUUID() + "." + extension;
        Path target = storagePath.resolve(fileName).normalize();
        if (!target.getParent().equals(storagePath)) throw new InvalidCoverException("Nome de arquivo inválido.");
        Path temporary = null;
        try {
            temporary = Files.createTempFile(storagePath, "cover-", ".upload");
            writer.write(temporary);
            validate(temporary, extension);
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException exception) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
            String previous;
            try {
                previous = editions.updateCover(editionId, ownerId, fileName, contentType);
            } catch (RuntimeException exception) {
                deleteQuietly(target);
                throw exception;
            }
            deleteQuietly(previous);
        } catch (InvalidCoverException exception) { throw exception;
        } catch (IOException exception) { throw new InvalidCoverException("Não foi possível salvar a imagem de capa.");
        } finally { if (temporary != null) deleteQuietly(temporary); }
    }

    private static void validate(Path file, String extension) throws IOException {
        try (var stream = ImageIO.createImageInputStream(file.toFile())) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new InvalidCoverException("O arquivo enviado não é uma imagem válida.");
            var reader = readers.next();
            try {
                String expected = "jpg".equals(extension) ? "JPEG" : "PNG";
                if (!expected.equalsIgnoreCase(reader.getFormatName())) {
                    throw new InvalidCoverException("O conteúdo da imagem não corresponde ao formato informado.");
                }
                reader.setInput(stream);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > 40_000_000) {
                    throw new InvalidCoverException("A imagem de capa deve ter até 40 milhões de pixels.");
                }
                reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }

    @FunctionalInterface
    private interface CoverWriter {
        void write(Path path) throws IOException;
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
