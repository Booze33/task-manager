package com.booze.task_manager.config;

import com.booze.task_manager.service.RateLimitService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.booze.task_manager.model.Role;
import com.booze.task_manager.model.User;
import com.booze.task_manager.model.UserPrincipal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ApiRateLimitFilterTest {
    private final RateLimitService rateLimitService = mock(RateLimitService.class);
    private final ApiRateLimitFilter filter = new ApiRateLimitFilter(rateLimitService);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void appliesQuotaHeadersAndPassesAllowedRequest() throws Exception {
        when(rateLimitService.consume("ip:127.0.0.1"))
                .thenReturn(new RateLimitService.RateLimitResult(true, 99, 1_800_000_000));
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals("99", response.getHeader("X-RateLimit-Remaining"));
        assertEquals("1800000000", response.getHeader("X-RateLimit-Reset"));
        verify(chain).doFilter(request, response);
    }

    @Test
    void returns429AndDoesNotContinueWhenQuotaIsExceeded() throws Exception {
        when(rateLimitService.consume("ip:127.0.0.1"))
                .thenReturn(new RateLimitService.RateLimitResult(false, 0, 1_800_000_000));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request(), response, chain);

        assertEquals(429, response.getStatus());
        assertEquals("0", response.getHeader("X-RateLimit-Remaining"));
        verifyNoInteractions(chain);
    }

    @Test
    void keysAuthenticatedTrafficByUserRatherThanClientIp() throws Exception {
        User user = new User();
        user.setId("user-42");
        user.setEmail("alex@example.com");
        user.setRole(Role.USER);
        UserPrincipal principal = UserPrincipal.forToken(user);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        when(rateLimitService.consume("user:user-42"))
                .thenReturn(new RateLimitService.RateLimitResult(true, 98, 1_800_000_000));

        filter.doFilter(request(), new MockHttpServletResponse(), mock(FilterChain.class));

        verify(rateLimitService).consume("user:user-42");
    }

    @Test
    void returnsServiceUnavailableInsteadOfBypassingQuotaWhenRedisFails() throws Exception {
        when(rateLimitService.consume("ip:127.0.0.1"))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("Redis is down"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request(), response, mock(FilterChain.class));

        assertEquals(503, response.getStatus());
        verify(rateLimitService).consume("ip:127.0.0.1");
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tasks");
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}
