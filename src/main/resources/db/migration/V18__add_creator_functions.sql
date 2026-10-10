CREATE TABLE creator_functions (
    creator_id UUID NOT NULL REFERENCES creators(id) ON DELETE CASCADE,
    function VARCHAR(40) NOT NULL,
    PRIMARY KEY (creator_id, function),
    CONSTRAINT ck_creator_functions_value CHECK (function IN (
        'ROTEIRISTA', 'DESENHISTA', 'ARTE_FINALISTA', 'COLORISTA',
        'LETRISTA', 'CAPISTA', 'EDITOR', 'TRADUTOR', 'OUTRO'
    ))
);
