package br.com.minhagibiteca.dashboard.application;

import br.com.minhagibiteca.dashboard.infrastructure.DashboardQueries;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final DashboardQueries queries;
    public DashboardService(DashboardQueries queries) { this.queries = queries; }
    @Transactional(readOnly = true)
    public CollectionSummary summary(UUID owner) { return queries.summary(owner); }
}
