package com.bhak.notification.service;

import com.bhak.notification.dto.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

// construit et envoie l'e-mail de verification via MailHog
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
