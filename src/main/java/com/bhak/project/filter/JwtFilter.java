package com.bhak.project.filter;

import com.bhak.project.configuration.JwtUtils;
import com.bhak.project.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * HTTP filter executed once per request for stateless JWT authentication.
 *
 * <h2>Purpose</h2>
 * Intercepts every incoming HTTP request, extracts the JWT token from the
 * {@code Authorization: Bearer <token>} header, validates it, and if correct,
 * places a {@link org.springframework.security.authentication.UsernamePasswordAuthenticationToken}
 * in the {@link org.springframework.security.core.context.SecurityContextHolder}.
 * This allows Spring Security and {@code @PreAuthorize} to recognise the
 * authenticated user without relying on an HTTP session (stateless).
 *
 * <h2>How it works</h2>
 * <ol>
 *   <li>Reads the {@code Authorization} header. If absent or not prefixed with
 *       {@code "Bearer "}, the request passes to the next filter without authentication.</li>
 *   <li>Extracts the username via {@link com.bhak.project.configuration.JwtUtils#extractLogin(String)}.</li>
 *   <li>Loads the corresponding {@link org.springframework.security.core.userdetails.UserDetails}
 *       from {@link com.bhak.project.service.CustomUserDetailsService}.</li>
 *   <li>Validates the token (signature + expiration) with {@code JwtUtils#isTokenValid()}.</li>
 *   <li>On success, creates a {@code UsernamePasswordAuthenticationToken} with the account's
 *       authorities and injects it into the {@code SecurityContext}.</li>
 *   <li>On failure (expired or invalid token), a warning is logged and the request
 *       continues without authentication (Spring Security will return a 401 or 403).</li>
 * </ol>
 *
 * <h2>Technologies</h2>
 * Extends {@link org.springframework.web.filter.OncePerRequestFilter} (ensures
 * a single execution per request), Lombok, SLF4J.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String header = request.getHeader(AUTH_HEADER);

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = header.substring(BEARER_PREFIX.length());

        try {
            String username = jwtUtils.extractLogin(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails details = userDetailsService.loadUserByUsername(username);

                if (jwtUtils.isTokenValid(token, details)) {
                    var authToken = new UsernamePasswordAuthenticationToken(
                            details, null, details.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ex) {
            log.warn("Token JWT rejeté : {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
