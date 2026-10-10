package br.com.minhagibiteca.dashboard.application;

import br.com.minhagibiteca.catalog.domain.MediaType;
import java.util.UUID;

/** Projeção da vitrine; não expõe entidades nem nomes de arquivos de armazenamento. */
public record HomeEdition(UUID id, String title, String number, Integer year,
                          String series, String publisher, boolean hasCover, MediaType media, boolean read) {
    public String mediaLabel() { return media == null ? "Suporte não informado" : media.getLabel(); }
    public String readingLabel() { return read ? "Já li" : "Não lido"; }
}
