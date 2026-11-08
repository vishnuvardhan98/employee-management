package com.peopledesk.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Access token (by JWT id) invalidated at logout, kept until its natural expiry. */
@Entity
@Table(name = "revoked_access_tokens")
public class RevokedAccessToken {

    @Id
    @Column(length = 36)
    private String jti;

    /** UTC. */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    protected RevokedAccessToken() {
    }

    public RevokedAccessToken(String jti, LocalDateTime expiresAt) {
        this.jti = jti;
        this.expiresAt = expiresAt;
    }

    public String getJti() {
        return jti;
    }
}
