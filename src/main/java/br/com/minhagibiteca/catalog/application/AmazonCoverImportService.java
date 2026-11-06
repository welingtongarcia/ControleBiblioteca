package br.com.minhagibiteca.catalog.application;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AmazonCoverImportService {
    private final AmazonImportService previews;
    private final AmazonCoverProvider provider;
    private final EditionCoverService covers;

    public AmazonCoverImportService(AmazonImportService previews, AmazonCoverProvider provider, EditionCoverService covers) {
        this.previews = previews;
        this.provider = provider;
        this.covers = covers;
    }

    public void attach(UUID edition, UUID owner, String token) {
        String url = previews.select(owner, token).metadata().coverUrl();
        if (url == null) throw new AmazonImportService.ImportException("Esta prévia não tem uma capa disponível.");
        var image = provider.download(url);
        covers.replace(edition, owner, image.bytes(), image.contentType());
    }
}
