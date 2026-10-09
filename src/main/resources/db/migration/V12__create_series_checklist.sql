ALTER TABLE series ADD CONSTRAINT uk_series_id_owner UNIQUE (id, owner_user_id);
CREATE TABLE series_checklist_items (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES users(id),
    series_id UUID NOT NULL,
    editorial_number VARCHAR(40) NOT NULL,
    normalized_number VARCHAR(40) NOT NULL,
    label VARCHAR(180),
    owned BOOLEAN NOT NULL DEFAULT FALSE,
    notes VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_checklist_series_owner FOREIGN KEY (series_id, owner_user_id) REFERENCES series(id, owner_user_id),
    CONSTRAINT uk_checklist_number UNIQUE (owner_user_id, series_id, normalized_number),
    CONSTRAINT ck_checklist_number CHECK (length(trim(editorial_number)) > 0 AND length(trim(normalized_number)) > 0)
);
CREATE INDEX idx_checklist_owner_series_date ON series_checklist_items(owner_user_id, series_id, created_at, id);
CREATE INDEX idx_checklist_owner_series_owned_date ON series_checklist_items(owner_user_id, series_id, owned, created_at, id);
