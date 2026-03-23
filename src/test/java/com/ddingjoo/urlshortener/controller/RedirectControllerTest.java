package com.ddingjoo.urlshortener.controller;

import com.ddingjoo.urlshortener.exception.handler.RestExceptionHandler;
import com.ddingjoo.urlshortener.service.ratelimit.RateLimitService;
import com.ddingjoo.urlshortener.service.url.UrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(RestExceptionHandler.class)
@WebMvcTest(
		controllers = RedirectController.class,
		properties = {
				"SERVER_PORT=0",
				"APP_BASE_URL=http://localhost:8080",
				"ADMIN_API_KEY=test-admin-key",
				"SHORT_CODE_OBFUSCATION_KEY=912345",
				"DB_HOST=localhost",
				"DB_PORT=5432",
				"POSTGRES_DB=test_db",
				"POSTGRES_USER=test_user",
				"POSTGRES_PASSWORD=test_password",
				"REDIS_HOST=localhost",
				"REDIS_PORT=6379"
		}
)
class RedirectControllerTest {
	
	@Autowired
	private MockMvc mockMvc;
	
	@MockitoBean
	private UrlService urlService;
	
	@MockitoBean
	private RateLimitService rateLimitService;
	
	@Test
	void redirectsToOriginalUrl() throws Exception {
		when(urlService.resolveOriginalUrl("3Ple")).thenReturn("https://example.com/docs");
		
		mockMvc.perform(get("/3Ple"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", "https://example.com/docs"));
	}
}
