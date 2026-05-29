CREATE SCHEMA IF NOT EXISTS auth;

CREATE TABLE auth.users (
    user_id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    username VARCHAR(64) NOT NULL,
    display_name VARCHAR(128),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uk_users_email_lower ON auth.users (LOWER(email));
CREATE UNIQUE INDEX uk_users_username_lower ON auth.users (LOWER(username));
CREATE INDEX idx_users_created_at ON auth.users (created_at);

CREATE TABLE auth.credentials (
    user_id UUID PRIMARY KEY REFERENCES auth.users(user_id),
    password_hash TEXT NOT NULL,
    password_algo VARCHAR(64) NOT NULL,
    password_updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE auth.user_roles (
    user_id UUID NOT NULL,
    role VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES auth.users(user_id)
);

CREATE INDEX idx_user_roles_role ON auth.user_roles (role);

CREATE TABLE auth.refresh_tokens (
    refresh_token_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    replaced_by_token_id UUID,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES auth.users(user_id)
);

CREATE UNIQUE INDEX uk_refresh_tokens_token_hash ON auth.refresh_tokens (token_hash);
CREATE INDEX idx_refresh_tokens_user_active ON auth.refresh_tokens (user_id, expires_at) WHERE revoked_at IS NULL;

CREATE TABLE auth.avatar_metadata (
    avatar_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    object_key TEXT NOT NULL,
    url TEXT,
    content_type VARCHAR(128) NOT NULL,
    size_bytes BIGINT NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_avatar_user FOREIGN KEY (user_id) REFERENCES auth.users(user_id)
);

CREATE UNIQUE INDEX uk_avatar_object_key ON auth.avatar_metadata (object_key);
CREATE INDEX idx_avatar_user_uploaded ON auth.avatar_metadata (user_id, uploaded_at DESC);
CREATE INDEX idx_avatar_user_active ON auth.avatar_metadata (user_id) WHERE active = true;