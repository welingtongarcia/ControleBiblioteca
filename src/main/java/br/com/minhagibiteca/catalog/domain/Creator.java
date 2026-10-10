package br.com.minhagibiteca.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.stream.Collectors;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;

@Entity
@Table(name = "creators")
public class Creator {
    @Id private UUID id;
    @Column(name = "owner_user_id", nullable = false) private UUID ownerUserId;
    @Column(nullable = false) private String name;
    @Column(name = "normalized_name", nullable = false) private String normalizedName;
    @Column(name = "sort_name") private String sortName;
    @Column(name = "created_at") private Instant createdAt;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "creator_functions", joinColumns = @JoinColumn(name = "creator_id"))
    @Column(name = "function", nullable = false)
    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.Fetch(org.hibernate.annotations.FetchMode.SUBSELECT)
    private Set<CreatorFunction> functions = new HashSet<>();

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
    public void replaceFunctions(Set<CreatorFunction> functions) {
        Set<CreatorFunction> replacement = functions == null ? Set.of() : Set.copyOf(functions);
        this.functions.clear();
        this.functions.addAll(replacement);
    }
    public Set<CreatorFunction> getFunctions() { return Collections.unmodifiableSet(functions); }
    public String getFunctionsDescription() {
        return functions.isEmpty() ? "Funções não informadas" : functions.stream().sorted()
                .map(CreatorFunction::getLabel).collect(Collectors.joining(", "));
    }
    public UUID getId() { return id; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public String getName() { return name; }
    public String getSortName() { return sortName; }
}
