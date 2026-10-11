CREATE TABLE wishlist_items (
 id UUID PRIMARY KEY,
 owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 title VARCHAR(240) NOT NULL CHECK (length(trim(title)) > 0),
 publication_type VARCHAR(20) NOT NULL CHECK (publication_type IN ('COMIC','BOOK')),
 author VARCHAR(180), publisher VARCHAR(180), isbn VARCHAR(32),
 estimated_price NUMERIC(12,2) CHECK (estimated_price >= 0),
 priority VARCHAR(20) NOT NULL CHECK (priority IN ('HIGH','NORMAL','LOW')),
 acquired BOOLEAN NOT NULL DEFAULT FALSE,
 notes VARCHAR(2000),
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
 updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX ix_wishlist_owner_priority_title ON wishlist_items(owner_user_id, priority, title, id);
CREATE INDEX ix_wishlist_owner_acquired ON wishlist_items(owner_user_id, acquired);
