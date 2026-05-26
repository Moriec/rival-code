CREATE SCHEMA IF NOT EXISTS problem;

CREATE TABLE problem.problems (
    problem_id UUID PRIMARY KEY,
    slug VARCHAR(128) NOT NULL,
    title VARCHAR(256) NOT NULL,
    difficulty VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    author_user_id UUID,
    accepted_count BIGINT NOT NULL DEFAULT 0,
    attempts_count BIGINT NOT NULL DEFAULT 0,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uk_problems_slug ON problem.problems (LOWER(slug));
CREATE INDEX idx_problems_status_difficulty ON problem.problems (status, difficulty);
CREATE INDEX idx_problems_published_at ON problem.problems (published_at DESC);
CREATE INDEX idx_problems_author ON problem.problems (author_user_id);

CREATE TABLE problem.problem_versions (
    problem_version_id UUID PRIMARY KEY,
    problem_id UUID NOT NULL REFERENCES problem.problems(problem_id),
    version_number INT NOT NULL,
    statement TEXT NOT NULL,
    input_spec TEXT,
    output_spec TEXT,
    statement_object_key TEXT,
    tests_manifest_object_key TEXT,
    checker_type VARCHAR(64) NOT NULL,
    custom_checker_object_key TEXT,
    time_limit_ms BIGINT NOT NULL,
    memory_limit_kb BIGINT NOT NULL,
    output_limit_bytes BIGINT NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uk_problem_versions_number ON problem.problem_versions (problem_id, version_number);
CREATE UNIQUE INDEX uk_problem_versions_one_active ON problem.problem_versions (problem_id) WHERE active = true;
CREATE INDEX idx_problem_versions_problem_active ON problem.problem_versions (problem_id, active);

CREATE TABLE problem.problem_examples (
    example_id UUID PRIMARY KEY,
    problem_version_id UUID NOT NULL REFERENCES problem.problem_versions(problem_version_id),
    order_no INT NOT NULL,
    input TEXT NOT NULL,
    expected_output TEXT NOT NULL,
    explanation TEXT,
    CONSTRAINT uk_example_order UNIQUE (problem_version_id, order_no)
);

CREATE TABLE problem.tags (
    tag_id UUID PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    color VARCHAR(32),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uk_tags_name_lower ON problem.tags (LOWER(name));

CREATE TABLE problem.problem_tags (
    problem_id UUID NOT NULL REFERENCES problem.problems(problem_id),
    tag_id UUID NOT NULL REFERENCES problem.tags(tag_id),
    PRIMARY KEY (problem_id, tag_id)
);

CREATE INDEX idx_problem_tags_tag ON problem.problem_tags (tag_id, problem_id);

CREATE TABLE problem.test_suites (
    test_suite_id UUID PRIMARY KEY,
    problem_version_id UUID NOT NULL REFERENCES problem.problem_versions(problem_version_id),
    object_key TEXT NOT NULL,
    checksum VARCHAR(128) NOT NULL,
    visible_sample_tests_count INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uk_test_suites_object_key ON problem.test_suites (object_key);
CREATE UNIQUE INDEX uk_test_suites_problem_version ON problem.test_suites (problem_version_id);