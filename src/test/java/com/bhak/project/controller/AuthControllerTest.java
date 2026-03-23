package com.bhak.project.controller;

import com.bhak.project.configuration.JwtUtils;
import com.bhak.project.entity.*;
import com.bhak.project.exception.DuplicateResourceException;
import com.bhak.project.exception.GlobalExceptionHandler;
import com.bhak.project.exception.InvalidRequestException;
import com.bhak.project.filter.JwtFilter;
import com.bhak.project.repository.CredentialsRepository;
import com.bhak.project.repository.RoleRepository;
import com.bhak.project.repository.UserRepository;
import com.bhak.project.service.CustomUserDetailsService;
import com.bhak.project.service.VerificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for the {@link AuthController} controller (web layer only).
 *
 * <h2>Purpose</h2>
 * Verifies that the authentication endpoints ({@code /api/auth/register},
 * {@code /api/auth/verify}, {@code /api/auth/login}) return the correct
 * HTTP status codes and response bodies for each scenario (success, 400,
 * 401, 409 errors).
 *
 * <h2>Approach</h2>
 * <ul>
 *   <li>{@code @WebMvcTest(AuthController.class)} – loads only the MVC context
 *       (no database, no RabbitMQ), making the tests fast and isolated.</li>
 *   <li>{@code @AutoConfigureMockMvc(addFilters = false)} – disables security filters
 *       (including {@code JwtFilter}) to test controller logic independently
 *       of authentication.</li>
 *   <li>Controller dependencies ({@code UserRepository}, {@code JwtUtils},
 *       {@code VerificationService}, etc.) are mocked with {@code @MockBean} (Mockito).</li>
 * </ul>
 *
 * <h2>Tested scenarios</h2>
 * <ul>
 *   <li><b>Register</b>: success (201), missing fields (400), duplicate username (409).</li>
 *   <li><b>Verify</b>: success (200), invalid token (400).</li>
 *   <li><b>Login</b>: success with JWT (200), wrong credentials (401), missing fields (400).</li>
 * </ul>
 *
 * <h2>Technologies</h2>
 * JUnit 5, Spring MockMvc, Mockito ({@code @MockBean}), Jackson ({@code ObjectMapper}).
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private UserRepository userRepository;
    @MockBean private RoleRepository roleRepository;
    @MockBean private CredentialsRepository credentialsRepository;
    @MockBean private PasswordEncoder passwordEncoder;
    @MockBean private JwtUtils jwtUtils;
    @MockBean private AuthenticationManager authenticationManager;
    @MockBean private VerificationService verificationService;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private JwtFilter jwtFilter;

    // ---- register ----

    @Test
    void register_success_returns201() throws Exception {
        Role role = new Role(1L, RoleType.USER, "Utilisateur");
        when(userRepository.findByUsername("bob")).thenReturn(null);
        when(credentialsRepository.findByEmail("bob@test.com")).thenReturn(null);
        when(roleRepository.findByName(RoleType.USER)).thenReturn(role);
        when(passwordEncoder.encode("pass123")).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        String body = objectMapper.writeValueAsString(Map.of(
                "username", "bob",
                "email", "bob@test.com",
                "password", "pass123"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("bob"));
    }

    @Test
    void register_missingFields_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", "bob"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        User existing = new User();
        existing.setId(1L);
        existing.setUsername("bob");
        when(userRepository.findByUsername("bob")).thenReturn(existing);

        String body = objectMapper.writeValueAsString(Map.of(
                "username", "bob",
                "email", "bob@test.com",
                "password", "pass123"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    // ---- verify ----

    @Test
    void verify_success_returns200() throws Exception {
        doNothing().when(verificationService).verify("tok_abc", "raw_token");

        mockMvc.perform(get("/api/auth/verify")
                        .param("tokenId", "tok_abc")
                        .param("t", "raw_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Compte vérifié avec succès"));
    }

    @Test
    void verify_invalidToken_returns400() throws Exception {
        doThrow(new InvalidRequestException("Token de verification invalide"))
                .when(verificationService).verify("tok_bad", "raw");

        mockMvc.perform(get("/api/auth/verify")
                        .param("tokenId", "tok_bad")
                        .param("t", "raw"))
                .andExpect(status().isBadRequest());
    }

    // ---- login ----

    @Test
    void login_success_returnsToken() throws Exception {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtUtils.generateToken("alice")).thenReturn("jwt.token.here");

        String body = objectMapper.writeValueAsString(Map.of(
                "username", "alice",
                "password", "pass123"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.here"))
                .andExpect(jsonPath("$.type").value("Bearer"));
    }

    @Test
    void login_badCredentials_returns401() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));

        String body = objectMapper.writeValueAsString(Map.of(
                "username", "alice",
                "password", "wrong"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_missingFields_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", "alice"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
