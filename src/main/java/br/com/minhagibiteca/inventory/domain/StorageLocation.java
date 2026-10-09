package br.com.minhagibiteca.inventory.domain;

import jakarta.persistence.*;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "storage_locations")
public class StorageLocation {
    @Id private UUID id;
    @Column(name = "owner_user_id", nullable = false, updatable = false) private UUID ownerUserId;
    @Column(name = "parent_id") private UUID parentId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private LocationLevel level;
    @Column(nullable = false, length = 120) private String name;
    @Column(name = "normalized_name", nullable = false, length = 120) private String normalizedName;
    @Column(length = 2000) private String notes;

    protected StorageLocation() { }
    public static StorageLocation create(UUID owner, UUID parent, LocationLevel level, String name, String notes) {
        var location = new StorageLocation(); location.id = UUID.randomUUID(); location.ownerUserId = owner;
        location.update(parent, level, name, notes); return location;
    }
    public void update(UUID parent, LocationLevel level, String name, String notes) {
        if (level == null || name == null || name.isBlank() || name.trim().length() > 120) throw new IllegalArgumentException("Informe um nome e um tipo válidos.");
        this.parentId = parent; this.level = level; this.name = name.trim(); this.normalizedName = normalize(name);
        this.notes = notes == null || notes.isBlank() ? null : notes.trim();
    }
    public static String normalize(String name) { return name.trim().toLowerCase(Locale.ROOT); }
    public UUID getId() { return id; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public UUID getParentId() { return parentId; }
    public LocationLevel getLevel() { return level; }
    public String getName() { return name; }
    public String getNotes() { return notes; }
}
