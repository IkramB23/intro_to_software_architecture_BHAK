package com.bhak.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO representing the {@code EmailVerified} event consumed from RabbitMQ.
 *
 * <h2>Purpose</h2>
 * Notification-service copy of the event published by the auth-service after
 * successful email address verification. Consumed by
 * {@link com.bhak.notification.listener.EmailVerifiedListener} (analytics).
 *
 * <h2>Fields</h2>
 * <ul>
 *   <li>{@code type} – {@code "EmailVerified"}.</li>
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

    private String type;
    private String eventId;
    private Instant occurredAt;
    private String userId;
    private String email;
    private String correlationId;
}
