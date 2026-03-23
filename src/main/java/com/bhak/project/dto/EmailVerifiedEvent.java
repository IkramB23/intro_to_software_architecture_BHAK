package com.bhak.project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Business event published to RabbitMQ after successful email address verification.
 *
 * <h2>Purpose</h2>
 * Signals to other services (analytics, notifications) that a user has validated
 * their email address. Consumed by the {@code analytics.email-verified} queue
 * in the notification-service.
 *
 * <h2>How it works</h2>
 * Published by {@link com.bhak.project.service.EventPublisher#publishEmailVerified(EmailVerifiedEvent)}
 * on the {@code auth.events} exchange with routing key {@code auth.email-verified}.
 *
 * <h2>Fields</h2>
 * <ul>
 *   <li>{@code type} – always {@code "EmailVerified"}.</li>
 *   <li>{@code eventId}, {@code occurredAt}, {@code correlationId} – tracing metadata.</li>
 *   <li>{@code userId}, {@code email} – identifier and address of the verified account.</li>
 * </ul>
 *
 * <h2>Technologies</h2>
 * {@code Serializable}, Jackson (JSON), Lombok ({@code @Data}).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerifiedEvent implements Serializable {

    private String type = "EmailVerified";
    private String eventId;
    private Instant occurredAt;
    private String userId;
    private String email;
    private String correlationId;
}
