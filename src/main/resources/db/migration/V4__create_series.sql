CREATE TABLE series (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES users(id),
    publisher_id UUID REFERENCES publishers(id),
    name VARCHAR(180) NOT NULL,
    normalized_name VARCHAR(180) NOT NULL,
    start_year INTEGER,
    end_year INTEGER,
    status VARCHAR(30),
    notes VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_series_owner_name UNIQUE (owner_user_id, normalized_name),
    CONSTRAINT chk_series_year_range CHECK (start_year IS NULL OR end_year IS NULL OR end_year >= start_year),
    CONSTRAINT chk_series_status CHECK (status IS NULL OR status IN ('ONGOING', 'FINISHED', 'PAUSED', 'UNKNOWN'))
);

CREATE INDEX idx_series_owner_name ON series (owner_user_id, normalized_name);
CREATE INDEX idx_series_owner_publisher ON series (owner_user_id, publisher_id);
