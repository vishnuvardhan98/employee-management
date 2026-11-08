package com.peopledesk.security;

import com.peopledesk.user.Role;
import java.time.Instant;

/** The verified contents of an access token. */
public record AccessTokenClaims(String username, Role role, String jti, Instant expiresAt) {
}
