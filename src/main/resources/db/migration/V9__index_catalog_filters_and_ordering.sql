CREATE INDEX idx_editions_owner_format ON editions (owner_user_id, LOWER(format));
CREATE INDEX idx_editions_owner_media_title ON editions (owner_user_id, media_type, normalized_title, id);
CREATE INDEX idx_editions_owner_created ON editions (owner_user_id, created_at, id);
