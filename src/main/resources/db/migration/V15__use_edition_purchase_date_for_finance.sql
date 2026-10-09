ALTER TABLE editions ADD COLUMN purchase_date DATE;
ALTER TABLE editions ADD CONSTRAINT ck_editions_purchase_date
 CHECK(purchase_date IS NULL OR purchase_date >= DATE '1900-01-01');
CREATE INDEX idx_editions_owner_purchase_date ON editions(owner_user_id, purchase_date, id);

-- O ledger desativado é preservado, mas não impede exclusões do acervo.
-- PostgreSQL 16: SET NULL somente em edition_id, mantendo o proprietário.
ALTER TABLE financial_purchases ALTER COLUMN edition_id DROP NOT NULL;
ALTER TABLE financial_purchases DROP CONSTRAINT financial_purchases_edition_id_owner_user_id_fkey;
ALTER TABLE financial_purchases ADD CONSTRAINT fk_legacy_purchases_edition_owner
 FOREIGN KEY(edition_id, owner_user_id) REFERENCES editions(id, owner_user_id)
 ON DELETE SET NULL (edition_id);
