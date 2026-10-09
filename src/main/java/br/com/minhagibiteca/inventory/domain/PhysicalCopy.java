package br.com.minhagibiteca.inventory.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name = "physical_copies")
public class PhysicalCopy {
    @Id private UUID id;
    @Column(name = "owner_user_id", nullable = false, updatable = false) private UUID ownerUserId;
    @Column(name = "edition_id", nullable = false, updatable = false) private UUID editionId;
    @Column(name = "storage_location_id") private UUID storageLocationId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private CopyCondition condition;
    @Column(name = "condition_notes", length = 2000) private String conditionNotes;
    @Column(name = "acquisition_source", length = 160) private String acquisitionSource;
    @Column(name = "acquisition_date") private LocalDate acquisitionDate;
    @Column(name = "acquisition_price", precision = 12, scale = 2) private BigDecimal acquisitionPrice;
    @Column(length = 2000) private String notes;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected PhysicalCopy() { }
    public static PhysicalCopy create(UUID owner, UUID edition, UUID location, CopyCondition condition,
            String conditionNotes, String source, LocalDate date, BigDecimal price, String notes) {
        var copy = new PhysicalCopy(); copy.id = UUID.randomUUID(); copy.ownerUserId = owner; copy.editionId = edition;
        copy.createdAt = Instant.now(); copy.update(location, condition, conditionNotes, source, date, price, notes); return copy;
    }
    public void update(UUID location, CopyCondition condition, String conditionNotes, String source, LocalDate date, BigDecimal price, String notes) {
        if (condition == null) throw new IllegalArgumentException("Informe a conservação.");
        this.storageLocationId = location; this.condition = condition; this.conditionNotes = blank(conditionNotes);
        this.acquisitionSource = blank(source); this.acquisitionDate = date; this.acquisitionPrice = price;
        this.notes = blank(notes); this.updatedAt = Instant.now();
    }
    private String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    public UUID getId() { return id; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public UUID getEditionId() { return editionId; }
}
