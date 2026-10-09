package br.com.minhagibiteca.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "series")
public class ComicSeries {

    @Id private UUID id;
    @Column(name = "owner_user_id", nullable = false, updatable = false) private UUID ownerUserId;
    @Column(name = "publisher_id") private UUID publisherId;
    @Column(nullable = false, length = 180) private String name;
    @Column(name = "normalized_name", nullable = false, length = 180) private String normalizedName;
    @Column(name = "start_year") private Integer startYear;
    @Column(name = "end_year") private Integer endYear;
    @Enumerated(EnumType.STRING) @Column(length = 30) private SeriesStatus status;
    @Column(length = 2000) private String notes;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected ComicSeries() { }

    private ComicSeries(UUID ownerUserId, UUID publisherId, String name, Integer startYear, Integer endYear,
                        SeriesStatus status, String notes) {
        this.id = UUID.randomUUID();
        this.ownerUserId = ownerUserId;
        update(publisherId, name, startYear, endYear, status, notes);
        this.createdAt = Instant.now();
    }

    public static ComicSeries create(UUID ownerUserId, UUID publisherId, String name, Integer startYear,
                                     Integer endYear, SeriesStatus status, String notes) {
        return new ComicSeries(ownerUserId, publisherId, name, startYear, endYear, status, notes);
    }

    public void update(UUID publisherId, String name, Integer startYear, Integer endYear, SeriesStatus status, String notes) {
        this.publisherId = publisherId;
        this.name = name.trim();
        this.normalizedName = name.trim().toLowerCase(Locale.ROOT);
        this.startYear = startYear;
        this.endYear = endYear;
        this.status = status;
        this.notes = notes == null || notes.isBlank() ? null : notes.trim();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public UUID getPublisherId() { return publisherId; }
    public String getName() { return name; }
    public Integer getStartYear() { return startYear; }
    public Integer getEndYear() { return endYear; }
    public SeriesStatus getStatus() { return status; }
    public String getNotes() { return notes; }
}
