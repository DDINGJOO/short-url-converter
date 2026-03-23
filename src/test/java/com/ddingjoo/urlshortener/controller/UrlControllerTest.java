package com.ddingjoo.urlshortener.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ddingjoo.urlshortener.dto.url.ShortenResponse;
import com.ddingjoo.urlshortener.dto.url.UrlAnalyticsResponse;
import com.ddingjoo.urlshortener.dto.url.UrlStatsResponse;
import com.ddingjoo.urlshortener.exception.RestExceptionHandler;
import com.ddingjoo.urlshortener.service.analytics.UrlAnalyticsService;
import com.ddingjoo.urlshortener.service.ratelimit.RateLimitService;
import com.ddingjoo.urlshortener.service.url.UrlService;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import(RestExceptionHandler.class)
@WebMvcTest(
        controllers = UrlController.class,
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
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UrlService urlService;

    @MockitoBean
    private UrlAnalyticsService urlAnalyticsService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @Test
    void createsShortUrl() throws Exception {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-03-23T15:00:00Z");
        ShortenResponse response = new ShortenResponse(
                "3PbD",
                "http://localhost:8080/3PbD",
                "https://example.com/docs",
                null,
                createdAt
        );

        when(urlService.shorten(any())).thenReturn(response);

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "original_url": "https://example.com/docs"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.short_code").value("3PbD"))
                .andExpect(jsonPath("$.short_url").value("http://localhost:8080/3PbD"))
                .andExpect(jsonPath("$.original_url").value("https://example.com/docs"))
                .andExpect(jsonPath("$.created_at").value("2026-03-23T15:00:00Z"));
    }

    @Test
    void returnsStats() throws Exception {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-03-23T15:00:00Z");
        UrlStatsResponse response = new UrlStatsResponse(
                "3Ple",
                "https://example.com/docs",
                12L,
                createdAt,
                null,
                true
        );

        when(urlService.getStats("3Ple")).thenReturn(response);

        mockMvc.perform(get("/api/urls/3Ple/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.short_code").value("3Ple"))
                .andExpect(jsonPath("$.total_clicks").value(12))
                .andExpect(jsonPath("$.is_active").value(true));
    }

    @Test
    void deletesWithApiKeyHeader() throws Exception {
        mockMvc.perform(delete("/api/urls/3Ple").header("X-API-KEY", "test-admin-key"))
                .andExpect(status().isNoContent());
    }

    @Test
    void returnsAnalytics() throws Exception {
        UrlAnalyticsResponse response = new UrlAnalyticsResponse(
                "3Ple",
                "hour",
                OffsetDateTime.parse("2026-03-23T00:00:00Z"),
                OffsetDateTime.parse("2026-03-23T03:00:00Z"),
                java.util.List.of()
        );

        when(urlAnalyticsService.getAnalytics(
                org.mockito.ArgumentMatchers.eq("3Ple"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(response);

        mockMvc.perform(get("/api/urls/3Ple/analytics")
                        .param("granularity", "hour")
                        .param("from", "2026-03-23T00:00:00Z")
                        .param("to", "2026-03-23T03:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.short_code").value("3Ple"))
                .andExpect(jsonPath("$.granularity").value("hour"));
    }
}
