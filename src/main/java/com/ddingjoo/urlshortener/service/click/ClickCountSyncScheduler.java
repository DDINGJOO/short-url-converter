package com.ddingjoo.urlshortener.service.click;

import com.ddingjoo.urlshortener.domain.Url;
import com.ddingjoo.urlshortener.repository.UrlRepository;
import com.ddingjoo.urlshortener.service.lock.SchedulerLockService;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ClickCountSyncScheduler {

    private static final Duration LOCK_TTL = Duration.ofSeconds(55);

    private final ClickCountService clickCountService;
    private final UrlRepository urlRepository;
    private final SchedulerLockService schedulerLockService;

    public ClickCountSyncScheduler(
            ClickCountService clickCountService,
            UrlRepository urlRepository,
            SchedulerLockService schedulerLockService
    ) {
        this.clickCountService = clickCountService;
        this.urlRepository = urlRepository;
        this.schedulerLockService = schedulerLockService;
    }

    @Scheduled(fixedDelayString = "${app.click-sync-interval-ms:60000}")
    @Transactional
    public void sync() {
        java.util.Optional<String> lockToken = schedulerLockService.acquire("click-count-sync", LOCK_TTL);
        if (lockToken.isEmpty()) {
            return;
        }

        try {
            syncInternal(lockToken.get());
        } finally {
            schedulerLockService.release("click-count-sync", lockToken.get());
        }
    }

    private void syncInternal(String lockToken) {
        Set<String> shortCodes = clickCountService.findTrackedShortCodes();
        if (shortCodes.isEmpty()) {
            return;
        }

        Map<String, Url> urlByShortCode = urlRepository.findAllByShortCodeIn(shortCodes).stream()
                .collect(java.util.stream.Collectors.toMap(Url::getShortCode, Function.identity()));
        for (String shortCode : shortCodes) {
            schedulerLockService.renew("click-count-sync", lockToken, LOCK_TTL);
            ClickSyncBatch batch = clickCountService.reserveBufferedClicks(shortCode);
            if (batch.clicks() <= 0) {
                continue;
            }

            Url url = urlByShortCode.get(shortCode);
            if (url != null) {
                applyClicks(shortCode, url, batch);
            }
        }
    }

    private void applyClicks(String shortCode, Url url, ClickSyncBatch batch) {
        if (Objects.equals(url.getLastClickSyncToken(), batch.token())) {
            clickCountService.acknowledgeBufferedClicks(shortCode, batch.token());
            return;
        }

        url.incrementClickCount(batch.clicks());
        url.updateLastClickSyncToken(batch.token());
        urlRepository.save(url);
        clickCountService.acknowledgeBufferedClicks(shortCode, batch.token());
    }
}
