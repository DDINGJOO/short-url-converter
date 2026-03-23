package com.ddingjoo.urlshortener.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "url_click_metrics")
public class UrlClickMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url_id", nullable = false)
    private Long urlId;

    @Enumerated(EnumType.STRING)
    @Column(name = "granularity", nullable = false, length = 16)
    private ClickMetricGranularity granularity;

    @Column(name = "bucket_start", nullable = false)
    private OffsetDateTime bucketStart;

    @Column(name = "click_count", nullable = false)
    private long clickCount;

    @Column(name = "last_sync_token", length = 64)
    private String lastSyncToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected UrlClickMetric() {
    }

    private UrlClickMetric(Long urlId, ClickMetricGranularity granularity, OffsetDateTime bucketStart) {
        this.urlId = urlId;
        this.granularity = granularity;
        this.bucketStart = bucketStart;
        this.clickCount = 0L;
    }

    public static UrlClickMetric create(Long urlId, ClickMetricGranularity granularity, OffsetDateTime bucketStart) {
        return new UrlClickMetric(urlId, granularity, bucketStart);
    }

    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getUrlId() {
        return urlId;
    }

    public ClickMetricGranularity getGranularity() {
        return granularity;
    }

    public OffsetDateTime getBucketStart() {
        return bucketStart;
    }

    public long getClickCount() {
        return clickCount;
    }

    public String getLastSyncToken() {
        return lastSyncToken;
    }

    public void incrementClickCount(long delta) {
        this.clickCount += delta;
    }

    public void updateLastSyncToken(String token) {
        this.lastSyncToken = token;
    }
}

