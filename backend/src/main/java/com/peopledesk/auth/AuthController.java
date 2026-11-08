package com.peopledesk.auth;

import com.peopledesk.auth.AuthDtos.AuthResponse;
import com.peopledesk.auth.AuthDtos.LoginRequest;
import com.peopledesk.auth.AuthDtos.LogoutRequest;
import com.peopledesk.auth.AuthDtos.RefreshRequest;
import com.peopledesk.auth.AuthDtos.UserInfo;
import com.peopledesk.security.AccessTokenClaims;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    /** Works even with an expired access token, so the refresh token can always be revoked. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) LogoutRequest request) {
        AccessTokenClaims claims = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof AccessTokenClaims details) {
            claims = details;
        }
        authService.logout(request == null ? null : request.refreshToken(), claims);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserInfo me(Authentication authentication) {
        return authService.currentUser(authentication.getName());
    }
}
