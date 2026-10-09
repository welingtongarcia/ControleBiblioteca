CREATE TABLE comic_characters (
 id UUID PRIMARY KEY, owner_user_id UUID NOT NULL REFERENCES users(id),
 name VARCHAR(180) NOT NULL, normalized_name VARCHAR(180) NOT NULL,
 CONSTRAINT uk_characters_owner_name UNIQUE(owner_user_id, normalized_name),
 CONSTRAINT uk_characters_id_owner UNIQUE(id, owner_user_id)
);
CREATE TABLE edition_characters (
 owner_user_id UUID NOT NULL REFERENCES users(id), edition_id UUID NOT NULL, character_id UUID NOT NULL,
 PRIMARY KEY(edition_id, character_id),
 FOREIGN KEY(edition_id, owner_user_id) REFERENCES editions(id, owner_user_id) ON DELETE CASCADE,
 FOREIGN KEY(character_id, owner_user_id) REFERENCES comic_characters(id, owner_user_id)
);
CREATE TABLE financial_purchases (
 id UUID PRIMARY KEY, owner_user_id UUID NOT NULL REFERENCES users(id), edition_id UUID NOT NULL,
 amount NUMERIC(12,2) NOT NULL CHECK(amount >= 0), purchase_date DATE,
 vendor VARCHAR(160), notes VARCHAR(2000),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(edition_id, owner_user_id) REFERENCES editions(id, owner_user_id)
);
CREATE INDEX idx_purchases_owner_date ON financial_purchases(owner_user_id, purchase_date, id);
CREATE INDEX idx_purchases_owner_edition ON financial_purchases(owner_user_id, edition_id);
CREATE INDEX idx_edition_characters_owner_character ON edition_characters(owner_user_id, character_id, edition_id);
CREATE INDEX idx_edition_creators_creator_edition ON edition_creators(creator_id, edition_id);
