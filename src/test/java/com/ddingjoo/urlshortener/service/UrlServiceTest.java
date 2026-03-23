package com.ddingjoo.urlshortener.service;

import com.ddingjoo.urlshortener.config.AppProperties;
import com.ddingjoo.urlshortener.domain.Url;
import com.ddingjoo.urlshortener.dto.url.request.ShortenRequest;
import com.ddingjoo.urlshortener.dto.url.response.ShortenResponse;
import com.ddingjoo.urlshortener.dto.url.response.UrlStatsResponse;
import com.ddingjoo.urlshortener.exception.core.BusinessException;
import com.ddingjoo.urlshortener.exception.core.ErrorCode;
import com.ddingjoo.urlshortener.repository.UrlRepository;
import com.ddingjoo.urlshortener.service.analytics.UrlAnalyticsService;
import com.ddingjoo.urlshortener.service.cache.UrlCacheService;
import com.ddingjoo.urlshortener.service.click.ClickBufferState;
import com.ddingjoo.urlshortener.service.click.ClickCountService;
import com.ddingjoo.urlshortener.service.lock.SchedulerLockService;
import com.ddingjoo.urlshortener.service.url.Base62Encoder;
import com.ddingjoo.urlshortener.service.url.ShortCodeGenerator;
import com.ddingjoo.urlshortener.service.url.UrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {
	
	@Mock
	private UrlRepository urlRepository;
	
	@Mock
	private UrlCacheService urlCacheService;
	
	@Mock
	private ClickCountService clickCountService;
	
	@Mock
	private UrlAnalyticsService urlAnalyticsService;
	
	@Mock
	private SchedulerLockService schedulerLockService;
	
	private UrlService urlService;
	
	@BeforeEach
	void setUp() {
		AppProperties appProperties = new AppProperties("http://localhost:8080", "test-api-key", 912345L, 120L);
		ShortCodeGenerator shortCodeGenerator = new ShortCodeGenerator(new Base62Encoder(), appProperties);
		urlService = new UrlService(
				urlRepository,
				shortCodeGenerator,
				appProperties,
				urlCacheService,
				clickCountService,
				urlAnalyticsService,
				schedulerLockService
		);
	}
	
	@Test
	void generatesObfuscatedBase62Code() {
		when(urlRepository.nextId()).thenReturn(1L);
		when(urlRepository.save(any(Url.class))).thenAnswer(invocation -> {
			Url url = invocation.getArgument(0);
			url.onCreate();
			return url;
		});
		
		ShortenResponse response = urlService.shorten(new ShortenRequest("https://example.com/docs", null, null));
		
		assertThat(response.shortCode()).isEqualTo("3Ple");
		assertThat(response.shortUrl()).isEqualTo("http://localhost:8080/3Ple");
		assertThat(response.originalUrl()).isEqualTo("https://example.com/docs");
		assertThat(response.createdAt()).isNotNull();
		
		ArgumentCaptor<Url> urlCaptor = ArgumentCaptor.forClass(Url.class);
		verify(urlRepository).save(urlCaptor.capture());
		verify(urlCacheService).cacheActive(any(Url.class));
		assertThat(urlCaptor.getValue().getId()).isEqualTo(1L);
	}
	
	@Test
	void preservesCustomCodeWhenProvided() {
		when(urlRepository.nextId()).thenReturn(99L);
		when(urlRepository.save(any(Url.class))).thenAnswer(invocation -> {
			Url url = invocation.getArgument(0);
			url.onCreate();
			return url;
		});
		
		ShortenResponse response = urlService.shorten(
				new ShortenRequest("https://example.com/docs", "team-docs", null)
		);
		
		assertThat(response.shortCode()).isEqualTo("team-docs");
	}
	
	@Test
	void resolvesFromCacheAndIncrementsClicks() {
		when(schedulerLockService.acquire(org.mockito.ArgumentMatchers.eq("url-op:team-docs"), org.mockito.ArgumentMatchers.any()))
				.thenReturn(Optional.of("lock-1"));
		when(urlCacheService.isGone("team-docs")).thenReturn(false);
		when(urlCacheService.findOriginalUrl("team-docs")).thenReturn(Optional.of("https://example.com/docs"));
		
		String originalUrl = urlService.resolveOriginalUrl("team-docs");
		
		assertThat(originalUrl).isEqualTo("https://example.com/docs");
		verify(clickCountService).increment("team-docs");
		verify(urlAnalyticsService).recordClick(org.mockito.ArgumentMatchers.eq("team-docs"), org.mockito.ArgumentMatchers.any());
		verify(schedulerLockService).release("url-op:team-docs", "lock-1");
		verify(urlRepository, never()).findByShortCode(any());
	}
	
	@Test
	void resolvesFromDatabaseWhenCacheMisses() {
		Url url = savedUrl(7L, "team-docs", "https://example.com/docs", null, false);
		when(schedulerLockService.acquire(org.mockito.ArgumentMatchers.eq("url-op:team-docs"), org.mockito.ArgumentMatchers.any()))
				.thenReturn(Optional.of("lock-1"));
		when(urlCacheService.isGone("team-docs")).thenReturn(false);
		when(urlCacheService.findOriginalUrl("team-docs")).thenReturn(Optional.empty());
		when(urlRepository.findByShortCode("team-docs")).thenReturn(Optional.of(url));
		
		String originalUrl = urlService.resolveOriginalUrl("team-docs");
		
		assertThat(originalUrl).isEqualTo("https://example.com/docs");
		verify(urlCacheService).cacheActive(url);
		verify(clickCountService).increment("team-docs");
		verify(urlAnalyticsService).recordClick(org.mockito.ArgumentMatchers.eq("team-docs"), org.mockito.ArgumentMatchers.any());
		verify(schedulerLockService).release("url-op:team-docs", "lock-1");
	}
	
	@Test
	void rejectsGoneShortUrl() {
		when(schedulerLockService.acquire(org.mockito.ArgumentMatchers.eq("url-op:gone-code"), org.mockito.ArgumentMatchers.any()))
				.thenReturn(Optional.of("lock-1"));
		when(urlCacheService.isGone("gone-code")).thenReturn(true);
		
		assertThatThrownBy(() -> urlService.resolveOriginalUrl("gone-code"))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.URL_GONE);
	}
	
	@Test
	void returnsStatsWithPendingRedisClicks() {
		Url url = savedUrl(3L, "team-docs", "https://example.com/docs", null, false);
		url.incrementClickCount(5);
		
		when(urlRepository.findByShortCode("team-docs")).thenReturn(Optional.of(url));
		when(clickCountService.getBufferedState("team-docs"))
				.thenReturn(new ClickBufferState(2L, 0L, null));
		
		UrlStatsResponse response = urlService.getStats("team-docs");
		
		assertThat(response.totalClicks()).isEqualTo(7L);
		assertThat(response.isActive()).isTrue();
	}
	
	@Test
	void avoidsDoubleCountingAlreadyAppliedProcessingBatch() {
		Url url = savedUrl(3L, "team-docs", "https://example.com/docs", null, false);
		url.incrementClickCount(5);
		url.updateLastClickSyncToken("batch-1");
		
		when(urlRepository.findByShortCode("team-docs")).thenReturn(Optional.of(url));
		when(clickCountService.getBufferedState("team-docs"))
				.thenReturn(new ClickBufferState(2L, 4L, "batch-1"));
		
		UrlStatsResponse response = urlService.getStats("team-docs");
		
		assertThat(response.totalClicks()).isEqualTo(7L);
	}
	
	@Test
	void softDeletesWithValidApiKey() {
		Url url = savedUrl(11L, "team-docs", "https://example.com/docs", null, false);
		when(schedulerLockService.acquire(org.mockito.ArgumentMatchers.eq("url-op:team-docs"), org.mockito.ArgumentMatchers.any()))
				.thenReturn(Optional.of("lock-1"));
		when(urlRepository.findByShortCode("team-docs")).thenReturn(Optional.of(url));
		when(urlRepository.save(any(Url.class))).thenAnswer(invocation -> invocation.getArgument(0));
		
		urlService.delete("team-docs", "test-api-key");
		
		assertThat(url.isDeleted()).isTrue();
		verify(urlCacheService).markGone("team-docs");
		verify(schedulerLockService).release("url-op:team-docs", "lock-1");
	}
	
	@Test
	void rejectsDeleteWithInvalidApiKey() {
		when(schedulerLockService.acquire(org.mockito.ArgumentMatchers.eq("url-op:team-docs"), org.mockito.ArgumentMatchers.any()))
				.thenReturn(Optional.of("lock-1"));
		
		assertThatThrownBy(() -> urlService.delete("team-docs", "wrong-key"))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.UNAUTHORIZED_API_KEY);
	}
	
	@Test
	void rejectsConcurrentOperationWhenLockCannotBeAcquired() {
		when(schedulerLockService.acquire(org.mockito.ArgumentMatchers.eq("url-op:team-docs"), org.mockito.ArgumentMatchers.any()))
				.thenReturn(Optional.empty());
		
		assertThatThrownBy(() -> urlService.resolveOriginalUrl("team-docs"))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.CONCURRENT_OPERATION);
	}
	
	@Test
	void rejectsMissingShortUrl() {
		when(urlRepository.findByShortCode("missing")).thenReturn(Optional.empty());
		
		assertThatThrownBy(() -> urlService.getStats("missing"))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.URL_NOT_FOUND);
	}
	
	@Test
	void rejectsInvalidOriginalUrl() {
		assertThatThrownBy(() -> urlService.shorten(new ShortenRequest("ftp://example.com", null, null)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_URL_SCHEME);
	}
	
	@Test
	void rejectsPastExpiration() {
		OffsetDateTime past = OffsetDateTime.now().minusMinutes(1);
		
		assertThatThrownBy(() -> urlService.shorten(new ShortenRequest("https://example.com", null, past)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_EXPIRATION);
	}
	
	@Test
	void rejectsInvalidCustomCodeCharacters() {
		assertThatThrownBy(() -> urlService.shorten(new ShortenRequest("https://example.com", "bad code!", null)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_SHORT_CODE_PATTERN);
	}
	
	private Url savedUrl(
			Long id,
			String shortCode,
			String originalUrl,
			OffsetDateTime expiresAt,
			boolean deleted
	) {
		Url url = Url.create(id, shortCode, originalUrl, expiresAt);
		url.onCreate();
		if (deleted) {
			url.markDeleted();
		}
		return url;
	}
}
