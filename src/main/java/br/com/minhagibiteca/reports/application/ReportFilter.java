package br.com.minhagibiteca.reports.application;

import br.com.minhagibiteca.catalog.domain.CreatorFunction;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record ReportFilter(String q, UUID seriesId, UUID publisherId, UUID creatorId,
        CreatorFunction creatorFunction, UUID characterId, String mediaType, String reading,
        LocalDate start, LocalDate end) {
    public ReportFilter {
        q = q == null ? "" : q.trim();
        mediaType = mediaType == null || mediaType.isBlank() ? "ALL" : mediaType;
        reading = reading == null || reading.isBlank() ? "ALL" : reading;
        if (q.length() > 180) throw new InvalidReport("A busca deve ter até 180 caracteres.");
        if (!Set.of("ALL", "PHYSICAL", "DIGITAL", "UNSPECIFIED").contains(mediaType))
            throw new InvalidReport("Selecione um suporte válido.");
        if (!Set.of("ALL", "READ", "UNREAD").contains(reading)) throw new InvalidReport("Selecione uma situação de leitura válida.");
        if ((start == null) != (end == null)) throw new InvalidReport("Informe as duas datas de compra ou deixe ambas vazias.");
        if (start != null && (start.isBefore(LocalDate.of(1900, 1, 1)) || start.isAfter(end) || end.isAfter(LocalDate.now())))
            throw new InvalidReport("Informe um período de compra entre 1900 e hoje, com início anterior ou igual ao fim.");
    }
    public static class InvalidReport extends RuntimeException {
        public InvalidReport(String message) { super(message); }
    }
}
