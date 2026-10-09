package br.com.minhagibiteca.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "publishers")
public class Publisher {

    @Id
    private UUID id;

    @Column(name = "owner_user_id", nullable = false, updatable = false)
    private UUID ownerUserId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 160)
    private String normalizedName;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(length = 2000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Publisher() {
    }

    private Publisher(UUID ownerUserId, String name, String countryCode, String notes) {
        this.id = UUID.randomUUID();
        this.ownerUserId = ownerUserId;
        applyDetails(name, countryCode, notes);
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public static Publisher create(UUID ownerUserId, String name, String countryCode, String notes) {
        return new Publisher(ownerUserId, name, countryCode, notes);
    }

    public void update(String name, String countryCode, String notes) {
        applyDetails(name, countryCode, notes);
        this.updatedAt = Instant.now();
    }

    private void applyDetails(String name, String countryCode, String notes) {
        this.name = name.trim();
        this.normalizedName = normalizeName(name);
        this.countryCode = blankToNull(countryCode == null ? null : countryCode.toUpperCase(Locale.ROOT));
        this.notes = blankToNull(notes);
    }

    public static String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() { return id; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public String getName() { return name; }
    public String getCountryCode() { return countryCode; }
    public String getNotes() { return notes; }
}
