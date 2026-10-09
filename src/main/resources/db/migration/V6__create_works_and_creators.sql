CREATE TABLE works (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(240) NOT NULL,
    normalized_title VARCHAR(240) NOT NULL,
    original_year INTEGER,
    summary VARCHAR(4000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_works_owner_title UNIQUE (owner_user_id, normalized_title)
);
CREATE TABLE creators (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES users(id),
    name VARCHAR(180) NOT NULL,
    normalized_name VARCHAR(180) NOT NULL,
    sort_name VARCHAR(180),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_creators_owner_name UNIQUE (owner_user_id, normalized_name)
);
CREATE TABLE edition_works (
    edition_id UUID NOT NULL REFERENCES editions(id) ON DELETE CASCADE,
    work_id UUID NOT NULL REFERENCES works(id),
    sequence INTEGER,
    PRIMARY KEY (edition_id, work_id)
);
CREATE TABLE edition_creators (
    edition_id UUID NOT NULL REFERENCES editions(id) ON DELETE CASCADE,
    creator_id UUID NOT NULL REFERENCES creators(id),
    creator_role VARCHAR(80) NOT NULL DEFAULT 'OUTRO',
    sequence INTEGER,
    PRIMARY KEY (edition_id, creator_id, creator_role)
);
CREATE INDEX idx_works_owner_title ON works(owner_user_id, normalized_title);
CREATE INDEX idx_creators_owner_name ON creators(owner_user_id, normalized_name);
