ALTER TABLE editions ADD COLUMN total_pages integer;
ALTER TABLE editions ADD COLUMN pages_read integer NOT NULL DEFAULT 0;
ALTER TABLE editions ADD CONSTRAINT ck_edition_page_reading CHECK (
    pages_read >= 0 AND (
        (total_pages IS NULL AND pages_read = 0) OR
        (total_pages IS NOT NULL AND total_pages BETWEEN 1 AND 100000 AND pages_read <= total_pages
            AND is_read = (pages_read = total_pages))
    )
);
