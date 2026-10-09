package br.com.minhagibiteca.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "creators")
public class Creator {
    @Id private UUID id;
    @Column(name = "owner_user_id", nullable = false) private UUID ownerUserId;
    @Column(nullable = false) private String name;
    @Column(name = "normalized_name", nullable = false) private String normalizedName;
    @Column(name = "sort_name") private String sortName;
    @Column(name = "created_at") private Instant createdAt;

    protected Creator() { }

    public static Creator create(UUID ownerUserId, String name, String sortName) {
        Creator creator = new Creator();
        creator.id = UUID.randomUUID();
        creator.ownerUserId = ownerUserId;
        creator.createdAt = Instant.now();
        creator.update(name, sortName);
        return creator;
    }

    public void update(String name, String sortName) {
        this.name = name.trim();
        this.normalizedName = normalizeName(name);
        this.sortName = sortName == null || sortName.isBlank() ? null : sortName.trim();
    }

    public static String normalizeName(String name) { return name.trim().toLowerCase(Locale.ROOT); }
    public UUID getId() { return id; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public String getName() { return name; }
    public String getSortName() { return sortName; }
}
