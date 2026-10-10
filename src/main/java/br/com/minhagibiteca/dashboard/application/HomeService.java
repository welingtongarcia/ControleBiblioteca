package br.com.minhagibiteca.dashboard.application;

import br.com.minhagibiteca.dashboard.infrastructure.HomeQueries;
import java.util.ArrayList;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomeService {
    private final HomeQueries queries;
    public HomeService(HomeQueries queries) { this.queries = queries; }

    @Transactional(readOnly = true)
    public HomeShowcase showcase(UUID owner) {
        var shelves = new ArrayList<HomeShowcase.Shelf>();
        HomeEdition recent = null, unread = null;
        for (var category : HomeShelf.values()) {
            var editions = queries.shelf(owner, category);
            if (editions.isEmpty()) continue;
            shelves.add(new HomeShowcase.Shelf(category, editions));
            if (category == HomeShelf.RECENT) recent = editions.getFirst();
            if (category == HomeShelf.UNREAD) unread = editions.getFirst();
        }
        return new HomeShowcase(unread == null ? recent : unread, shelves);
    }
}
