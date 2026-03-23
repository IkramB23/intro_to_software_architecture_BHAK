package com.bhak.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the notification microservice.
 *
 * <h2>Purpose</h2>
 * This standalone microservice consumes events published by the main service
 * (auth-service) via RabbitMQ and performs the corresponding actions:
 * <ul>
 *   <li>{@code UserRegistered} → sends a verification email via MailHog.</li>
 *   <li>{@code EmailVerified} → analytics counter (minimal consumer).</li>
 * </ul>
 *
 * <h2>Architecture</h2>
 * <em>Asynchronous</em> communication with the main service: the notification-service
 * has no direct dependency on the auth-service. It only connects to
 * RabbitMQ (to consume messages) and an SMTP server (MailHog) to
 * send emails.
 *
 * <h2>Technologies</h2>
 * Spring Boot 3, Spring AMQP (RabbitMQ), Spring Mail (JavaMailSender),
 * Jackson (JSON message deserialization), Lombok.
 *
 * <h2>Containerization</h2>
 * This service has its own {@code Dockerfile} and is launched by
 * {@code docker-compose} alongside RabbitMQ, PostgreSQL,
 * MailHog, and the main service.
 */
@SpringBootApplication
public class NotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}
