package com.peopledesk.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.peopledesk.auth.AuthDtos.AuthResponse;
import com.peopledesk.auth.AuthDtos.LoginRequest;
import com.peopledesk.security.AccessTokenClaims;
import com.peopledesk.security.JwtProperties;
import com.peopledesk.security.JwtService;
import com.peopledesk.user.Role;
import com.peopledesk.user.User;
import com.peopledesk.user.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;

class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-08T10:00:00Z");
    private static final JwtProperties PROPS = new JwtProperties(
            "unit-test-secret-with-more-than-32-bytes!", Duration.ofMinutes(15), Duration.ofHours(8), Duration.ofDays(7));

    private final AuthenticationManager authManager = mock(AuthenticationManager.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    private final RevokedAccessTokenRepository revoked = mock(RevokedAccessTokenRepository.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final JwtService jwt = new JwtService(PROPS, clock);
    private AuthService service;
    private final User admin = new User("admin", "hash", "Alex Morgan", Role.ADMIN);

    @BeforeEach
    void setUp() {
        service = new AuthService(authManager, users, refreshTokens, revoked, jwt, PROPS, clock);
    }

    @Test
    void loginIssuesAccessAndHashedRefreshToken() {
        when(users.findByUsername("admin")).thenReturn(Optional.of(admin));

        AuthResponse response = service.login(new LoginRequest(" admin ", "Admin@123", false));

        assertThat(jwt.parse(response.accessToken())).isPresent();
        assertThat(response.user().role()).isEqualTo(Role.ADMIN);
        assertThat(response.refreshTokenExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(8)));

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokens).save(saved.capture());
        assertThat(saved.getValue().getTokenHash())
                .isEqualTo(AuthService.hash(response.refreshToken()))
                .isNotEqualTo(response.refreshToken());
    }

    @Test
    void rememberMeUsesLongerRefreshLifetime() {
        when(users.findByUsername("admin")).thenReturn(Optional.of(admin));

        AuthResponse response = service.login(new LoginRequest("admin", "Admin@123", true));

        assertThat(response.refreshTokenExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
    }

    @Test
    void loginWithBadCredentialsGivesGenericMessage() {
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> service.login(new LoginRequest("admin", "wrong-password", false)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage(AuthService.INVALID_CREDENTIALS);
    }

    @Test
    void refreshRotatesTokenAndKeepsOriginalExpiry() {
        LocalDateTime expiry = LocalDateTime.ofInstant(NOW.plus(Duration.ofHours(2)), ZoneOffset.UTC);
        RefreshToken stored = new RefreshToken(AuthService.hash("old"), admin, expiry, LocalDateTime.now());
        when(refreshTokens.findByTokenHash(AuthService.hash("old"))).thenReturn(Optional.of(stored));

        AuthResponse response = service.refresh("old");

        verify(refreshTokens).delete(stored);
        assertThat(response.refreshToken()).isNotEqualTo("old");
        assertThat(response.refreshTokenExpiresAt()).isEqualTo(expiry.toInstant(ZoneOffset.UTC));
    }

    @Test
    void refreshRejectsExpiredToken() {
        LocalDateTime past = LocalDateTime.ofInstant(NOW.minusSeconds(1), ZoneOffset.UTC);
        RefreshToken stored = new RefreshToken(AuthService.hash("old"), admin, past, LocalDateTime.now());
        when(refreshTokens.findByTokenHash(AuthService.hash("old"))).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.refresh("old")).isInstanceOf(BadCredentialsException.class);
        verify(refreshTokens).delete(stored);
    }

    @Test
    void refreshRejectsUnknownToken() {
        when(refreshTokens.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("unknown")).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void logoutDeletesRefreshTokenAndRevokesAccessToken() {
        AccessTokenClaims claims = new AccessTokenClaims("admin", Role.ADMIN, "jti-1", NOW.plusSeconds(600));

        service.logout("refresh", claims);

        verify(refreshTokens).deleteByTokenHash(AuthService.hash("refresh"));
        ArgumentCaptor<RevokedAccessToken> captor = ArgumentCaptor.forClass(RevokedAccessToken.class);
        verify(revoked).save(captor.capture());
        assertThat(captor.getValue().getJti()).isEqualTo("jti-1");
    }
}
