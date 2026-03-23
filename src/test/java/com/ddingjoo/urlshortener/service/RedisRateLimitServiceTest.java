package com.ddingjoo.urlshortener.service;

import com.ddingjoo.urlshortener.config.AppProperties;
import com.ddingjoo.urlshortener.exception.types.RateLimitExceededException;
import com.ddingjoo.urlshortener.service.ratelimit.RedisRateLimitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRateLimitServiceTest {
	
	@Mock
	private StringRedisTemplate stringRedisTemplate;
	
	@Test
	void allowsRequestsWithinLimit() {
		when(stringRedisTemplate.execute(
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.anyList(),
				org.mockito.ArgumentMatchers.anyString()
		)).thenReturn(1L);
		
		RedisRateLimitService service = new RedisRateLimitService(
				stringRedisTemplate,
				new AppProperties("http://localhost:8080", "admin", 912345L, 2L)
		);
		
		assertThatCode(() -> service.validate("127.0.0.1")).doesNotThrowAnyException();
	}
	
	@Test
	void rejectsRequestsOverLimit() {
		when(stringRedisTemplate.execute(
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.anyList(),
				org.mockito.ArgumentMatchers.anyString()
		)).thenReturn(3L);
		
		RedisRateLimitService service = new RedisRateLimitService(
				stringRedisTemplate,
				new AppProperties("http://localhost:8080", "admin", 912345L, 2L)
		);
		
		assertThatThrownBy(() -> service.validate("127.0.0.1"))
				.isInstanceOf(RateLimitExceededException.class);
	}
}
