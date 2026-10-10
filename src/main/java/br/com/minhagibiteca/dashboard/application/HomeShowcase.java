package br.com.minhagibiteca.dashboard.application;

import java.util.List;

public record HomeShowcase(HomeEdition featured, List<Shelf> shelves) {
    public HomeShowcase { shelves = List.copyOf(shelves); }
    public record Shelf(HomeShelf category, List<HomeEdition> editions) {
        public Shelf { editions = List.copyOf(editions); }
    }
}
