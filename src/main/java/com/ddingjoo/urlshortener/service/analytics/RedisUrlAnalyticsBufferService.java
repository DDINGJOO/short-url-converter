package com.ddingjoo.urlshortener.service.analytics;

import com.ddingjoo.urlshortener.domain.ClickMetricGranularity;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RedisUrlAnalyticsBufferService implements UrlAnalyticsBufferService {

    private static final String PREFIX = "url_click_metrics:";
    private static final String PENDING_PREFIX = PREFIX + "pending:";
    private static final String PROCESSING_PREFIX = PREFIX + "processing:";
    private static final String PROCESSING_TOKEN_PREFIX = PREFIX + "processing:token:";
    private static final String TRACKED_SHORT_CODES_KEY = PREFIX + "tracked_short_codes";
    private static final String TRACKED_GRANULARITIES_PREFIX = PREFIX + "tracked_granularities:";
    private static final String TRACKED_BUCKETS_PREFIX = PREFIX + "tracked_buckets:";
    private static final DefaultRedisScript<String> RESERVE_SCRIPT = new DefaultRedisScript<>(
            """
            local pending = tonumber(redis.call('GET', KEYS[1]) or '0')
            local processing = tonumber(redis.call('GET', KEYS[2]) or '0')
            local token = redis.call('GET', KEYS[3]) or ''

            if processing > 0 then
                return token .. ':' .. tostring(processing)
            end

            if pending > 0 then
                local newToken = ARGV[1]
                redis.call('SET', KEYS[2], pending)
                redis.call('SET', KEYS[3], newToken)
                redis.call('DEL', KEYS[1])
                return newToken .. ':' .. tostring(pending)
            end

            return ':0'
            """,
            String.class
    );
    private static final DefaultRedisScript<Long> ACK_SCRIPT = new DefaultRedisScript<>(
            """
            local token = redis.call('GET', KEYS[2]) or ''

            if token == ARGV[1] then
                redis.call('DEL', KEYS[1])
                redis.call('DEL', KEYS[2])
                return 1
            end

            return 0
            """,
            Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;

    public RedisUrlAnalyticsBufferService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void increment(String shortCode, ClickMetricGranularity granularity, OffsetDateTime bucketStart) {
        stringRedisTemplate.opsForValue().increment(pendingKey(shortCode, granularity, bucketStart));
        stringRedisTemplate.opsForSet().add(TRACKED_SHORT_CODES_KEY, shortCode);
        stringRedisTemplate.opsForSet().add(trackedGranularitiesKey(shortCode), granularity.name().toLowerCase());
        stringRedisTemplate.opsForSet().add(trackedBucketsKey(shortCode, granularity), String.valueOf(bucketStart.toEpochSecond()));
    }

    @Override
    public Set<String> findTrackedShortCodes() {
        Set<String> tracked = stringRedisTemplate.opsForSet().members(TRACKED_SHORT_CODES_KEY);
        if (tracked == null || tracked.isEmpty()) {
            return Set.of();
        }

        return tracked.stream()
                .filter(this::refreshShortCodeTracking)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public Set<ClickMetricGranularity> findTrackedGranularities(String shortCode) {
        Set<String> tracked = stringRedisTemplate.opsForSet().members(trackedGranularitiesKey(shortCode));
        if (tracked == null || tracked.isEmpty()) {
            return Set.of();
        }
        return tracked.stream()
                .map(ClickMetricGranularity::from)
                .filter(granularity -> refreshGranularityTracking(shortCode, granularity))
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public Set<OffsetDateTime> findTrackedBuckets(String shortCode, ClickMetricGranularity granularity) {
        Set<String> tracked = stringRedisTemplate.opsForSet().members(trackedBucketsKey(shortCode, granularity));
        if (tracked == null || tracked.isEmpty()) {
            return Set.of();
        }
        return tracked.stream()
                .map(epochSeconds -> OffsetDateTime.ofInstant(java.time.Instant.ofEpochSecond(Long.parseLong(epochSeconds)), java.time.ZoneOffset.UTC))
                .filter(bucketStart -> refreshBucketTracking(shortCode, granularity, bucketStart))
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public ClickMetricSyncBatch reserve(String shortCode, ClickMetricGranularity granularity, OffsetDateTime bucketStart) {
        String token = UUID.randomUUID().toString();
        String result = stringRedisTemplate.execute(
                RESERVE_SCRIPT,
                List.of(
                        pendingKey(shortCode, granularity, bucketStart),
                        processingKey(shortCode, granularity, bucketStart),
                        processingTokenKey(shortCode, granularity, bucketStart)
                ),
                token
        );

        return parseBatch(granularity, bucketStart, result);
    }

    @Override
    public boolean acknowledge(String shortCode, ClickMetricGranularity granularity, OffsetDateTime bucketStart, String token) {
        Long result = stringRedisTemplate.execute(
                ACK_SCRIPT,
                List.of(
                        processingKey(shortCode, granularity, bucketStart),
                        processingTokenKey(shortCode, granularity, bucketStart)
                ),
                token
        );
        cleanupTracking(shortCode, granularity, bucketStart);
        return result != null && result == 1L;
    }

    @Override
    public Map<OffsetDateTime, ClickMetricBufferState> getBufferedStates(
            String shortCode,
            ClickMetricGranularity granularity,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        Set<OffsetDateTime> buckets = findTrackedBuckets(shortCode, granularity);
        Map<OffsetDateTime, ClickMetricBufferState> states = new LinkedHashMap<>();
        for (OffsetDateTime bucketStart : buckets) {
            if (bucketStart.isBefore(from) || bucketStart.isAfter(to)) {
                continue;
            }

            states.put(
                    bucketStart,
                    new ClickMetricBufferState(
                            granularity,
                            bucketStart,
                            readLong(pendingKey(shortCode, granularity, bucketStart)),
                            readLong(processingKey(shortCode, granularity, bucketStart)),
                            stringRedisTemplate.opsForValue().get(processingTokenKey(shortCode, granularity, bucketStart))
                    )
            );
        }
        return states;
    }

    private long readLong(String key) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null) {
            return 0L;
        }
        return Long.parseLong(value);
    }

    private ClickMetricSyncBatch parseBatch(ClickMetricGranularity granularity, OffsetDateTime bucketStart, String result) {
        if (result == null || result.isBlank()) {
            return new ClickMetricSyncBatch(granularity, bucketStart, "", 0L);
        }

        int separatorIndex = result.indexOf(':');
        String token = result.substring(0, separatorIndex);
        long clicks = Long.parseLong(result.substring(separatorIndex + 1));
        return new ClickMetricSyncBatch(granularity, bucketStart, token, clicks);
    }

    private String pendingKey(String shortCode, ClickMetricGranularity granularity, OffsetDateTime bucketStart) {
        return PENDING_PREFIX + suffix(shortCode, granularity, bucketStart);
    }

    private String processingKey(String shortCode, ClickMetricGranularity granularity, OffsetDateTime bucketStart) {
        return PROCESSING_PREFIX + suffix(shortCode, granularity, bucketStart);
    }

    private String processingTokenKey(String shortCode, ClickMetricGranularity granularity, OffsetDateTime bucketStart) {
        return PROCESSING_TOKEN_PREFIX + suffix(shortCode, granularity, bucketStart);
    }

    private String suffix(String shortCode, ClickMetricGranularity granularity, OffsetDateTime bucketStart) {
        return shortCode + ":" + granularity.name().toLowerCase() + ":" + bucketStart.toEpochSecond();
    }

    private String trackedGranularitiesKey(String shortCode) {
        return TRACKED_GRANULARITIES_PREFIX + shortCode;
    }

    private String trackedBucketsKey(String shortCode, ClickMetricGranularity granularity) {
        return TRACKED_BUCKETS_PREFIX + shortCode + ":" + granularity.name().toLowerCase();
    }

    private void cleanupTracking(String shortCode, ClickMetricGranularity granularity, OffsetDateTime bucketStart) {
        boolean hasPending = Boolean.TRUE.equals(stringRedisTemplate.hasKey(pendingKey(shortCode, granularity, bucketStart)));
        boolean hasProcessing = Boolean.TRUE.equals(stringRedisTemplate.hasKey(processingKey(shortCode, granularity, bucketStart)));
        if (hasPending || hasProcessing) {
            return;
        }

        stringRedisTemplate.opsForSet().remove(trackedBucketsKey(shortCode, granularity), String.valueOf(bucketStart.toEpochSecond()));
        Set<String> remainingBuckets = stringRedisTemplate.opsForSet().members(trackedBucketsKey(shortCode, granularity));
        if (remainingBuckets == null || remainingBuckets.isEmpty()) {
            stringRedisTemplate.delete(trackedBucketsKey(shortCode, granularity));
            stringRedisTemplate.opsForSet().remove(trackedGranularitiesKey(shortCode), granularity.name().toLowerCase());
        }

        Set<String> remainingGranularities = stringRedisTemplate.opsForSet().members(trackedGranularitiesKey(shortCode));
        if (remainingGranularities == null || remainingGranularities.isEmpty()) {
            stringRedisTemplate.delete(trackedGranularitiesKey(shortCode));
            stringRedisTemplate.opsForSet().remove(TRACKED_SHORT_CODES_KEY, shortCode);
        }
    }

    private boolean refreshShortCodeTracking(String shortCode) {
        Set<ClickMetricGranularity> granularities = findTrackedGranularities(shortCode);
        if (granularities.isEmpty()) {
            stringRedisTemplate.opsForSet().remove(TRACKED_SHORT_CODES_KEY, shortCode);
            return false;
        }
        return true;
    }

    private boolean refreshGranularityTracking(String shortCode, ClickMetricGranularity granularity) {
        Set<OffsetDateTime> buckets = findTrackedBuckets(shortCode, granularity);
        if (buckets.isEmpty()) {
            stringRedisTemplate.delete(trackedBucketsKey(shortCode, granularity));
            stringRedisTemplate.opsForSet().remove(trackedGranularitiesKey(shortCode), granularity.name().toLowerCase());
            return false;
        }
        return true;
    }

    private boolean refreshBucketTracking(String shortCode, ClickMetricGranularity granularity, OffsetDateTime bucketStart) {
        cleanupTracking(shortCode, granularity, bucketStart);
        boolean hasPending = Boolean.TRUE.equals(stringRedisTemplate.hasKey(pendingKey(shortCode, granularity, bucketStart)));
        boolean hasProcessing = Boolean.TRUE.equals(stringRedisTemplate.hasKey(processingKey(shortCode, granularity, bucketStart)));
        return hasPending || hasProcessing;
    }
}
