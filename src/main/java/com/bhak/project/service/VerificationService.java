package com.bhak.project.service;

import com.bhak.project.dto.EmailVerifiedEvent;
import com.bhak.project.dto.UserRegisteredEvent;
import com.bhak.project.entity.User;
import com.bhak.project.entity.VerificationToken;
import com.bhak.project.exception.InvalidRequestException;
import com.bhak.project.exception.ResourceNotFoundException;
import com.bhak.project.repository.UserRepository;
import com.bhak.project.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

// gere la generation des tokens de verification et la validation
@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;

    @Value("${app.verification.token-ttl-minutes}")
    private long tokenTtlMinutes;

    // genere un token, stocke le hash, et publie l'evenement rabbitmq
    @Transactional
    public void createTokenAndPublishEvent(User user) {
        String tokenId = "tok_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String tokenClear = UUID.randomUUID().toString();
        String tokenHash = passwordEncoder.encode(tokenClear);

        VerificationToken vt = new VerificationToken();
        vt.setTokenId(tokenId);
        vt.setUserId(user.getId());
        vt.setTokenHash(tokenHash);
        vt.setExpiresAt(LocalDateTime.now().plusMinutes(tokenTtlMinutes));
        tokenRepository.save(vt);

        String correlationId = UUID.randomUUID().toString();
        log.info("Token de verification cree [tokenId={}, userId={}, correlationId={}]", tokenId, user.getId(), correlationId);

        UserRegisteredEvent event = new UserRegisteredEvent();
        event.setType("UserRegistered");
        event.setEventId(UUID.randomUUID().toString());
        event.setOccurredAt(Instant.now());
        event.setUserId(String.valueOf(user.getId()));
        event.setEmail(user.getCredentials().getEmail());
        event.setTokenId(tokenId);
        event.setTokenClear(tokenClear);
        event.setCorrelationId(correlationId);

        eventPublisher.publishUserRegistered(event);
    }

    // verifie le token et marque le compte comme verifie (one-shot)
    @Transactional
    public void verify(String tokenId, String rawToken) {
        VerificationToken vt = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new InvalidRequestException("Token de verification invalide ou deja utilise"));

        // verification de l'expiration
        if (vt.getExpiresAt().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(vt);
            throw new InvalidRequestException("Le token de verification a expire");
        }

        // comparaison BCrypt du token clair avec le hash stocke
        if (!passwordEncoder.matches(rawToken, vt.getTokenHash())) {
            throw new InvalidRequestException("Token de verification invalide");
        }

        // marquer le compte comme verifie
        User user = userRepository.findById(vt.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Compte", vt.getUserId()));

        if (user.isVerified()) {
            // idempotent : le compte est deja verifie
            tokenRepository.delete(vt);
            log.info("Compte #{} deja verifie (idempotent) [tokenId={}]", user.getId(), tokenId);
            return;
        }

        user.setVerified(true);
        userRepository.save(user);

        // supprimer le token (one-shot)
        tokenRepository.delete(vt);
        // publier l'evenement EmailVerified
        String correlationId = UUID.randomUUID().toString();
        log.info("Compte #{} verifie avec succes [tokenId={}, correlationId={}]", user.getId(), tokenId, correlationId);

        EmailVerifiedEvent event = new EmailVerifiedEvent();
        event.setType("EmailVerified");
        event.setEventId(UUID.randomUUID().toString());
        event.setOccurredAt(Instant.now());
        event.setUserId(String.valueOf(user.getId()));
        event.setEmail(user.getCredentials().getEmail());
        event.setCorrelationId(correlationId);
        eventPublisher.publishEmailVerified(event);
    }
}
