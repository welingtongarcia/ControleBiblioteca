ALTER TABLE editions ADD COLUMN rating integer;
ALTER TABLE editions ADD CONSTRAINT ck_editions_rating CHECK (rating BETWEEN 0 AND 5);
