CREATE SCHEMA IF NOT EXISTS notification;

CREATE TABLE notification.notifications (
    notification_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    type VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    title VARCHAR(256) NOT NULL,
    body TEXT,
    payload JSONB,
    created_at TIMESTAMPTZ NOT NULL,
    read_at TIMESTAMPTZ,
    archived_at TIMESTAMPTZ
);

CREATE INDEX idx_notifications_user_created ON notification.notifications (user_id, created_at DESC);
CREATE INDEX idx_notifications_user_status_created ON notification.notifications (user_id, status, created_at DESC);
CREATE INDEX idx_notifications_unread ON notification.notifications (user_id, created_at DESC) WHERE status = 'NEW';

CREATE TABLE notification.notification_delivery_attempts (
    attempt_id UUID PRIMARY KEY,
    notification_id UUID NOT NULL REFERENCES notification.notifications(notification_id) ON DELETE CASCADE,
    channel VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    error_message TEXT,
    attempted_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_delivery_attempts_notification
    ON notification.notification_delivery_attempts (notification_id, attempted_at DESC);

CREATE TABLE notification.processed_notification_commands (
    command_id VARCHAR(256) PRIMARY KEY,
    notification_id UUID,
    source_topic VARCHAR(128),
    source_partition INT,
    source_offset BIGINT,
    processed_at TIMESTAMPTZ NOT NULL
);
