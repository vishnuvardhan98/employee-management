package com.peopledesk.security;

import com.peopledesk.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Issues and verifies short-lived HMAC-SHA256 signed access tokens. */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String ISSUER = "peopledesk";
    private static final int MIN_SECRET_BYTES = 32;

    public record IssuedToken(String token, String jti, Instant expiresAt) {
    }

    private final SecretKey key;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.key = Keys.hmacShaKeyFor(secretBytes(properties.secret()));
    }

    public IssuedToken issue(String username, Role role) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.accessTokenTtl());
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .issuer(ISSUER)
                .subject(username)
                .id(jti)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        return new IssuedToken(token, jti, expiresAt);
    }

    /** Returns the claims if the signature, issuer and expiry are valid; otherwise empty. */
    public Optional<AccessTokenClaims> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(ISSUER)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new AccessTokenClaims(
                    claims.getSubject(),
                    Role.valueOf(claims.get("role", String.class)),
                    claims.getId(),
                    claims.getExpiration().toInstant()));
        } catch (JwtException | IllegalArgumentException | NullPointerException ex) {
            log.debug("Rejected access token: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private static byte[] secretBytes(String secret) {
        if (!StringUtils.hasText(secret)) {
            log.warn("JWT_SECRET is not set; using a random signing key. Sessions will not survive a restart.");
            byte[] random = new byte[64];
            new SecureRandom().nextBytes(random);
            return random;
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes long.");
        }
        return bytes;
    }
}
