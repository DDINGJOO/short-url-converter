package com.ddingjoo.urlshortener.service.analytics;

import com.ddingjoo.urlshortener.domain.ClickMetricGranularity;
import com.ddingjoo.urlshortener.domain.Url;
import com.ddingjoo.urlshortener.domain.UrlClickMetric;
import com.ddingjoo.urlshortener.repository.UrlClickMetricRepository;
import com.ddingjoo.urlshortener.repository.UrlRepository;
import com.ddingjoo.urlshortener.service.lock.SchedulerLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class UrlAnalyticsSyncScheduler {
	
	private static final Duration LOCK_TTL = Duration.ofSeconds(55);
	
	private final UrlAnalyticsBufferService urlAnalyticsBufferService;
	private final UrlRepository urlRepository;
	private final UrlClickMetricRepository urlClickMetricRepository;
	private final SchedulerLockService schedulerLockService;
	
	@Scheduled(fixedDelayString = "${app.analytics-sync-interval-ms:60000}")
	@Transactional
	public void sync() {
		java.util.Optional<String> lockToken = schedulerLockService.acquire("analytics-sync", LOCK_TTL);
		if (lockToken.isEmpty()) {
			return;
		}
		
		try {
			syncInternal(lockToken.get());
		} finally {
			schedulerLockService.release("analytics-sync", lockToken.get());
		}
	}
	
	private void syncInternal(String lockToken) {
		Set<String> trackedShortCodes = urlAnalyticsBufferService.findTrackedShortCodes();
		if (trackedShortCodes.isEmpty()) {
			return;
		}
		
		Map<String, Url> urlByShortCode = urlRepository.findAllByShortCodeIn(trackedShortCodes).stream()
				.collect(java.util.stream.Collectors.toMap(Url::getShortCode, Function.identity()));
		for (String shortCode : trackedShortCodes) {
			schedulerLockService.renew("analytics-sync", lockToken, LOCK_TTL);
			Url url = urlByShortCode.get(shortCode);
			if (url == null) {
				continue;
			}
			
			for (ClickMetricGranularity granularity : urlAnalyticsBufferService.findTrackedGranularities(shortCode)) {
				for (OffsetDateTime bucketStart : urlAnalyticsBufferService.findTrackedBuckets(shortCode, granularity)) {
					ClickMetricSyncBatch batch = urlAnalyticsBufferService.reserve(shortCode, granularity, bucketStart);
					if (batch.clicks() <= 0) {
						continue;
					}
					applyBatch(shortCode, url.getId(), batch);
				}
			}
		}
	}
	
	private void applyBatch(String shortCode, Long urlId, ClickMetricSyncBatch batch) {
		UrlClickMetric metric = urlClickMetricRepository.findByUrlIdAndGranularityAndBucketStart(
						urlId,
						batch.granularity(),
						batch.bucketStart()
				)
				.orElseGet(() -> UrlClickMetric.create(urlId, batch.granularity(), batch.bucketStart()));
		
		if (Objects.equals(metric.getLastSyncToken(), batch.token())) {
			urlAnalyticsBufferService.acknowledge(shortCode, batch.granularity(), batch.bucketStart(), batch.token());
			return;
		}
		
		metric.incrementClickCount(batch.clicks());
		metric.updateLastSyncToken(batch.token());
		urlClickMetricRepository.save(metric);
		urlAnalyticsBufferService.acknowledge(shortCode, batch.granularity(), batch.bucketStart(), batch.token());
	}
}
