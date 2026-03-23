package com.ddingjoo.urlshortener.service.lock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisSchedulerLockService implements SchedulerLockService {
	
	private static final String LOCK_PREFIX = "scheduler_lock:";
	private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
			"""
					local token = redis.call('GET', KEYS[1]) or ''
					if token == ARGV[1] then
					    redis.call('DEL', KEYS[1])
					    return 1
					end
					return 0
					""",
			Long.class
	);
	private static final DefaultRedisScript<Long> RENEW_SCRIPT = new DefaultRedisScript<>(
			"""
					local token = redis.call('GET', KEYS[1]) or ''
					if token == ARGV[1] then
					    redis.call('PEXPIRE', KEYS[1], ARGV[2])
					    return 1
					end
					return 0
					""",
			Long.class
	);
	
	private final StringRedisTemplate stringRedisTemplate;
	
	@Override
	public Optional<String> acquire(String lockName, Duration ttl) {
		String token = UUID.randomUUID().toString();
		Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(lockKey(lockName), token, ttl);
		return Boolean.TRUE.equals(acquired) ? Optional.of(token) : Optional.empty();
	}
	
	@Override
	public boolean renew(String lockName, String token, Duration ttl) {
		Long result = stringRedisTemplate.execute(
				RENEW_SCRIPT,
				java.util.List.of(lockKey(lockName)),
				token,
				String.valueOf(ttl.toMillis())
		);
		return result == 1L;
	}
	
	@Override
	public void release(String lockName, String token) {
		stringRedisTemplate.execute(RELEASE_SCRIPT, java.util.List.of(lockKey(lockName)), token);
	}
	
	private String lockKey(String lockName) {
		return LOCK_PREFIX + lockName;
	}
}
