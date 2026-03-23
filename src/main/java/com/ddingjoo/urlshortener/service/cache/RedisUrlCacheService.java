package com.ddingjoo.urlshortener.service.cache;

import com.ddingjoo.urlshortener.domain.Url;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisUrlCacheService implements UrlCacheService {

    private static final String URL_KEY_PREFIX = "url:";
    private static final String GONE_KEY_PREFIX = "url:gone:";

    private final StringRedisTemplate stringRedisTemplate;

    public RedisUrlCacheService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public Optional<String> findOriginalUrl(String shortCode) {
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(activeUrlKey(shortCode)));
    }

    @Override
    public boolean isGone(String shortCode) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(goneKey(shortCode)));
    }

    @Override
    public void cacheActive(Url url) {
        if (url.isDeleted()) {
            markGone(url.getShortCode());
            return;
        }

        OffsetDateTime expiresAt = url.getExpiresAt();
        if (expiresAt == null) {
            stringRedisTemplate.opsForValue().set(activeUrlKey(url.getShortCode()), url.getOriginalUrl());
            return;
        }

        Duration ttl = Duration.between(OffsetDateTime.now(), expiresAt);
        if (ttl.isZero() || ttl.isNegative()) {
            markGone(url.getShortCode());
            return;
        }

        stringRedisTemplate.opsForValue().set(activeUrlKey(url.getShortCode()), url.getOriginalUrl(), ttl);
    }

    @Override
    public void markGone(String shortCode) {
        stringRedisTemplate.delete(activeUrlKey(shortCode));
        stringRedisTemplate.opsForValue().set(goneKey(shortCode), "1");
    }

    private String activeUrlKey(String shortCode) {
        return URL_KEY_PREFIX + shortCode;
    }

    private String goneKey(String shortCode) {
        return GONE_KEY_PREFIX + shortCode;
    }
}
