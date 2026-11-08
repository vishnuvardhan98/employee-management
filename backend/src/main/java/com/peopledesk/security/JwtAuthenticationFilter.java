package com.peopledesk.security;

import com.peopledesk.auth.RevokedAccessTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads "Authorization: Bearer &lt;jwt&gt;", verifies it and populates the SecurityContext.
 * Invalid, expired or revoked tokens are simply ignored, so protected endpoints answer 401.
 * The role is taken from the signed token, so no database lookup is needed per request.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;
    private final RevokedAccessTokenRepository revokedTokens;

    public JwtAuthenticationFilter(JwtService jwtService, RevokedAccessTokenRepository revokedTokens) {
        this.jwtService = jwtService;
        this.revokedTokens = revokedTokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER)) {
            jwtService.parse(header.substring(BEARER.length()).trim())
                    .filter(claims -> !revokedTokens.existsById(claims.jti()))
                    .ifPresent(claims -> {
                        var authentication = new UsernamePasswordAuthenticationToken(
                                claims.username(), null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + claims.role().name())));
                        authentication.setDetails(claims);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }
        chain.doFilter(request, response);
    }
}
