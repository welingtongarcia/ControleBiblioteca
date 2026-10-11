ALTER TABLE creator_functions DROP CONSTRAINT ck_creator_functions_value;
ALTER TABLE creator_functions ADD CONSTRAINT ck_creator_functions_value CHECK (function IN (
 'ROTEIRISTA', 'DESENHISTA', 'ARTE_FINALISTA', 'COLORISTA',
 'LETRISTA', 'CAPISTA', 'EDITOR', 'TRADUTOR', 'ESCRITOR', 'OUTRO'
));
