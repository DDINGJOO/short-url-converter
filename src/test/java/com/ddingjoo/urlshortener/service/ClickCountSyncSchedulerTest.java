package com.ddingjoo.urlshortener.service;

import com.ddingjoo.urlshortener.domain.Url;
import com.ddingjoo.urlshortener.repository.UrlRepository;
import com.ddingjoo.urlshortener.service.click.ClickCountService;
import com.ddingjoo.urlshortener.service.click.ClickCountSyncScheduler;
import com.ddingjoo.urlshortener.service.click.ClickSyncBatch;
import com.ddingjoo.urlshortener.service.lock.SchedulerLockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClickCountSyncSchedulerTest {
	
	@Mock
	private ClickCountService clickCountService;
	
	@Mock
	private UrlRepository urlRepository;
	
	@Mock
	private SchedulerLockService schedulerLockService;
	
	@Test
	void acknowledgesBufferAfterSuccessfulSync() {
		ClickCountSyncScheduler scheduler = new ClickCountSyncScheduler(clickCountService, urlRepository, schedulerLockService);
		Url url = Url.create(1L, "3Ple", "https://example.com/docs", null);
		url.onCreate();
		ClickSyncBatch batch = new ClickSyncBatch("batch-1", 5L);
		
		when(schedulerLockService.acquire(org.mockito.ArgumentMatchers.eq("click-count-sync"), org.mockito.ArgumentMatchers.any()))
				.thenReturn(java.util.Optional.of("lock-1"));
		when(clickCountService.findTrackedShortCodes()).thenReturn(Set.of("3Ple"));
		when(clickCountService.reserveBufferedClicks("3Ple")).thenReturn(batch);
		when(urlRepository.findAllByShortCodeIn(Set.of("3Ple"))).thenReturn(java.util.List.of(url));
		when(urlRepository.save(any(Url.class))).thenAnswer(invocation -> invocation.getArgument(0));
		
		scheduler.sync();
		
		verify(urlRepository).save(url);
		verify(clickCountService).acknowledgeBufferedClicks("3Ple", "batch-1");
		verify(schedulerLockService).release("click-count-sync", "lock-1");
	}
	
	@Test
	void keepsBufferWhenUrlSaveCannotProceed() {
		ClickCountSyncScheduler scheduler = new ClickCountSyncScheduler(clickCountService, urlRepository, schedulerLockService);
		
		when(schedulerLockService.acquire(org.mockito.ArgumentMatchers.eq("click-count-sync"), org.mockito.ArgumentMatchers.any()))
				.thenReturn(java.util.Optional.of("lock-1"));
		when(clickCountService.findTrackedShortCodes()).thenReturn(Set.of("missing"));
		when(clickCountService.reserveBufferedClicks("missing")).thenReturn(new ClickSyncBatch("batch-1", 3L));
		when(urlRepository.findAllByShortCodeIn(Set.of("missing"))).thenReturn(java.util.List.of());
		
		scheduler.sync();
		
		verify(clickCountService, never()).acknowledgeBufferedClicks("missing", "batch-1");
		verify(urlRepository, never()).save(any());
	}
	
	@Test
	void skipsEmptyBuffers() {
		ClickCountSyncScheduler scheduler = new ClickCountSyncScheduler(clickCountService, urlRepository, schedulerLockService);
		
		when(schedulerLockService.acquire(org.mockito.ArgumentMatchers.eq("click-count-sync"), org.mockito.ArgumentMatchers.any()))
				.thenReturn(java.util.Optional.of("lock-1"));
		when(clickCountService.findTrackedShortCodes()).thenReturn(Set.of("3Ple"));
		when(urlRepository.findAllByShortCodeIn(Set.of("3Ple"))).thenReturn(java.util.List.of());
		when(clickCountService.reserveBufferedClicks("3Ple")).thenReturn(new ClickSyncBatch("", 0L));
		
		scheduler.sync();
		
		verify(urlRepository).findAllByShortCodeIn(Set.of("3Ple"));
		verify(clickCountService, never()).acknowledgeBufferedClicks(any(), any());
	}
	
	@Test
	void onlyAcknowledgesAlreadyAppliedBatch() {
		ClickCountSyncScheduler scheduler = new ClickCountSyncScheduler(clickCountService, urlRepository, schedulerLockService);
		Url url = Url.create(1L, "3Ple", "https://example.com/docs", null);
		url.onCreate();
		url.updateLastClickSyncToken("batch-1");
		
		when(schedulerLockService.acquire(org.mockito.ArgumentMatchers.eq("click-count-sync"), org.mockito.ArgumentMatchers.any()))
				.thenReturn(java.util.Optional.of("lock-1"));
		when(clickCountService.findTrackedShortCodes()).thenReturn(Set.of("3Ple"));
		when(clickCountService.reserveBufferedClicks("3Ple")).thenReturn(new ClickSyncBatch("batch-1", 5L));
		when(urlRepository.findAllByShortCodeIn(Set.of("3Ple"))).thenReturn(java.util.List.of(url));
		
		scheduler.sync();
		
		verify(urlRepository, never()).save(any());
		verify(clickCountService).acknowledgeBufferedClicks("3Ple", "batch-1");
	}
	
	@Test
	void skipsWhenLockIsNotAcquired() {
		ClickCountSyncScheduler scheduler = new ClickCountSyncScheduler(clickCountService, urlRepository, schedulerLockService);
		
		when(schedulerLockService.acquire(org.mockito.ArgumentMatchers.eq("click-count-sync"), org.mockito.ArgumentMatchers.any()))
				.thenReturn(java.util.Optional.empty());
		
		scheduler.sync();
		
		verify(clickCountService, never()).findTrackedShortCodes();
		verify(urlRepository, never()).findAllByShortCodeIn(any());
	}
}
