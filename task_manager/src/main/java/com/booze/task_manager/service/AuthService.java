package com.booze.task_manager.service;

import com.booze.task_manager.dto.AuthResponse;
import com.booze.task_manager.dto.LoginRequest;
import com.booze.task_manager.dto.RegisterRequest;
import com.booze.task_manager.exception.EmailAlreadyUsedException;
import com.booze.task_manager.exception.InvalidRefreshTokenException;
import com.booze.task_manager.model.Role;
import com.booze.task_manager.model.User;
import com.booze.task_manager.model.UserPrincipal;
import com.booze.task_manager.repo.UserRepo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final String BEARER = "Bearer ";

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TokenRevocationService tokenRevocationService;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;

    public AuthResponse signup(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepo.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyUsedException();
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .enabled(true)
                .role(Role.USER)
                .build();

        try {
            userRepo.saveAndFlush(user);
        } catch (DataIntegrityViolationException raceLost) {
            throw new EmailAlreadyUsedException();
        }

        return issueTokens(user.getId());
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (BadCredentialsException e) {
            loginAttemptService.recordFailure(email);
            throw e;
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        loginAttemptService.recordSuccess(principal.getId());
        return issueTokens(principal.getId());
    }

    public AuthResponse refresh(String rawRefreshToken) {
        RefreshTokenService.Rotated rotated = refreshTokenService.rotate(rawRefreshToken);

        User user = userRepo.findById(rotated.userId())
                .orElseThrow(InvalidRefreshTokenException::new);
        if (!user.isEnabled()) {
            refreshTokenService.revokeAllForUser(user.getId());
            throw new InvalidRefreshTokenException();
        }

        return new AuthResponse(
                jwtService.generateAccessToken(user.getId()),
                rotated.newRawToken(),
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds());
    }

    public void logout(String authorizationHeader, String refreshToken) {
        denylistAccessToken(authorizationHeader);
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revokeFamilyOf(refreshToken);
        }
    }

    public void logoutAll(String userId, String authorizationHeader) {
        denylistAccessToken(authorizationHeader);
        refreshTokenService.revokeAllForUser(userId);
    }

    private AuthResponse issueTokens(String userId) {
        return new AuthResponse(
                jwtService.generateAccessToken(userId),
                refreshTokenService.issueNewFamily(userId),
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds());
    }

    private void denylistAccessToken(String header) {
        if (header == null || !header.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            return;
        }
        try {
            Claims claims = jwtService.parseToken(header.substring(BEARER.length()).trim());
            tokenRevocationService.revoke(claims.getId(), claims.getExpiration().toInstant());
        } catch (JwtException | IllegalArgumentException ignored) {
            // Already invalid -> nothing to revoke.
        }
    }
}