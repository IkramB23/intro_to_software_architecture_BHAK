package com.bhak.notification.service;

import com.bhak.notification.dto.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Verification email sending service.
 *
 * <h2>Purpose</h2>
 * Builds and sends an email containing the verification link
 * to the new user, via the configured SMTP server (MailHog in development).
 *
 * <h2>How it works</h2>
 * <ol>
 *   <li>Receives a {@link com.bhak.notification.dto.UserRegisteredEvent} containing
 *       the email address, {@code tokenId}, and {@code tokenClear}.</li>
 *   <li>Builds the verification link: {@code <baseUrl>/api/auth/verify?tokenId=...&t=...}.
 *       The {@code baseUrl} is injected via {@code @Value("${app.auth.base-url}")}.</li>
 *   <li>Creates a {@link org.springframework.mail.SimpleMailMessage} (plain text email)
 *       and sends it via {@link org.springframework.mail.javamail.JavaMailSender}.</li>
 * </ol>
 *
 * <h2>MailHog</h2>
 * In the Docker environment, emails are captured by MailHog (test SMTP server)
 * and viewable via its web interface (port 8025 by default).
 * No emails are actually sent externally.
 *
 * <h2>Technologies</h2>
 * Spring Mail ({@code JavaMailSender}, {@code SimpleMailMessage}), {@code @Value},
 * Lombok ({@code @RequiredArgsConstructor}, {@code @Slf4j}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.auth.base-url}")
    private String authBaseUrl;

    public void sendVerificationEmail(UserRegisteredEvent event) {
        String verifyLink = authBaseUrl + "/api/auth/verify?tokenId="
                + event.getTokenId() + "&t=" + event.getTokenClear();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@bhak.com");
        message.setTo(event.getEmail());
        message.setSubject("Vérifiez votre adresse e-mail");
        message.setText("Bonjour,\n\n"
                + "Veuillez cliquer sur le lien suivant pour vérifier votre compte :\n\n"
                + verifyLink + "\n\n"
                + "Ce lien est valable 30 minutes.\n\n"
                + "— L'équipe BHAK");

        mailSender.send(message);
        log.info("E-mail de verification envoye a {} [correlationId={}]",
                event.getEmail(), event.getCorrelationId());
    }
}
