CREATE TABLE storage_locations (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES users(id),
    parent_id UUID,
    level VARCHAR(20) NOT NULL,
    name VARCHAR(120) NOT NULL,
    normalized_name VARCHAR(120) NOT NULL,
    notes VARCHAR(2000),
    CONSTRAINT uk_locations_id_owner UNIQUE (id, owner_user_id),
    CONSTRAINT fk_locations_parent_owner FOREIGN KEY (parent_id, owner_user_id)
        REFERENCES storage_locations (id, owner_user_id),
    CONSTRAINT ck_locations_level CHECK (level IN ('ROOM', 'SHELF_UNIT', 'SHELF', 'BOX')),
    CONSTRAINT ck_locations_not_self CHECK (parent_id IS NULL OR parent_id <> id),
    CONSTRAINT uk_locations_sibling_name UNIQUE NULLS NOT DISTINCT (owner_user_id, parent_id, normalized_name)
);
CREATE INDEX idx_locations_owner_name ON storage_locations (owner_user_id, normalized_name, id);
CREATE INDEX idx_locations_owner_parent ON storage_locations (owner_user_id, parent_id);
