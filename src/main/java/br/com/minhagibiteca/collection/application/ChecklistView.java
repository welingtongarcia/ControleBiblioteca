package br.com.minhagibiteca.collection.application;
import java.util.UUID;
public record ChecklistView(UUID id, UUID seriesId, String editorialNumber, String label, boolean owned, String notes) {
    public String getOwnedLabel() { return owned ? "Possuo" : "Não possuo"; }
}
