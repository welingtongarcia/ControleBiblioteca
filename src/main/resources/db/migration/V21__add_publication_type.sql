ALTER TABLE editions ADD COLUMN publication_type varchar(20) NOT NULL DEFAULT 'COMIC';
ALTER TABLE editions ADD CONSTRAINT ck_editions_publication_type CHECK (publication_type IN ('COMIC', 'BOOK'));
CREATE INDEX ix_editions_owner_publication_title ON editions(owner_user_id, publication_type, normalized_title, id);
