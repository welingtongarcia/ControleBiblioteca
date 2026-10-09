package br.com.minhagibiteca.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "works")
public class Work {
    @Id private UUID id;
    @Column(name = "owner_user_id", nullable = false) private UUID ownerUserId;
    @Column(nullable = false) private String title;
    @Column(name = "normalized_title", nullable = false) private String normalizedTitle;
    @Column(name = "original_year") private Integer originalYear;
    private String summary;
    @Column(name = "created_at") private Instant createdAt;

    protected Work() { }

    public static Work create(UUID ownerUserId, String title, Integer originalYear, String summary) {
        Work work = new Work();
        work.id = UUID.randomUUID();
        work.ownerUserId = ownerUserId;
        work.createdAt = Instant.now();
        work.update(title, originalYear, summary);
        return work;
    }

    public void update(String title, Integer originalYear, String summary) {
        this.title = title.trim();
        this.normalizedTitle = normalizeTitle(title);
        this.originalYear = originalYear;
        this.summary = blankToNull(summary);
    }

    public static String normalizeTitle(String title) { return title.trim().toLowerCase(Locale.ROOT); }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    public UUID getId() { return id; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public String getTitle() { return title; }
    public Integer getOriginalYear() { return originalYear; }
    public String getSummary() { return summary; }
}
