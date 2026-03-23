package com.ddingjoo.urlshortener.controller;

import com.ddingjoo.urlshortener.controller.docs.url.UrlApiDocs;
import com.ddingjoo.urlshortener.domain.ClickMetricGranularity;
import com.ddingjoo.urlshortener.dto.url.request.ShortenRequest;
import com.ddingjoo.urlshortener.dto.url.response.ShortenResponse;
import com.ddingjoo.urlshortener.dto.url.response.UrlAnalyticsResponse;
import com.ddingjoo.urlshortener.dto.url.response.UrlStatsResponse;
import com.ddingjoo.urlshortener.exception.types.InvalidUrlException;
import com.ddingjoo.urlshortener.service.analytics.UrlAnalyticsService;
import com.ddingjoo.urlshortener.service.url.UrlService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@Tag(name = "URL")
@RestController
@RequestMapping("/api")
public class UrlController implements UrlApiDocs {
	
	private final UrlService urlService;
	private final UrlAnalyticsService urlAnalyticsService;
	
	public UrlController(UrlService urlService, UrlAnalyticsService urlAnalyticsService) {
		this.urlService = urlService;
		this.urlAnalyticsService = urlAnalyticsService;
	}
	
	@Override
	@PostMapping("/shorten")
	@ResponseStatus(HttpStatus.CREATED)
	public ShortenResponse shorten(@Valid @RequestBody ShortenRequest request) {
		return urlService.shorten(request);
	}
	
	@Override
	@GetMapping("/urls/{shortCode}/stats")
	public UrlStatsResponse stats(@PathVariable String shortCode) {
		return urlService.getStats(shortCode);
	}
	
	@Override
	@GetMapping("/urls/{shortCode}/analytics")
	public UrlAnalyticsResponse analytics(
			@PathVariable String shortCode,
			@RequestParam String granularity,
			@RequestParam OffsetDateTime from,
			@RequestParam OffsetDateTime to
	) {
		try {
			return urlAnalyticsService.getAnalytics(shortCode, ClickMetricGranularity.from(granularity), from, to);
		} catch (IllegalArgumentException exception) {
			throw new InvalidUrlException("granularity must be one of [hour, day]");
		}
	}
	
	@Override
	@DeleteMapping("/urls/{shortCode}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable String shortCode, @RequestHeader("X-API-KEY") String apiKey) {
		urlService.delete(shortCode, apiKey);
	}
}
