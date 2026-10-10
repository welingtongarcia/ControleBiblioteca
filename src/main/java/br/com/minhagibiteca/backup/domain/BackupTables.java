package br.com.minhagibiteca.backup.domain;

import java.util.*;

/** Identificadores SQL exclusivamente definidos pelo servidor, nunca pelo ZIP. */
public final class BackupTables {
    private BackupTables() {}
    public static final List<String> OWNED = List.of("publishers", "series", "works", "creators", "comic_characters",
            "storage_locations", "editions", "physical_copies", "series_checklist_items", "financial_purchases");
    public static final List<String> JOINS = List.of("creator_functions", "edition_works", "edition_creators", "edition_characters");
    public static List<String> all() { var result = new ArrayList<>(OWNED); result.addAll(JOINS); return List.copyOf(result); }
    public static Map<String, String> references(String table) {
        return switch (table) {
            case "series" -> Map.of("publisher_id", "publishers");
            case "storage_locations" -> Map.of("parent_id", "storage_locations");
            case "editions" -> Map.of("publisher_id", "publishers", "series_id", "series");
            case "physical_copies" -> Map.of("edition_id", "editions", "storage_location_id", "storage_locations");
            case "series_checklist_items" -> Map.of("series_id", "series");
            case "financial_purchases" -> Map.of("edition_id", "editions");
            case "creator_functions" -> Map.of("creator_id", "creators");
            case "edition_works" -> Map.of("edition_id", "editions", "work_id", "works");
            case "edition_creators" -> Map.of("edition_id", "editions", "creator_id", "creators");
            case "edition_characters" -> Map.of("edition_id", "editions", "character_id", "comic_characters");
            default -> Map.of();
        };
    }
}
