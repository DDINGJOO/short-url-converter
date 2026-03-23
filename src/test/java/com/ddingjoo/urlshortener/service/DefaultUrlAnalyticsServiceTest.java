package com.ddingjoo.urlshortener.service;

import com.ddingjoo.urlshortener.domain.ClickMetricGranularity;
import com.ddingjoo.urlshortener.domain.Url;
import com.ddingjoo.urlshortener.domain.UrlClickMetric;
import com.ddingjoo.urlshortener.dto.url.response.UrlAnalyticsResponse;
import com.ddingjoo.urlshortener.exception.core.BusinessException;
import com.ddingjoo.urlshortener.exception.core.ErrorCode;
import com.ddingjoo.urlshortener.repository.UrlClickMetricRepository;
import com.ddingjoo.urlshortener.repository.UrlRepository;
import com.ddingjoo.urlshortener.service.analytics.ClickMetricBufferState;
import com.ddingjoo.urlshortener.service.analytics.DefaultUrlAnalyticsService;
import com.ddingjoo.urlshortener.service.analytics.UrlAnalyticsBufferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultUrlAnalyticsServiceTest {
	
	@Mock
	private UrlRepository urlRepository;
	
	@Mock
	private UrlClickMetricRepository urlClickMetricRepository;
	
	@Mock
	private UrlAnalyticsBufferService urlAnalyticsBufferService;
	
	private DefaultUrlAnalyticsService urlAnalyticsService;
	
	@BeforeEach
	void setUp() {
		urlAnalyticsService = new DefaultUrlAnalyticsService(
				urlRepository,
				urlClickMetricRepository,
				urlAnalyticsBufferService
		);
	}
	
	@Test
	void mergesPersistedAndBufferedAnalytics() {
		Url url = Url.create(1L, "3Ple", "https://example.com/docs", null);
		url.onCreate();
		UrlClickMetric metric = UrlClickMetric.create(1L, ClickMetricGranularity.HOUR, OffsetDateTime.parse("2026-03-23T01:00:00Z"));
		metric.onCreate();
		metric.incrementClickCount(5L);
		
		when(urlRepository.findByShortCode("3Ple")).thenReturn(Optional.of(url));
		when(urlClickMetricRepository.findAllByUrlIdAndGranularityAndBucketStartBetweenOrderByBucketStartAsc(
				1L,
				ClickMetricGranularity.HOUR,
				OffsetDateTime.parse("2026-03-23T00:00:00Z"),
				OffsetDateTime.parse("2026-03-23T02:00:00Z")
		)).thenReturn(List.of(metric));
		when(urlAnalyticsBufferService.getBufferedStates(
				"3Ple",
				ClickMetricGranularity.HOUR,
				OffsetDateTime.parse("2026-03-23T00:00:00Z"),
				OffsetDateTime.parse("2026-03-23T02:00:00Z")
		)).thenReturn(Map.of(
				OffsetDateTime.parse("2026-03-23T01:00:00Z"),
				new ClickMetricBufferState(ClickMetricGranularity.HOUR, OffsetDateTime.parse("2026-03-23T01:00:00Z"), 2L, 0L, null)
		));
		
		UrlAnalyticsResponse response = urlAnalyticsService.getAnalytics(
				"3Ple",
				ClickMetricGranularity.HOUR,
				OffsetDateTime.parse("2026-03-23T00:10:00Z"),
				OffsetDateTime.parse("2026-03-23T02:20:00Z")
		);
		
		assertThat(response.buckets()).hasSize(1);
		assertThat(response.buckets().get(0).clickCount()).isEqualTo(7L);
	}
	
	@Test
	void rejectsInvalidRange() {
		assertThatThrownBy(() -> urlAnalyticsService.getAnalytics(
				"3Ple",
				ClickMetricGranularity.HOUR,
				OffsetDateTime.parse("2026-03-23T03:00:00Z"),
				OffsetDateTime.parse("2026-03-23T02:00:00Z")
		)).isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_ANALYTICS_RANGE);
	}
}
