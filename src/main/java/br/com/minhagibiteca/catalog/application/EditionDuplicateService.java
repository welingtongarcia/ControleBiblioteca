package br.com.minhagibiteca.catalog.application;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EditionDuplicateService {
    public record Candidate(UUID id, String title, String number, String variant, String reason) {}
    private final JdbcTemplate jdbc;

    public EditionDuplicateService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(readOnly = true)
    public List<Candidate> find(UUID owner, EditionService.Command command) {
        String isbn13 = "";
        String isbn10 = "";
        if (command.isbn() != null && !command.isbn().isBlank()) {
            try {
                isbn13 = IsbnLookupService.canonical(command.isbn());
                isbn10 = isbn13;
                if (isbn13.startsWith("978")) {
                    String prefix = isbn13.substring(3, 12);
                    int sum = 0;
                    for (int i = 0; i < 9; i++) sum += (10 - i) * (prefix.charAt(i) - '0');
                    int check = (11 - sum % 11) % 11;
                    isbn10 = prefix + (check == 10 ? "X" : Integer.toString(check));
                }
            } catch (IsbnLookupService.LookupException ignored) {
                // O ISBN opcional pode conter um identificador ainda não validado.
            }
        }
        String number = command.editorialNumber() == null ? "" : command.editorialNumber().strip().toLowerCase(Locale.ROOT);
        if (isbn13.isEmpty() && (command.seriesId() == null || number.isEmpty())) return List.of();
        return jdbc.query("""
                select id, title, editorial_number, variant,
                    case when ? <> '' and upper(regexp_replace(coalesce(isbn, ''), '[[:space:]-]', '', 'g')) in (?, ?)
                        then 'Mesmo ISBN' else 'Mesma série, número e variante' end as reason
                from editions
                where owner_user_id = ? and (
                    (? <> '' and upper(regexp_replace(coalesce(isbn, ''), '[[:space:]-]', '', 'g')) in (?, ?))
                    or (series_id = ? and ? <> '' and lower(btrim(editorial_number)) = ?
                        and lower(btrim(coalesce(variant, ''))) = lower(btrim(coalesce(?, '')))
                        and publication_type = ?))
                order by normalized_title, id limit 6
                """, (row, index) -> new Candidate(row.getObject("id", UUID.class), row.getString("title"),
                        row.getString("editorial_number"), row.getString("variant"), row.getString("reason")),
                isbn13, isbn13, isbn10, owner, isbn13, isbn13, isbn10,
                command.seriesId(), number, number, command.variant(), command.publicationType().name());
    }
}
