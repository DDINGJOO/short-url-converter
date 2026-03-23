package com.ddingjoo.urlshortener.service;

import com.ddingjoo.urlshortener.domain.ClickMetricGranularity;
import com.ddingjoo.urlshortener.domain.Url;
import com.ddingjoo.urlshortener.domain.UrlClickMetric;
import com.ddingjoo.urlshortener.repository.UrlClickMetricRepository;
import com.ddingjoo.urlshortener.repository.UrlRepository;
import com.ddingjoo.urlshortener.service.analytics.ClickMetricSyncBatch;
import com.ddingjoo.urlshortener.service.analytics.UrlAnalyticsBufferService;
import com.ddingjoo.urlshortener.service.analytics.UrlAnalyticsSyncScheduler;
import com.ddingjoo.urlshortener.service.lock.SchedulerLockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlAnalyticsSyncSchedulerTest {
	
	@Mock
	private UrlAnalyticsBufferService urlAnalyticsBufferService;
	
	@Mock
	private UrlRepository urlRepository;
	
	@Mock
	private UrlClickMetricRepository urlClickMetricRepository;
	
	@Mock
	private SchedulerLockService schedulerLockService;
	
	@Test
	void syncsMetricBucketsAndAcknowledgesToken() {
		UrlAnalyticsSyncScheduler scheduler = new UrlAnalyticsSyncScheduler(
				urlAnalyticsBufferService,
				urlRepository,
				urlClickMetricRepository,
				schedulerLockService
		);
		OffsetDateTime bucketStart = OffsetDateTime.parse("2026-03-23T01:00:00Z");
		Url url = Url.create(1L, "3Ple", "https://example.com/docs", null);
		url.onCreate();
		
		when(schedulerLockService.acquire(org.mockito.ArgumentMatchers.eq("analytics-sync"), org.mockito.ArgumentMatchers.any()))
				.thenReturn(java.util.Optional.of("lock-1"));
		when(urlAnalyticsBufferService.findTrackedShortCodes()).thenReturn(Set.of("3Ple"));
		when(urlRepository.findAllByShortCodeIn(Set.of("3Ple"))).thenReturn(java.util.List.of(url));
		when(urlAnalyticsBufferService.findTrackedGranularities("3Ple")).thenReturn(Set.of(ClickMetricGranularity.HOUR));
		when(urlAnalyticsBufferService.findTrackedBuckets("3Ple", ClickMetricGranularity.HOUR)).thenReturn(Set.of(bucketStart));
		when(urlAnalyticsBufferService.reserve("3Ple", ClickMetricGranularity.HOUR, bucketStart))
				.thenReturn(new ClickMetricSyncBatch(ClickMetricGranularity.HOUR, bucketStart, "metric-batch-1", 3L));
		when(urlClickMetricRepository.findByUrlIdAndGranularityAndBucketStart(1L, ClickMetricGranularity.HOUR, bucketStart))
				.thenReturn(Optional.empty());
		when(urlClickMetricRepository.save(any(UrlClickMetric.class))).thenAnswer(invocation -> invocation.getArgument(0));
		
		scheduler.sync();
		
		verify(urlClickMetricRepository).save(any(UrlClickMetric.class));
		verify(urlAnalyticsBufferService).acknowledge("3Ple", ClickMetricGranularity.HOUR, bucketStart, "metric-batch-1");
		verify(schedulerLockService).release("analytics-sync", "lock-1");
	}
	
	@Test
	void avoidsDoubleApplyingMetricBatch() {
		UrlAnalyticsSyncScheduler scheduler = new UrlAnalyticsSyncScheduler(
				urlAnalyticsBufferService,
				urlRepository,
				urlClickMetricRepository,
				schedulerLockService
		);
		OffsetDateTime bucketStart = OffsetDateTime.parse("2026-03-23T01:00:00Z");
		Url url = Url.create(1L, "3Ple", "https://example.com/docs", null);
		url.onCreate();
		UrlClickMetric metric = UrlClickMetric.create(1L, ClickMetricGranularity.HOUR, bucketStart);
		metric.onCreate();
		metric.updateLastSyncToken("metric-batch-1");
		
		when(schedulerLockService.acquire(org.mockito.ArgumentMatchers.eq("analytics-sync"), org.mockito.ArgumentMatchers.any()))
				.thenReturn(java.util.Optional.of("lock-1"));
		when(urlAnalyticsBufferService.findTrackedShortCodes()).thenReturn(Set.of("3Ple"));
		when(urlRepository.findAllByShortCodeIn(Set.of("3Ple"))).thenReturn(java.util.List.of(url));
		when(urlAnalyticsBufferService.findTrackedGranularities("3Ple")).thenReturn(Set.of(ClickMetricGranularity.HOUR));
		when(urlAnalyticsBufferService.findTrackedBuckets("3Ple", ClickMetricGranularity.HOUR)).thenReturn(Set.of(bucketStart));
		when(urlAnalyticsBufferService.reserve("3Ple", ClickMetricGranularity.HOUR, bucketStart))
				.thenReturn(new ClickMetricSyncBatch(ClickMetricGranularity.HOUR, bucketStart, "metric-batch-1", 3L));
		when(urlClickMetricRepository.findByUrlIdAndGranularityAndBucketStart(1L, ClickMetricGranularity.HOUR, bucketStart))
				.thenReturn(Optional.of(metric));
		
		scheduler.sync();
		
		verify(urlClickMetricRepository, never()).save(any());
		verify(urlAnalyticsBufferService).acknowledge("3Ple", ClickMetricGranularity.HOUR, bucketStart, "metric-batch-1");
	}
	
	@Test
	void skipsWhenLockIsNotAcquired() {
		UrlAnalyticsSyncScheduler scheduler = new UrlAnalyticsSyncScheduler(
				urlAnalyticsBufferService,
				urlRepository,
				urlClickMetricRepository,
				schedulerLockService
		);
		
		when(schedulerLockService.acquire(org.mockito.ArgumentMatchers.eq("analytics-sync"), org.mockito.ArgumentMatchers.any()))
				.thenReturn(java.util.Optional.empty());
		
		scheduler.sync();
		
		verify(urlAnalyticsBufferService, never()).findTrackedShortCodes();
		verify(urlRepository, never()).findAllByShortCodeIn(any());
	}
}
