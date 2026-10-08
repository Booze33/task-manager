package com.booze.task_manager.config;

import com.booze.task_manager.model.UserPrincipal;
import com.booze.task_manager.service.JwtService;
import com.booze.task_manager.service.TokenRevocationService;
import com.booze.task_manager.service.UserDetailServiceImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static org.aspectj.weaver.NameMangler.PREFIX;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailServiceImpl userDetailsService;
    private final TokenRevocationService revocationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            authenticate(header.substring(PREFIX.length()).trim());
        }

        String token = header.substring(7);
        try {
            Claims claims = jwtService.parseToken(token);
            String jti = claims.getId();

            if (jti == null || revocationService.isRevoked(jti)) {
                unauthorized(response);
                return;
            }

            String userId = claims.getSubject();

            if (userId == null) {
                unauthorized(response);
                return;
            }

            UserPrincipal user = (UserPrincipal) userDetailsService.loadById(userId);

            if (!user.isEnabled()) {
                unauthorized(response);
                return;
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    user.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException e) {
            unauthorized(response);
        }
    }

    private void authenticate(String token) {
        try {
            Claims claims = jwtService.parseToken(token);
            String jti = claims.getId();
            String userId = claims.getSubject();

            if (jti == null || userId == null || revocationService.isRevoked(jti)) {
                return;
            }

            UserPrincipal user = userDetailsService.loadById(userId);
            if (!user.isEnabled()) {
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            SecurityContextHolder.clearContext();
        }
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("""
                {
                    "error": "Unauthorized",
                    "message": "Invalid or expired authentication token"
                }
                """);
    }
}
