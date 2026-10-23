CREATE TABLE onedrive_connections (
 owner_user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
 generation UUID NOT NULL,
 drive_id VARCHAR(256) NOT NULL,
 drive_name VARCHAR(300) NOT NULL,
 access_token TEXT NOT NULL,
 refresh_token TEXT NOT NULL,
 token_expires_at TIMESTAMPTZ NOT NULL,
 scopes TEXT NOT NULL,
 sync_token UUID,
 sync_started_at TIMESTAMPTZ,
 last_synced_at TIMESTAMPTZ,
 sync_error VARCHAR(400)
);
CREATE TABLE onedrive_folders (
 id UUID PRIMARY KEY,
 owner_user_id UUID NOT NULL REFERENCES onedrive_connections(owner_user_id) ON DELETE CASCADE,
 remote_id VARCHAR(256) NOT NULL,
 name VARCHAR(300) NOT NULL,
 UNIQUE(owner_user_id, remote_id)
);
CREATE TABLE onedrive_files (
 id UUID PRIMARY KEY,
 owner_user_id UUID NOT NULL REFERENCES onedrive_connections(owner_user_id) ON DELETE CASCADE,
 remote_id VARCHAR(256) NOT NULL,
 name VARCHAR(400) NOT NULL,
 extension VARCHAR(10) NOT NULL,
 folder_path VARCHAR(2000) NOT NULL,
 size_bytes BIGINT NOT NULL CHECK(size_bytes>=0),
 remote_modified_at TIMESTAMPTZ,
 web_url VARCHAR(3000),
 available BOOLEAN NOT NULL DEFAULT TRUE,
 edition_id UUID,
 UNIQUE(owner_user_id,remote_id),
 FOREIGN KEY(edition_id,owner_user_id) REFERENCES editions(id,owner_user_id) ON DELETE SET NULL (edition_id)
);
CREATE INDEX ix_onedrive_owner_name ON onedrive_files(owner_user_id,lower(name),id);
CREATE INDEX ix_onedrive_owner_edition ON onedrive_files(owner_user_id,edition_id);
