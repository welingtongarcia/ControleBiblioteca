ALTER TABLE editions ADD CONSTRAINT uk_editions_id_owner UNIQUE (id, owner_user_id);
CREATE TABLE physical_copies (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES users(id),
    edition_id UUID NOT NULL,
    storage_location_id UUID,
    condition VARCHAR(20) NOT NULL,
    condition_notes VARCHAR(2000),
    acquisition_source VARCHAR(160),
    acquisition_date DATE,
    acquisition_price NUMERIC(12,2),
    notes VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_copies_edition_owner FOREIGN KEY (edition_id, owner_user_id) REFERENCES editions(id, owner_user_id),
    CONSTRAINT fk_copies_location_owner FOREIGN KEY (storage_location_id, owner_user_id) REFERENCES storage_locations(id, owner_user_id),
    CONSTRAINT ck_copies_condition CHECK (condition IN ('UNKNOWN','NEW','VERY_GOOD','GOOD','FAIR','POOR')),
    CONSTRAINT ck_copies_price CHECK (acquisition_price >= 0)
);
CREATE INDEX idx_copies_owner_edition ON physical_copies(owner_user_id, edition_id, created_at, id);
CREATE INDEX idx_copies_owner_location ON physical_copies(owner_user_id, storage_location_id);
