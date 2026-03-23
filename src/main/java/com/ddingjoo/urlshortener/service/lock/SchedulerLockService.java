package com.ddingjoo.urlshortener.service.lock;

import java.time.Duration;
import java.util.Optional;

public interface SchedulerLockService {

    Optional<String> acquire(String lockName, Duration ttl);

    boolean renew(String lockName, String token, Duration ttl);

    void release(String lockName, String token);
}
