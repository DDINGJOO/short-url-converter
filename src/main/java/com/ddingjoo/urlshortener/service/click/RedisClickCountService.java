package com.ddingjoo.urlshortener.service.click;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisClickCountService implements ClickCountService {
	
	private static final String PENDING_CLICK_KEY_PREFIX = "url_clicks:pending:";
	private static final String PROCESSING_CLICK_KEY_PREFIX = "url_clicks:processing:";
	private static final String PROCESSING_TOKEN_KEY_PREFIX = "url_clicks:processing:token:";
	private static final String TRACKED_SHORT_CODES_KEY = "url_clicks:tracked_short_codes";
	private static final DefaultRedisScript<String> RESERVE_BUFFERED_CLICKS_SCRIPT = new DefaultRedisScript<>(
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
	private static final DefaultRedisScript<Long> ACKNOWLEDGE_BUFFERED_CLICKS_SCRIPT = new DefaultRedisScript<>(
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
	
	@Override
	public void increment(String shortCode) {
		stringRedisTemplate.opsForValue().increment(pendingClickKey(shortCode));
		stringRedisTemplate.opsForSet().add(TRACKED_SHORT_CODES_KEY, shortCode);
	}
	
	@Override
	public ClickBufferState getBufferedState(String shortCode) {
		return new ClickBufferState(
				readLong(pendingClickKey(shortCode)),
				readLong(processingClickKey(shortCode)),
				stringRedisTemplate.opsForValue().get(processingTokenKey(shortCode))
		);
	}
	
	@Override
	public Set<String> findTrackedShortCodes() {
		Set<String> trackedShortCodes = stringRedisTemplate.opsForSet().members(TRACKED_SHORT_CODES_KEY);
		if (trackedShortCodes == null || trackedShortCodes.isEmpty()) {
			return Set.of();
		}
		
		return trackedShortCodes.stream()
				.filter(this::refreshTrackingMembership)
				.collect(java.util.stream.Collectors.toSet());
	}
	
	@Override
	public ClickSyncBatch reserveBufferedClicks(String shortCode) {
		String token = UUID.randomUUID().toString();
		String result = stringRedisTemplate.execute(
				RESERVE_BUFFERED_CLICKS_SCRIPT,
				List.of(pendingClickKey(shortCode), processingClickKey(shortCode), processingTokenKey(shortCode)),
				token
		);
		return parseBatch(result);
	}
	
	@Override
	public boolean acknowledgeBufferedClicks(String shortCode, String token) {
		Long result = stringRedisTemplate.execute(
				ACKNOWLEDGE_BUFFERED_CLICKS_SCRIPT,
				List.of(processingClickKey(shortCode), processingTokenKey(shortCode)),
				token
		);
		cleanupTracking(shortCode);
		return result == 1L;
	}
	
	private long readLong(String key) {
		String value = stringRedisTemplate.opsForValue().get(key);
		if (value == null) {
			return 0L;
		}
		return Long.parseLong(value);
	}
	
	private String pendingClickKey(String shortCode) {
		return PENDING_CLICK_KEY_PREFIX + shortCode;
	}
	
	private String processingClickKey(String shortCode) {
		return PROCESSING_CLICK_KEY_PREFIX + shortCode;
	}
	
	private String processingTokenKey(String shortCode) {
		return PROCESSING_TOKEN_KEY_PREFIX + shortCode;
	}
	
	private void cleanupTracking(String shortCode) {
		boolean hasPending = stringRedisTemplate.hasKey(pendingClickKey(shortCode));
		boolean hasProcessing = stringRedisTemplate.hasKey(processingClickKey(shortCode));
		if (!hasPending && !hasProcessing) {
			stringRedisTemplate.opsForSet().remove(TRACKED_SHORT_CODES_KEY, shortCode);
		}
	}
	
	private boolean refreshTrackingMembership(String shortCode) {
		cleanupTracking(shortCode);
		boolean hasPending = stringRedisTemplate.hasKey(pendingClickKey(shortCode));
		boolean hasProcessing = stringRedisTemplate.hasKey(processingClickKey(shortCode));
		return hasPending || hasProcessing;
	}
	
	private ClickSyncBatch parseBatch(String result) {
		if (result == null || result.isBlank()) {
			return new ClickSyncBatch("", 0L);
		}
		
		int separatorIndex = result.indexOf(':');
		if (separatorIndex < 0) {
			throw new IllegalStateException("Unexpected click sync batch payload: " + result);
		}
		
		String token = result.substring(0, separatorIndex);
		long clicks = Long.parseLong(result.substring(separatorIndex + 1));
		return new ClickSyncBatch(token, clicks);
	}
}
