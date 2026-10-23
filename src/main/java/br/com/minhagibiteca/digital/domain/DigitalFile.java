package br.com.minhagibiteca.digital.domain;

import java.time.Instant;
import java.util.UUID;

public record DigitalFile(UUID id,String remoteId,String name,String extension,String folderPath,long sizeBytes,
        Instant modifiedAt,String webUrl,boolean available,UUID editionId,String editionTitle) {
    public String getSizeLabel(){return sizeBytes<1_048_576?String.format(java.util.Locale.forLanguageTag("pt-BR"),"%.1f KB",sizeBytes/1024.0):String.format(java.util.Locale.forLanguageTag("pt-BR"),"%.1f MB",sizeBytes/1048576.0);}
}
