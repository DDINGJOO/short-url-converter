package com.ddingjoo.urlshortener.service.ratelimit;

import com.ddingjoo.urlshortener.config.AppProperties;
import com.ddingjoo.urlshortener.exception.RateLimitExceededException;
import java.time.Instant;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RedisRateLimitService implements RateLimitService {

    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            """
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """,
            Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;
    private final long rateLimitPerMinute;

    public RedisRateLimitService(StringRedisTemplate stringRedisTemplate, AppProperties appProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.rateLimitPerMinute = appProperties.rateLimitPerMinute();
    }

    @Override
    public void validate(String clientKey) {
        long minuteWindow = Instant.now().getEpochSecond() / 60;
        String key = "rate_limit:" + clientKey + ":" + minuteWindow;
        Long count = stringRedisTemplate.execute(RATE_LIMIT_SCRIPT, java.util.List.of(key), "90000");
        if (count != null && count > rateLimitPerMinute) {
            throw new RateLimitExceededException();
        }
    }
}
