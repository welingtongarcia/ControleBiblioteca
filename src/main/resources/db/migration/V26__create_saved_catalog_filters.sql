CREATE TABLE saved_catalog_filters (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(80) NOT NULL CHECK (btrim(name) <> ''),
    query VARCHAR(240) NOT NULL DEFAULT '',
    publisher_id UUID,
    series_id UUID,
    format VARCHAR(60) NOT NULL DEFAULT '',
    media_type VARCHAR(20) CHECK (media_type IN ('PHYSICAL', 'DIGITAL')),
    publication_type VARCHAR(20) CHECK (publication_type IN ('COMIC', 'BOOK')),
    reading VARCHAR(20) NOT NULL CHECK (reading IN ('ALL', 'READ', 'UNREAD', 'IN_PROGRESS')),
    missing VARCHAR(20) NOT NULL CHECK (missing IN ('NONE', 'COVER', 'PAGES', 'PUBLISHER', 'SERIES', 'PAID')),
    catalog_order VARCHAR(30) NOT NULL CHECK (catalog_order IN
        ('TITLE_ASC', 'TITLE_DESC', 'NUMBER_ASC', 'NUMBER_DESC', 'TITLE_NUMBER_ASC', 'TITLE_NUMBER_DESC', 'NEWEST', 'OLDEST')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX saved_catalog_filters_owner_name ON saved_catalog_filters(owner_user_id, lower(name));
