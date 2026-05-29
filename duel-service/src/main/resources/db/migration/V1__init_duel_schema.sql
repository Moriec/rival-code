CREATE SCHEMA IF NOT EXISTS duel;

CREATE TABLE duel.seasons (
    season_id UUID PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    active BOOLEAN NOT NULL
);

CREATE INDEX idx_seasons_active ON duel.seasons (active, starts_at DESC);

CREATE TABLE duel.queue_presets (
    preset_id UUID PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    mode VARCHAR(32) NOT NULL,
    duel_duration_seconds INT NOT NULL,
    initial_rating_window INT NOT NULL,
    max_rating_window INT NOT NULL,
    active BOOLEAN NOT NULL
);

CREATE INDEX idx_queue_presets_mode_active ON duel.queue_presets (mode, active);

CREATE TABLE duel.duel_problem_pools (
    pool_id UUID PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    season_id UUID REFERENCES duel.seasons(season_id),
    preset_id UUID REFERENCES duel.queue_presets(preset_id),
    active BOOLEAN NOT NULL
);

CREATE INDEX idx_problem_pools_season_preset_active
    ON duel.duel_problem_pools (season_id, preset_id, active);

CREATE TABLE duel.duel_problem_pool_items (
    pool_id UUID NOT NULL REFERENCES duel.duel_problem_pools(pool_id) ON DELETE CASCADE,
    problem_id UUID NOT NULL,
    PRIMARY KEY (pool_id, problem_id)
);

CREATE TABLE duel.matchmaking_tickets (
    ticket_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    preset_id UUID NOT NULL REFERENCES duel.queue_presets(preset_id),
    mode VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    current_rating INT NOT NULL,
    matched_duel_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_tickets_user_status ON duel.matchmaking_tickets (user_id, status);
CREATE INDEX idx_tickets_preset_status_created
    ON duel.matchmaking_tickets (preset_id, status, created_at);

CREATE TABLE duel.duels (
    duel_id UUID PRIMARY KEY,
    season_id UUID REFERENCES duel.seasons(season_id),
    preset_id UUID REFERENCES duel.queue_presets(preset_id),
    mode VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    problem_id UUID NOT NULL,
    problem_version_id UUID,
    problem_title VARCHAR(256),
    winner_user_id UUID,
    started_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_duels_status_ends_at ON duel.duels (status, ends_at);
CREATE INDEX idx_duels_season_status ON duel.duels (season_id, status);

CREATE TABLE duel.duel_participants (
    participant_id UUID PRIMARY KEY,
    duel_id UUID NOT NULL REFERENCES duel.duels(duel_id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    username VARCHAR(128),
    display_name VARCHAR(128),
    avatar_url TEXT,
    rating_before INT NOT NULL,
    rating_after INT,
    outcome VARCHAR(32),
    accepted_submission_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_duel_participants_duel_user UNIQUE (duel_id, user_id)
);

CREATE INDEX idx_duel_participants_user ON duel.duel_participants (user_id, created_at DESC);
CREATE INDEX idx_duel_participants_duel ON duel.duel_participants (duel_id);

CREATE TABLE duel.user_duel_profiles (
    user_id UUID PRIMARY KEY,
    username VARCHAR(128),
    display_name VARCHAR(128),
    avatar_url TEXT,
    rating INT NOT NULL,
    wins INT NOT NULL,
    losses INT NOT NULL,
    draws INT NOT NULL,
    solved_problems INT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_profiles_rating ON duel.user_duel_profiles (rating DESC, user_id);

CREATE TABLE duel.rating_history (
    rating_history_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    duel_id UUID REFERENCES duel.duels(duel_id),
    season_id UUID REFERENCES duel.seasons(season_id),
    old_rating INT NOT NULL,
    new_rating INT NOT NULL,
    delta_value INT NOT NULL,
    reason VARCHAR(64) NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_rating_history_user_changed
    ON duel.rating_history (user_id, changed_at DESC);

CREATE TABLE duel.duel_event_log (
    event_id UUID PRIMARY KEY,
    duel_id UUID NOT NULL REFERENCES duel.duels(duel_id) ON DELETE CASCADE,
    event_type VARCHAR(128) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_duel_event_log_duel_created
    ON duel.duel_event_log (duel_id, created_at);

CREATE TABLE duel.outbox_events (
    outbox_event_id UUID PRIMARY KEY,
    event_type VARCHAR(128) NOT NULL,
    aggregate_id UUID NOT NULL,
    topic VARCHAR(128) NOT NULL,
    message_key VARCHAR(128) NOT NULL,
    payload_json JSONB NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    last_error TEXT
);

CREATE INDEX idx_duel_outbox_status_created
    ON duel.outbox_events (status, created_at);

INSERT INTO duel.seasons (season_id, name, starts_at, ends_at, active)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Default Season',
    '2026-01-01T00:00:00Z',
    '2027-01-01T00:00:00Z',
    true
)
ON CONFLICT (season_id) DO NOTHING;

INSERT INTO duel.queue_presets (
    preset_id,
    name,
    mode,
    duel_duration_seconds,
    initial_rating_window,
    max_rating_window,
    active
)
VALUES
    ('00000000-0000-0000-0000-000000000101', 'Rated 15 min', 'RATED', 900, 150, 600, true),
    ('00000000-0000-0000-0000-000000000102', 'Unrated 15 min', 'UNRATED', 900, 9999, 9999, true),
    ('00000000-0000-0000-0000-000000000103', 'Practice 15 min', 'PRACTICE', 900, 9999, 9999, true)
ON CONFLICT (preset_id) DO NOTHING;
