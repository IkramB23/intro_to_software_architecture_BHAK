package com.bhak.project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Business event published to RabbitMQ after a successful registration.
 *
 * <h2>Purpose</h2>
 * Carries the information needed by the notification-service to send
 * the verification email: user identifier, email address, tokenId
 * (short identifier), and tokenClear (cleartext token to include in the link).
 *
 * <h2>How it works</h2>
 * Published by {@link com.bhak.project.service.EventPublisher#publishUserRegistered(UserRegisteredEvent)}
 * on the {@code auth.events} exchange with routing key {@code auth.user-registered}.
 * The notification-service consumes this event from the queue
 * {@code notification.user-registered}.
 *
 * <h2>Notable fields</h2>
 * <ul>
 *   <li>{@code eventId} – unique event identifier (UUID).</li>
 *   <li>{@code correlationId} – correlation identifier for distributed tracing.</li>
 *   <li>{@code occurredAt} – event timestamp ({@code Instant}).</li>
 * </ul>
 *
 * <h2>Technologies</h2>
 * Implements {@code Serializable} for Jackson (JSON) serialisation,
 * Lombok ({@code @Data}, {@code @NoArgsConstructor}, {@code @AllArgsConstructor}).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent implements Serializable {

    private String type = "UserRegistered";
    private String eventId;
    private Instant occurredAt;
    private String userId;
    private String email;
    private String tokenId;
    private String tokenClear;
    private String correlationId;
}
