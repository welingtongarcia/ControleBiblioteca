package br.com.minhagibiteca.dashboard.infrastructure;

import br.com.minhagibiteca.dashboard.application.CollectionSummary;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardQueries {
    private final JdbcTemplate jdbc;
    public DashboardQueries(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public CollectionSummary summary(UUID owner) {
        return jdbc.queryForObject("""
                select e.*, c.copies, c.owned_editions, c.priced_copies, c.total_paid
                from (select count(*) as editions,
                    count(*) filter (where media_type = 'PHYSICAL') as physical_editions,
                    count(*) filter (where media_type = 'DIGITAL') as digital_editions,
                    count(reference_paid_price) as priced_editions, sum(reference_paid_price) as registered_paid
                    from editions where owner_user_id = ?) e
                cross join (select count(*) as copies, count(distinct edition_id) as owned_editions,
                    count(acquisition_price) as priced_copies, sum(acquisition_price) as total_paid
                    from physical_copies where owner_user_id = ?) c
                """, (r, i) -> new CollectionSummary(r.getLong("editions"), r.getLong("copies"),
                r.getLong("owned_editions"), r.getLong("priced_copies"), r.getBigDecimal("total_paid"),
                r.getLong("physical_editions"), r.getLong("digital_editions"), r.getLong("priced_editions"),
                r.getBigDecimal("registered_paid")), owner, owner);
    }
}
