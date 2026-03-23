package com.bhak.project.configuration;

import com.bhak.project.filter.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Core HTTP security configuration (Spring Security 6).
 *
 * <h2>Purpose</h2>
 * Defines the security filter chain (SecurityFilterChain) and the authentication
 * beans used throughout the application.
 *
 * <h2>How it works</h2>
 * <ul>
 *   <li><b>CSRF disabled</b>: the API is consumed by REST clients (Postman, SPA front-end),
 *       no HTML forms → CSRF protection is not needed.</li>
 *   <li><b>Stateless sessions</b>: {@code SessionCreationPolicy.STATELESS}.
 *       No HTTP session is created; authentication relies entirely on the JWT
 *       sent in the {@code Authorization} header.</li>
 *   <li><b>Public routes</b>: {@code /api/auth/**}, Swagger UI, root {@code /}.</li>
 *   <li><b>Admin routes</b>: {@code /api/admin/**} requires the {@code ADMIN} role.</li>
 *   <li><b>JWT filter</b>: the {@link com.bhak.project.filter.JwtFilter} is inserted
 *       <em>before</em> the standard {@code UsernamePasswordAuthenticationFilter},
 *       so the user is authenticated from the token before any role check.</li>
 *   <li><b>DaoAuthenticationProvider</b>: uses {@link com.bhak.project.service.CustomUserDetailsService}
 *       to load users and {@link org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder}
 *       to compare passwords.</li>
 * </ul>
 *
 * <h2>Annotations</h2>
 * {@code @Configuration}, {@code @EnableWebSecurity}, {@code @EnableMethodSecurity}
 * (enables {@code @PreAuthorize} in controllers), Lombok ({@code @RequiredArgsConstructor}).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final UserDetailsService userDetailsService;

    // chaine de filtres de securite
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/"
                ).permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
