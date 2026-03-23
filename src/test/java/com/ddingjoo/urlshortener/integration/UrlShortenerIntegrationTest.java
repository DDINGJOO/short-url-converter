package com.ddingjoo.urlshortener.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UrlShortenerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("SERVER_PORT", () -> "0");
        registry.add("APP_BASE_URL", () -> "http://localhost:8080");
        registry.add("ADMIN_API_KEY", () -> "integration-admin-key");
        registry.add("SHORT_CODE_OBFUSCATION_KEY", () -> "912345");
        registry.add("RATE_LIMIT_PER_MINUTE", () -> "1000");
        registry.add("app.click-sync-interval-ms", () -> "600000");
        registry.add("app.analytics-sync-interval-ms", () -> "600000");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void supportsFullLifecycle() throws Exception {
        String response = mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "original_url": "https://example.com/integration"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.short_code").isNotEmpty())
                .andExpect(jsonPath("$.short_url").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String shortCode = response.replaceAll(".*\"short_code\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/integration"));

        mockMvc.perform(get("/api/urls/" + shortCode + "/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.short_code").value(shortCode))
                .andExpect(jsonPath("$.total_clicks").value(1))
                .andExpect(jsonPath("$.is_active").value(true));

        mockMvc.perform(get("/api/urls/" + shortCode + "/analytics")
                        .param("granularity", "hour")
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2030-01-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.short_code").value(shortCode))
                .andExpect(jsonPath("$.granularity").value("hour"))
                .andExpect(jsonPath("$.buckets", hasSize(1)))
                .andExpect(jsonPath("$.buckets[0].click_count").value(1));

        mockMvc.perform(delete("/api/urls/" + shortCode)
                        .header("X-API-KEY", "integration-admin-key"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isGone());
    }
}
