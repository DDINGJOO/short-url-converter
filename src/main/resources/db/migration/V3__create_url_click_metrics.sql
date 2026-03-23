CREATE TABLE url_click_metrics (
    id BIGSERIAL PRIMARY KEY,
    url_id BIGINT NOT NULL REFERENCES urls(id),
    granularity VARCHAR(16) NOT NULL,
    bucket_start TIMESTAMPTZ NOT NULL,
    click_count BIGINT NOT NULL DEFAULT 0,
    last_sync_token VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_url_click_metrics_bucket
    ON url_click_metrics(url_id, granularity, bucket_start);

CREATE INDEX idx_url_click_metrics_lookup
    ON url_click_metrics(url_id, granularity, bucket_start);

