-- O formulário anterior gravava OUTRO para todos os vínculos sem uma função por edição.
DELETE FROM edition_creators old
USING edition_creators unspecified
WHERE old.edition_id = unspecified.edition_id AND old.creator_id = unspecified.creator_id
  AND old.creator_role = 'OUTRO' AND unspecified.creator_role = 'NAO_INFORMADO';
UPDATE edition_creators SET creator_role = 'NAO_INFORMADO' WHERE creator_role = 'OUTRO';
ALTER TABLE edition_creators ALTER COLUMN creator_role SET DEFAULT 'NAO_INFORMADO';
