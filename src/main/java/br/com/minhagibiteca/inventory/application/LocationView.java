package br.com.minhagibiteca.inventory.application;

import br.com.minhagibiteca.inventory.domain.LocationLevel;
import java.util.UUID;

public record LocationView(UUID id, UUID parentId, LocationLevel level, String name, String notes, String path) { }
