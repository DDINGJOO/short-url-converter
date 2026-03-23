package com.ddingjoo.urlshortener.service.url;

import com.ddingjoo.urlshortener.config.AppProperties;
import com.ddingjoo.urlshortener.domain.Url;
import com.ddingjoo.urlshortener.dto.url.request.ShortenRequest;
import com.ddingjoo.urlshortener.dto.url.response.ShortenResponse;
import com.ddingjoo.urlshortener.dto.url.response.UrlStatsResponse;
import com.ddingjoo.urlshortener.exception.types.*;
import com.ddingjoo.urlshortener.repository.UrlRepository;
import com.ddingjoo.urlshortener.service.analytics.UrlAnalyticsService;
import com.ddingjoo.urlshortener.service.cache.UrlCacheService;
import com.ddingjoo.urlshortener.service.click.ClickBufferState;
import com.ddingjoo.urlshortener.service.click.ClickCountService;
import com.ddingjoo.urlshortener.service.lock.SchedulerLockService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.function.Supplier;

@Service
public class UrlService {
	
	private static final int MAX_GENERATION_ATTEMPTS = 5;
	
	private final UrlRepository urlRepository;
	private final ShortCodeGenerator shortCodeGenerator;
	private final AppProperties appProperties;
	private final UrlCacheService urlCacheService;
	private final ClickCountService clickCountService;
	private final UrlAnalyticsService urlAnalyticsService;
	private final SchedulerLockService schedulerLockService;
	
	public UrlService(
			UrlRepository urlRepository,
			ShortCodeGenerator shortCodeGenerator,
			AppProperties appProperties,
			UrlCacheService urlCacheService,
			ClickCountService clickCountService,
			UrlAnalyticsService urlAnalyticsService,
			SchedulerLockService schedulerLockService
	) {
		this.urlRepository = urlRepository;
		this.shortCodeGenerator = shortCodeGenerator;
		this.appProperties = appProperties;
		this.urlCacheService = urlCacheService;
		this.clickCountService = clickCountService;
		this.urlAnalyticsService = urlAnalyticsService;
		this.schedulerLockService = schedulerLockService;
	}
	
	@Transactional
	public ShortenResponse shorten(ShortenRequest request) {
		validateOriginalUrl(request.originalUrl());
		validateExpiration(request.expiresAt());
		
		String customCode = normalizeCustomCode(request.customCode());
		if (customCode != null) {
			return shortenWithCustomCode(request.originalUrl(), customCode, request.expiresAt());
		}
		
		return shortenWithGeneratedCode(request.originalUrl(), request.expiresAt());
	}
	
	private ShortenResponse shortenWithCustomCode(String originalUrl, String customCode, OffsetDateTime expiresAt) {
		try {
			Url url = Url.create(urlRepository.nextId(), customCode, originalUrl, expiresAt);
			Url savedUrl = urlRepository.save(url);
			urlCacheService.cacheActive(savedUrl);
			return toResponse(savedUrl);
		} catch (DataIntegrityViolationException exception) {
			throw new CodeConflictException();
		}
	}
	
	private ShortenResponse shortenWithGeneratedCode(String originalUrl, OffsetDateTime expiresAt) {
		for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
			Long nextId = urlRepository.nextId();
			String shortCode = shortCodeGenerator.generate(nextId);
			
			try {
				Url url = Url.create(nextId, shortCode, originalUrl, expiresAt);
				Url savedUrl = urlRepository.save(url);
				urlCacheService.cacheActive(savedUrl);
				return toResponse(savedUrl);
			} catch (DataIntegrityViolationException exception) {
				// Generated codes are deterministic, so a conflict means a custom code occupied this value.
				// Retry with the next reserved sequence value.
			}
		}
		
