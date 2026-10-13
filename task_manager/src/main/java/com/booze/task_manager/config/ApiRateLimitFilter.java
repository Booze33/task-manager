package com.booze.task_manager.config;

import com.booze.task_manager.model.UserPrincipal;
import com.booze.task_manager.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiRateLimitFilter extends OncePerRequestFilter {
    private final RateLimitService rateLimitService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(request.getContextPath() + "/api/")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String identity = identity(request);
        RateLimitService.RateLimitResult result;
        try {
            result = rateLimitService.consume(identity);
        } catch (DataAccessException exception) {
            log.error("Rate limiter unavailable; rejecting API request", exception);
            writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Rate limiter unavailable");
            return;
        }

        response.setHeader("X-RateLimit-Remaining", Long.toString(result.remaining()));
        response.setHeader("X-RateLimit-Reset", Long.toString(result.resetEpochSeconds()));
        if (!result.allowed()) {
            writeError(response, 429, "Rate limit exceeded");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static String identity(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserPrincipal user) {
            return "user:" + user.getId();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private static void writeError(HttpServletResponse response, int status, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":" + status + ",\"title\":\""
                + (status == 429 ? "Too Many Requests" : "Service Unavailable")
                + "\",\"detail\":\"" + detail + "\"}");
    }
}
