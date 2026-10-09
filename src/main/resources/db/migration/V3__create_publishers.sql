CREATE TABLE publishers (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES users(id),
    name VARCHAR(160) NOT NULL,
    normalized_name VARCHAR(160) NOT NULL,
    country_code VARCHAR(2),
    notes VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_publishers_owner_name UNIQUE (owner_user_id, normalized_name),
    CONSTRAINT chk_publishers_country_code CHECK (country_code IS NULL OR country_code ~ '^[A-Z]{2}$')
);

CREATE INDEX idx_publishers_owner_name ON publishers (owner_user_id, normalized_name);
