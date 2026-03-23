package com.bhak.project.service;

import com.bhak.project.dto.EmailVerifiedEvent;
import com.bhak.project.dto.UserRegisteredEvent;
import com.bhak.project.entity.Credentials;
import com.bhak.project.entity.Role;
import com.bhak.project.entity.RoleType;
import com.bhak.project.entity.User;
import com.bhak.project.entity.VerificationToken;
import com.bhak.project.exception.InvalidRequestException;
import com.bhak.project.exception.ResourceNotFoundException;
import com.bhak.project.repository.UserRepository;
import com.bhak.project.repository.VerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link VerificationService} service (email verification).
 *
 * <h2>Purpose</h2>
 * Verifies the complete verification cycle: token generation + event publishing,
 * then token validation with all edge cases.
 *
 * <h2>Tested scenarios</h2>
 * <ul>
 *   <li><b>createTokenAndPublishEvent</b>: persists a {@code VerificationToken}
 *       with the correct fields (tokenId, userId, hash, expiration) and publishes a
 *       consistent {@code UserRegistered} event.</li>
 *   <li><b>verify (success)</b>: valid token → the account is marked as verified,
 *       the token is deleted (one-shot), an {@code EmailVerified} event is published.</li>
 *   <li><b>verify (expired token)</b>: throws {@code InvalidRequestException} and deletes the token.</li>
 *   <li><b>verify (wrong token)</b>: BCrypt does not match → {@code InvalidRequestException}.</li>
 *   <li><b>verify (unknown tokenId)</b>: {@code InvalidRequestException}.</li>
 *   <li><b>verify (idempotent)</b>: already verified account → no save, no event published.</li>
 *   <li><b>verify (user not found)</b>: {@code ResourceNotFoundException}.</li>
 * </ul>
 *
 * <h2>Technologies</h2>
 * JUnit 5, Mockito ({@code @Mock}, {@code @InjectMocks}, {@code ArgumentCaptor}),
 * Spring {@code ReflectionTestUtils}, AssertJ.
 */
@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock private VerificationTokenRepository tokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EventPublisher eventPublisher;

    @InjectMocks private VerificationService verificationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(verificationService, "tokenTtlMinutes", 30L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("alice");
        testUser.setRole(new Role(1L, RoleType.USER, "Utilisateur"));

        Credentials cred = new Credentials();
        cred.setEmail("alice@test.com");
        cred.setUser(testUser);
        testUser.setCredentials(cred);
    }

    // ---- createTokenAndPublishEvent ----

    @Test
    void createTokenAndPublishEvent_savesTokenAndPublishes() {
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");

        verificationService.createTokenAndPublishEvent(testUser);

        // verifie que le token est enregistre
        ArgumentCaptor<VerificationToken> tokenCaptor = ArgumentCaptor.forClass(VerificationToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        VerificationToken saved = tokenCaptor.getValue();
        assertThat(saved.getTokenId()).startsWith("tok_");
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getTokenHash()).isEqualTo("$2a$hashed");
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(29));

        // verifie que l'evenement est publie
        ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(eventPublisher).publishUserRegistered(eventCaptor.capture());
        UserRegisteredEvent event = eventCaptor.getValue();
        assertThat(event.getType()).isEqualTo("UserRegistered");
        assertThat(event.getUserId()).isEqualTo("1");
        assertThat(event.getEmail()).isEqualTo("alice@test.com");
        assertThat(event.getEventId()).isNotBlank();
        assertThat(event.getCorrelationId()).isNotBlank();
        assertThat(event.getTokenId()).startsWith("tok_");
        assertThat(event.getTokenClear()).isNotBlank();
    }

    // ---- verify ----

    @Test
    void verify_validToken_marksUserVerifiedAndPublishes() {
        VerificationToken vt = new VerificationToken("tok_abc", 1L, "$2a$hash", LocalDateTime.now().plusMinutes(15));

        when(tokenRepository.findById("tok_abc")).thenReturn(Optional.of(vt));
        when(passwordEncoder.matches("raw_token", "$2a$hash")).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        verificationService.verify("tok_abc", "raw_token");

        assertThat(testUser.isVerified()).isTrue();
        verify(userRepository).save(testUser);
        verify(tokenRepository).delete(vt);
        verify(eventPublisher).publishEmailVerified(any(EmailVerifiedEvent.class));
    }

    @Test
    void verify_expiredToken_throws() {
        VerificationToken vt = new VerificationToken("tok_abc", 1L, "$2a$hash", LocalDateTime.now().minusMinutes(1));

        when(tokenRepository.findById("tok_abc")).thenReturn(Optional.of(vt));

        assertThatThrownBy(() -> verificationService.verify("tok_abc", "raw_token"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("expire");

        verify(tokenRepository).delete(vt);
    }

    @Test
    void verify_wrongToken_throws() {
        VerificationToken vt = new VerificationToken("tok_abc", 1L, "$2a$hash", LocalDateTime.now().plusMinutes(15));

        when(tokenRepository.findById("tok_abc")).thenReturn(Optional.of(vt));
        when(passwordEncoder.matches("bad_token", "$2a$hash")).thenReturn(false);

        assertThatThrownBy(() -> verificationService.verify("tok_abc", "bad_token"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("invalide");
    }

    @Test
    void verify_unknownTokenId_throws() {
        when(tokenRepository.findById("tok_unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> verificationService.verify("tok_unknown", "raw"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void verify_alreadyVerified_isIdempotent() {
        testUser.setVerified(true);
        VerificationToken vt = new VerificationToken("tok_abc", 1L, "$2a$hash", LocalDateTime.now().plusMinutes(15));

        when(tokenRepository.findById("tok_abc")).thenReturn(Optional.of(vt));
        when(passwordEncoder.matches("raw", "$2a$hash")).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        verificationService.verify("tok_abc", "raw");

        verify(tokenRepository).delete(vt);
        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publishEmailVerified(any());
    }

    @Test
    void verify_userNotFound_throws() {
        VerificationToken vt = new VerificationToken("tok_abc", 99L, "$2a$hash", LocalDateTime.now().plusMinutes(15));

        when(tokenRepository.findById("tok_abc")).thenReturn(Optional.of(vt));
        when(passwordEncoder.matches("raw", "$2a$hash")).thenReturn(true);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> verificationService.verify("tok_abc", "raw"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
