package com.peopledesk.auth;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Hourly removal of expired refresh tokens and revoked-token entries. */
@Component
public class TokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupJob.class);

    private final RefreshTokenRepository refreshTokens;
    private final RevokedAccessTokenRepository revokedTokens;
    private final Clock clock;

    public TokenCleanupJob(RefreshTokenRepository refreshTokens, RevokedAccessTokenRepository revokedTokens,
                           Clock clock) {
        this.refreshTokens = refreshTokens;
        this.revokedTokens = revokedTokens;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void purgeExpired() {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        int refresh = refreshTokens.deleteExpired(now);
        int revoked = revokedTokens.deleteExpired(now);
        if (refresh + revoked > 0) {
            log.info("Purged {} expired refresh tokens and {} revoked access tokens", refresh, revoked);
        }
    }
}
