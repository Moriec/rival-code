CREATE SCHEMA IF NOT EXISTS submission;

CREATE TABLE submission.submissions (
    submission_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    problem_id UUID NOT NULL,
    problem_version_id UUID NOT NULL,
    duel_id UUID,
    mode VARCHAR(32) NOT NULL,
    language VARCHAR(32) NOT NULL,
    source_code TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    overall_status VARCHAR(64),
    accepted BOOLEAN,
    max_time_ms BIGINT,
    max_memory_kb BIGINT,
    created_at TIMESTAMPTZ NOT NULL,
    queued_at TIMESTAMPTZ,
    judged_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_submissions_user_created ON submission.submissions (user_id, created_at DESC);
CREATE INDEX idx_submissions_problem_created ON submission.submissions (problem_id, created_at DESC);
CREATE INDEX idx_submissions_duel ON submission.submissions (duel_id);
CREATE INDEX idx_submissions_status ON submission.submissions (status, created_at);
CREATE INDEX idx_submissions_problem_accepted ON submission.submissions (problem_id, user_id) WHERE accepted = true;

CREATE TABLE submission.execution_snapshots (
    submission_id UUID PRIMARY KEY REFERENCES submission.submissions(submission_id) ON DELETE CASCADE,
    problem_version_id UUID NOT NULL,
    time_limit_ms BIGINT NOT NULL,
    memory_limit_kb BIGINT NOT NULL,
    output_limit_bytes BIGINT,
    checker_type VARCHAR(64) NOT NULL,
    custom_checker_code TEXT,
    test_archive_object_key TEXT,
    visible_sample_tests_count INT NOT NULL,
    store_full_judge_log BOOLEAN NOT NULL,
    rated_mode BOOLEAN NOT NULL,
    snapshot_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE submission.judge_results (
    submission_id UUID PRIMARY KEY REFERENCES submission.submissions(submission_id) ON DELETE CASCADE,
    overall_status VARCHAR(64) NOT NULL,
    max_time_ms BIGINT,
    max_memory_kb BIGINT,
    compilation_error TEXT,
    raw_result_json JSONB NOT NULL,
    judge_log_object_key TEXT,
    received_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE submission.test_case_results (
    test_case_result_id UUID PRIMARY KEY,
    submission_id UUID NOT NULL REFERENCES submission.submissions(submission_id) ON DELETE CASCADE,
    order_no INT NOT NULL,
    status VARCHAR(64) NOT NULL,
    time_ms BIGINT,
    memory_kb BIGINT,
    input TEXT,
    actual_output TEXT,
    expected_output TEXT,
    message TEXT,
    visible BOOLEAN NOT NULL,
    CONSTRAINT uk_test_case_result_order UNIQUE (submission_id, order_no)
);

CREATE INDEX idx_test_case_results_submission ON submission.test_case_results (submission_id, order_no);

CREATE TABLE submission.submission_public_views (
    submission_id UUID PRIMARY KEY REFERENCES submission.submissions(submission_id) ON DELETE CASCADE,
    overall_status VARCHAR(64) NOT NULL,
    passed_tests INT NOT NULL,
    total_tests INT NOT NULL,
    max_time_ms BIGINT,
    max_memory_kb BIGINT,
    compilation_error TEXT,
    visible_tests_json JSONB,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE submission.processed_judge_results (
    submission_id UUID PRIMARY KEY,
    result_event_hash VARCHAR(128) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE submission.outbox_events (
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

CREATE INDEX idx_outbox_status_created ON submission.outbox_events (status, created_at);
