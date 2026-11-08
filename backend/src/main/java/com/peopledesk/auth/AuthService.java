package com.peopledesk.auth;

import com.peopledesk.auth.AuthDtos.AuthResponse;
import com.peopledesk.auth.AuthDtos.LoginRequest;
import com.peopledesk.auth.AuthDtos.UserInfo;
import com.peopledesk.security.AccessTokenClaims;
import com.peopledesk.security.JwtProperties;
import com.peopledesk.security.JwtService;
import com.peopledesk.security.JwtService.IssuedToken;
import com.peopledesk.user.User;
import com.peopledesk.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Login issues a short-lived JWT access token plus an opaque refresh token. Refresh tokens are
 * rotated on every use and keep their original absolute expiry, so "remember me" cannot extend a
 * session indefinitely. Logout deletes the refresh token and revokes the current access token.
 */
@Service
public class AuthService {

    static final String INVALID_CREDENTIALS = "Username or password is incorrect.";
    static final String SESSION_EXPIRED = "Your session has expired. Please sign in again.";

    private final AuthenticationManager authenticationManager;
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final RevokedAccessTokenRepository revokedTokens;
    private final JwtService jwtService;
    private final JwtProperties properties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public AuthService(AuthenticationManager authenticationManager, UserRepository users,
                       RefreshTokenRepository refreshTokens, RevokedAccessTokenRepository revokedTokens,
                       JwtService jwtService, JwtProperties properties, Clock clock) {
        this.authenticationManager = authenticationManager;
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.revokedTokens = revokedTokens;
        this.jwtService = jwtService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String username = request.username().trim();
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, request.password()));
        } catch (AuthenticationException ex) {
            // Same message for unknown user and wrong password, so usernames cannot be probed.
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }
        User user = users.findByUsername(username).orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS));
        Instant refreshExpiry = clock.instant().plus(request.rememberMe()
                ? properties.rememberMeRefreshTokenTtl()
                : properties.refreshTokenTtl());
        return issueTokens(user, refreshExpiry);
    }

    /** The used token is deleted even when it turns out to be expired (hence noRollbackFor). */
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokens.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new BadCredentialsException(SESSION_EXPIRED));
        refreshTokens.delete(stored); // rotation: each refresh token works exactly once
        Instant expiry = stored.getExpiresAt().toInstant(ZoneOffset.UTC);
        if (!expiry.isAfter(clock.instant())) {
            throw new BadCredentialsException(SESSION_EXPIRED);
        }
        return issueTokens(stored.getUser(), expiry);
    }

    @Transactional
    public void logout(String rawRefreshToken, AccessTokenClaims accessToken) {
        if (StringUtils.hasText(rawRefreshToken)) {
            refreshTokens.deleteByTokenHash(hash(rawRefreshToken));
        }
        if (accessToken != null && !revokedTokens.existsById(accessToken.jti())) {
            revokedTokens.save(new RevokedAccessToken(accessToken.jti(), utc(accessToken.expiresAt())));
        }
    }

    public UserInfo currentUser(String username) {
        return users.findByUsername(username)
                .map(AuthService::toInfo)
                .orElseThrow(() -> new BadCredentialsException(SESSION_EXPIRED));
    }

    private AuthResponse issueTokens(User user, Instant refreshExpiry) {
        IssuedToken access = jwtService.issue(user.getUsername(), user.getRole());
        String refresh = newRefreshToken();
        refreshTokens.save(new RefreshToken(hash(refresh), user, utc(refreshExpiry), utc(clock.instant())));
        return new AuthResponse(access.token(), access.expiresAt(), refresh, refreshExpiry, toInfo(user));
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static UserInfo toInfo(User user) {
        return new UserInfo(user.getUsername(), user.getFullName(), user.getRole());
    }
}