		throw new CodeConflictException("Failed to generate a unique short code after retry budget");
	}
	
	@Transactional(readOnly = true)
	public String resolveOriginalUrl(String shortCode) {
		return withShortCodeLock(shortCode, () -> {
			if (urlCacheService.isGone(shortCode)) {
				throw new UrlGoneException();
			}
			
			OffsetDateTime clickedAt = OffsetDateTime.now();
			return urlCacheService.findOriginalUrl(shortCode)
					.map(originalUrl -> {
						clickCountService.increment(shortCode);
						urlAnalyticsService.recordClick(shortCode, clickedAt);
						return originalUrl;
					})
					.orElseGet(() -> resolveFromDatabase(shortCode, clickedAt));
		});
	}
	
	@Transactional(readOnly = true)
	public UrlStatsResponse getStats(String shortCode) {
		Url url = urlRepository.findByShortCode(shortCode)
				.orElseThrow(UrlNotFoundException::new);
		
		ClickBufferState bufferedState = clickCountService.getBufferedState(shortCode);
		long totalClicks = java.util.stream.LongStream.of(
						url.getClickCount(),
						bufferedState.pendingClicks(),
						shouldCountProcessingClicks(url, bufferedState) ? bufferedState.processingClicks() : 0L
				)
				.sum();
		return new UrlStatsResponse(
				url.getShortCode(),
				url.getOriginalUrl(),
				totalClicks,
				url.getCreatedAt(),
				url.getExpiresAt(),
				url.isActive(OffsetDateTime.now())
		);
	}
	
	@Transactional
	public void delete(String shortCode, String authorizationHeader) {
		withShortCodeLock(shortCode, () -> {
			validateAuthorization(authorizationHeader);
			
			Url url = urlRepository.findByShortCode(shortCode)
					.orElseThrow(UrlNotFoundException::new);
			
			if (!url.isDeleted()) {
				url.markDeleted();
				urlRepository.save(url);
			}
			
			urlCacheService.markGone(shortCode);
			return null;
		});
	}
	
	private ShortenResponse toResponse(Url url) {
		return new ShortenResponse(
				url.getShortCode(),
				buildShortUrl(url.getShortCode()),
				url.getOriginalUrl(),
				url.getExpiresAt(),
				url.getCreatedAt()
		);
	}
	
	private String buildShortUrl(String shortCode) {
		String baseUrl = appProperties.baseUrl();
		if (baseUrl.endsWith("/")) {
			return baseUrl + shortCode;
		}
		return baseUrl + "/" + shortCode;
	}
	
	private String resolveFromDatabase(String shortCode, OffsetDateTime clickedAt) {
		Url url = urlRepository.findByShortCode(shortCode)
				.orElseThrow(UrlNotFoundException::new);
		
		if (!url.isActive(OffsetDateTime.now())) {
			urlCacheService.markGone(shortCode);
			throw new UrlGoneException();
		}
		
		urlCacheService.cacheActive(url);
		clickCountService.increment(shortCode);
		urlAnalyticsService.recordClick(shortCode, clickedAt);
		return url.getOriginalUrl();
	}
	
	private void validateOriginalUrl(String originalUrl) {
		try {
			URI uri = new URI(originalUrl);
			String scheme = uri.getScheme();
			if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
				throw new InvalidUrlException("original_url must use http or https");
			}
			if (uri.getHost() == null || uri.getHost().isBlank()) {
				throw new InvalidUrlException("original_url must include a valid host");
			}
		} catch (URISyntaxException exception) {
			throw new InvalidUrlException("original_url is not a valid URI");
		}
	}
	
	private String normalizeCustomCode(String customCode) {
		if (customCode == null || customCode.isBlank()) {
			return null;
		}
		
		if (!customCode.matches("^[A-Za-z0-9_-]{1,20}$")) {
			throw new InvalidShortCodeException("custom_code must match [A-Za-z0-9_-]{1,20}");
		}
		
		return customCode;
	}
	
	private void validateExpiration(OffsetDateTime expiresAt) {
		if (expiresAt != null && !expiresAt.isAfter(OffsetDateTime.now())) {
			throw new InvalidUrlException("expires_at must be in the future");
		}
	}
	
	private void validateAuthorization(String authorizationHeader) {
		if (!Objects.equals(appProperties.adminApiKey(), authorizationHeader)) {
			throw new UnauthorizedException();
		}
	}
	
	private boolean shouldCountProcessingClicks(Url url, ClickBufferState bufferedState) {
		return bufferedState.processingClicks() > 0
				&& !Objects.equals(url.getLastClickSyncToken(), bufferedState.processingToken());
	}
	
	private <T> T withShortCodeLock(String shortCode, Supplier<T> action) {
		String lockName = "url-op:" + shortCode;
		Duration ttl = Duration.ofSeconds(5);
		for (int attempt = 0; attempt < 3; attempt++) {
			java.util.Optional<String> token = schedulerLockService.acquire(lockName, ttl);
			if (token.isPresent()) {
				try {
					return action.get();
				} finally {
					schedulerLockService.release(lockName, token.get());
				}
			}
			sleepBackoff();
		}
		
		throw new ConcurrentOperationException();
	}
	
	private void sleepBackoff() {
		try {
			Thread.sleep(25L);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new ConcurrentOperationException();
		}
	}
}
