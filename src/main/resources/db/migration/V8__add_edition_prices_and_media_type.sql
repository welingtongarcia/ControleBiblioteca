ALTER TABLE editions
    ADD COLUMN media_type VARCHAR(20),
    ADD COLUMN cover_price NUMERIC(12, 2),
    ADD COLUMN reference_paid_price NUMERIC(12, 2),
    ADD CONSTRAINT ck_editions_media_type CHECK (media_type IN ('PHYSICAL', 'DIGITAL')),
    ADD CONSTRAINT ck_editions_cover_price CHECK (cover_price >= 0),
    ADD CONSTRAINT ck_editions_paid_price CHECK (reference_paid_price >= 0);
