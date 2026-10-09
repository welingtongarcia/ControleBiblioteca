ALTER TABLE editions
    ADD COLUMN cover_file_name VARCHAR(255),
    ADD COLUMN cover_content_type VARCHAR(100),
    ADD COLUMN cover_updated_at TIMESTAMP WITH TIME ZONE;
