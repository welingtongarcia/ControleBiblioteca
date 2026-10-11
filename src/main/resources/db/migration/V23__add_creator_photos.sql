ALTER TABLE creators ADD COLUMN photo_file_name VARCHAR(160);
ALTER TABLE creators ADD COLUMN photo_content_type VARCHAR(40);
ALTER TABLE creators ADD COLUMN photo_updated_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE creators ADD CONSTRAINT ck_creators_photo_metadata CHECK (
 (photo_file_name IS NULL AND photo_content_type IS NULL AND photo_updated_at IS NULL)
 OR (photo_file_name IS NOT NULL AND photo_content_type IS NOT NULL AND photo_content_type IN ('image/png','image/jpeg') AND photo_updated_at IS NOT NULL)
);
