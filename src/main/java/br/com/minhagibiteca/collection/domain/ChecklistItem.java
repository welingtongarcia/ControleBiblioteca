package br.com.minhagibiteca.collection.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity @Table(name = "series_checklist_items")
public class ChecklistItem {
    @Id private UUID id;
    @Column(name = "owner_user_id", nullable = false, updatable = false) private UUID ownerUserId;
    @Column(name = "series_id", nullable = false, updatable = false) private UUID seriesId;
    @Column(name = "editorial_number", nullable = false, length = 40) private String editorialNumber;
    @Column(name = "normalized_number", nullable = false, length = 40) private String normalizedNumber;
    @Column(length = 180) private String label;
    @Column(nullable = false) private boolean owned;
    @Column(length = 2000) private String notes;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected ChecklistItem() { }
    public static ChecklistItem create(UUID owner, UUID series, String number, String label, boolean owned, String notes) {
        var item = new ChecklistItem(); item.id = UUID.randomUUID(); item.ownerUserId = owner; item.seriesId = series;
        item.createdAt = Instant.now(); item.update(number, label, owned, notes); return item;
    }
    public void update(String number, String label, boolean owned, String notes) {
        this.normalizedNumber = normalize(number); this.editorialNumber = number.trim();
        this.label = optional(label, 180); this.owned = owned; this.notes = optional(notes, 2000); this.updatedAt = Instant.now();
    }
    public static String normalize(String number) {
        if (number == null || number.isBlank() || number.length() > 40) throw new IllegalArgumentException("Informe um número editorial com até 40 caracteres.");
        String normalized = number.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 40) throw new IllegalArgumentException("Informe um número editorial com até 40 caracteres.");
        return normalized;
    }
    private String optional(String value, int max) {
        if (value != null && value.length() > max) throw new IllegalArgumentException("Texto acima do limite permitido.");
        return value == null || value.isBlank() ? null : value.trim();
    }
    public UUID getId() { return id; }
    public UUID getSeriesId() { return seriesId; }
    public String getEditorialNumber() { return editorialNumber; }
    public String getNormalizedNumber() { return normalizedNumber; }
    public boolean isOwned() { return owned; }
}
