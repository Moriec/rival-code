ALTER TABLE duel.matchmaking_tickets
    ADD COLUMN IF NOT EXISTS difficulty VARCHAR(32) NOT NULL DEFAULT 'MEDIUM';

ALTER TABLE duel.duels
    ADD COLUMN IF NOT EXISTS problem_difficulty VARCHAR(32) NOT NULL DEFAULT 'MEDIUM';

CREATE INDEX IF NOT EXISTS idx_tickets_matchmaking_difficulty
    ON duel.matchmaking_tickets (preset_id, mode, difficulty, status, created_at);
