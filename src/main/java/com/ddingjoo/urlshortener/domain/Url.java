package com.ddingjoo.urlshortener.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "urls")
public class Url {
	
	@Id
	private Long id;
	
	@Column(name = "short_code", nullable = false, unique = true, length = 20)
	private String shortCode;
	
	@Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
	private String originalUrl;
	
	@Column(name = "click_count", nullable = false)
	private long clickCount;
	
	@Column(name = "expires_at")
	private OffsetDateTime expiresAt;
	
	@Column(name = "is_deleted", nullable = false)
	private boolean deleted;
	
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;
	
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;
	
	@Column(name = "last_click_sync_token", length = 64)
	private String lastClickSyncToken;
	
	protected Url() {
	}
	
	private Url(Long id, String shortCode, String originalUrl, OffsetDateTime expiresAt) {
		this.id = id;
		this.shortCode = shortCode;
		this.originalUrl = originalUrl;
		this.expiresAt = expiresAt;
		this.clickCount = 0L;
		this.deleted = false;
	}
	
	public static Url create(Long id, String shortCode, String originalUrl, OffsetDateTime expiresAt) {
		return new Url(id, shortCode, originalUrl, expiresAt);
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
	
	public Long getId() {
		return id;
	}
	
	public String getShortCode() {
		return shortCode;
	}
	
	public String getOriginalUrl() {
		return originalUrl;
	}
	
	public long getClickCount() {
		return clickCount;
	}
	
	public OffsetDateTime getExpiresAt() {
		return expiresAt;
	}
	
	public boolean isDeleted() {
		return deleted;
	}
	
	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
	
	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
	
	public String getLastClickSyncToken() {
		return lastClickSyncToken;
	}
	
	public void markDeleted() {
		this.deleted = true;
	}
	
	public void incrementClickCount(long clickDelta) {
		this.clickCount += clickDelta;
	}
	
	public boolean isExpired(OffsetDateTime now) {
		return expiresAt != null && !expiresAt.isAfter(now);
	}
	
	public boolean isActive(OffsetDateTime now) {
		return !deleted && !isExpired(now);
	}
	
	public void updateLastClickSyncToken(String lastClickSyncToken) {
		this.lastClickSyncToken = lastClickSyncToken;
	}
}
