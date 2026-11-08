package com.peopledesk.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.peopledesk.user.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-with-more-than-32-bytes!";
    private static final Instant NOW = Instant.parse("2026-11-08T10:00:00Z");

    private JwtService service(String secret, Clock clock) {
        return new JwtService(new JwtProperties(secret, Duration.ofMinutes(15), Duration.ofHours(8), Duration.ofDays(7)), clock);
    }

    @Test
    void issuedTokenRoundTripsUsernameRoleAndExpiry() {
        JwtService jwt = service(SECRET, Clock.fixed(NOW, ZoneOffset.UTC));

        JwtService.IssuedToken issued = jwt.issue("admin", Role.ADMIN);

        assertThat(jwt.parse(issued.token())).hasValueSatisfying(claims -> {
            assertThat(claims.username()).isEqualTo("admin");
            assertThat(claims.role()).isEqualTo(Role.ADMIN);
            assertThat(claims.jti()).isEqualTo(issued.jti());
            assertThat(claims.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        });
    }

    @Test
    void rejectsExpiredToken() {
        String token = service(SECRET, Clock.fixed(NOW, ZoneOffset.UTC)).issue("admin", Role.ADMIN).token();
        JwtService later = service(SECRET, Clock.fixed(NOW.plus(Duration.ofMinutes(16)), ZoneOffset.UTC));

        assertThat(later.parse(token)).isEmpty();
    }

    @Test
    void rejectsTokenSignedWithDifferentKey() {
        String token = service(SECRET, Clock.systemUTC()).issue("admin", Role.ADMIN).token();
        JwtService other = service("another-secret-that-is-also-32-bytes-long", Clock.systemUTC());

        assertThat(other.parse(token)).isEmpty();
    }

    @Test
    void rejectsTamperedAndGarbageTokens() {
        JwtService jwt = service(SECRET, Clock.systemUTC());
        String token = jwt.issue("viewer", Role.USER).token();
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        assertThat(jwt.parse(tampered)).isEmpty();
        assertThat(jwt.parse("not-a-jwt")).isEmpty();
    }

    @Test
    void refusesShortSecret() {
        assertThatThrownBy(() -> service("too-short", Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class);
    }
}
