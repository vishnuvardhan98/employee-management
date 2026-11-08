package com.peopledesk.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peopledesk.auth.RevokedAccessTokenRepository;
import com.peopledesk.common.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    private static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",   // Angular Material sets inline styles
            "img-src 'self' data:",
            "font-src 'self' data:",
            "connect-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "frame-ancestors 'none'");

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
                                            RevokedAccessTokenRepository revokedTokens,
                                            ObjectMapper objectMapper) throws Exception {
        http
                // Stateless API authenticated by a header token, so there is no cookie to forge (no CSRF risk).
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> { })
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h.contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/reports/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                        .requestMatchers("/api/**").hasRole("ADMIN")      // POST / PUT / DELETE
                        .anyRequest().permitAll())                        // static Angular files
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> writeError(objectMapper, req, res,
                                HttpStatus.UNAUTHORIZED, "Your session has expired. Please sign in again."))
                        .accessDeniedHandler((req, res, ex) -> writeError(objectMapper, req, res,
                                HttpStatus.FORBIDDEN, "You do not have permission to perform this action.")))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, revokedTokens),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    /** Only needed when the Angular dev server calls the API directly instead of via its proxy. */
    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") List<String> origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Content-Disposition"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private static void writeError(ObjectMapper mapper, HttpServletRequest request, HttpServletResponse response,
                                   HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(),
                ApiError.of(status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
    }
}
