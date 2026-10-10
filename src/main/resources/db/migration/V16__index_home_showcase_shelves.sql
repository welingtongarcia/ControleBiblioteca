CREATE INDEX idx_editions_owner_read_created ON editions (owner_user_id, is_read, created_at DESC, id DESC);
CREATE INDEX idx_editions_owner_media_created ON editions (owner_user_id, media_type, created_at DESC, id DESC);
