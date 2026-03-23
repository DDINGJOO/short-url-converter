package com.ddingjoo.urlshortener.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ddingjoo.urlshortener.service.ratelimit.RateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = HealthController.class,
        properties = {
                "SERVER_PORT=0",
                "APP_BASE_URL=http://localhost:8080",
                "ADMIN_API_KEY=test-admin-key",
                "DB_HOST=localhost",
                "DB_PORT=5432",
                "POSTGRES_DB=test_db",
                "POSTGRES_USER=test_user",
                "POSTGRES_PASSWORD=test_password",
                "REDIS_HOST=localhost",
                "REDIS_PORT=6379"
        }
)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RateLimitService rateLimitService;

    @Test
    void returnsHealthPayload() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
