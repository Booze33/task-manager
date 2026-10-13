package com.booze.task_manager.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class CorsConfigurationTest {
    @Test
    void exposesApiSignalHeadersToBrowserClients() {
        SecurityConfig config = new SecurityConfig(mock(JwtAuthFilter.class), mock(ApiRateLimitFilter.class));

        var source = config.corsConfigurationSource(List.of("http://localhost:5173"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(5173);
        var cors = source.getCorsConfiguration(request);

        assertTrue(cors.getExposedHeaders().containsAll(List.of(
                "X-Cache",
                "X-Idempotent",
                "X-RateLimit-Remaining",
                "X-RateLimit-Reset")));
    }

    @Test
    void preflightAllowsConfiguredOriginAndExposesApiSignalHeaders() throws Exception {
        SecurityConfig config = new SecurityConfig(mock(JwtAuthFilter.class), mock(ApiRateLimitFilter.class));
        CorsFilter filter = new CorsFilter(config.corsConfigurationSource(List.of("http://localhost:5173")));
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/users/me/stats");
        request.addHeader("Origin", "http://localhost:5173");
        request.addHeader("Access-Control-Request-Method", "GET");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertTrue(response.getStatus() >= 200 && response.getStatus() < 300);
        assertEquals("http://localhost:5173", response.getHeader("Access-Control-Allow-Origin"));
        String exposedHeaders = response.getHeader("Access-Control-Expose-Headers");
        assertTrue(exposedHeaders.contains("X-Cache"));
        assertTrue(exposedHeaders.contains("X-Idempotent"));
        assertTrue(exposedHeaders.contains("X-RateLimit-Remaining"));
        assertTrue(exposedHeaders.contains("X-RateLimit-Reset"));
    }
}
