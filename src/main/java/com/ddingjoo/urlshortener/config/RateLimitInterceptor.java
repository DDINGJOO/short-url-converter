package com.ddingjoo.urlshortener.config;

import com.ddingjoo.urlshortener.service.ratelimit.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    public RateLimitInterceptor(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (shouldSkip(request.getRequestURI())) {
            return true;
        }

        rateLimitService.validate(extractClientKey(request));
        return true;
    }

    private boolean shouldSkip(String uri) {
        return uri.startsWith("/api/health")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs");
    }

    private String extractClientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
