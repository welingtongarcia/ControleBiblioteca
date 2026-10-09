CREATE TABLE editions (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES users(id),
    publisher_id UUID REFERENCES publishers(id),
    series_id UUID REFERENCES series(id),
    title VARCHAR(240) NOT NULL,
    normalized_title VARCHAR(240) NOT NULL,
    editorial_number VARCHAR(60),
    variant VARCHAR(120),
    isbn VARCHAR(32),
    publication_year INTEGER,
    format VARCHAR(60),
    notes VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_editions_year CHECK (publication_year IS NULL OR publication_year BETWEEN 1800 AND 2100)
);
CREATE INDEX idx_editions_owner_title ON editions (owner_user_id, normalized_title);
CREATE INDEX idx_editions_owner_series ON editions (owner_user_id, series_id);
CREATE INDEX idx_editions_owner_publisher ON editions (owner_user_id, publisher_id);
