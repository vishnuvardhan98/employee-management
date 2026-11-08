package com.peopledesk.auth;

import com.peopledesk.user.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Request/response bodies for /api/auth. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "Username is required.")
            @Size(min = 3, max = 50, message = "Username must be 3–50 characters.")
            String username,

            @NotBlank(message = "Password is required.")
            @Size(min = 6, max = 100, message = "Password must be 6–100 characters.")
            String password,

            boolean rememberMe) {
    }

    public record RefreshRequest(@NotBlank(message = "Refresh token is required.") String refreshToken) {
    }

    public record LogoutRequest(String refreshToken) {
    }

    public record UserInfo(String username, String fullName, Role role) {
    }

    public record AuthResponse(
            String accessToken,
            Instant accessTokenExpiresAt,
            String refreshToken,
            Instant refreshTokenExpiresAt,
            UserInfo user) {
    }
}
