package com.bhak.notification.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO representing the {@code UserRegistered} event consumed from RabbitMQ.
 *
 * <h2>Purpose</h2>
 * Notification-service copy of the event published by the auth-service.
 * Automatically deserialized by Jackson from the JSON message in the
 * {@code notification.user-registered} queue.
 *
 * <h2>Fields</h2>
 * <ul>
 *   <li>{@code type} – event type ({@code "UserRegistered"}).</li>
 *   <li>{@code eventId}, {@code occurredAt}, {@code correlationId} – tracing metadata.</li>
 *   <li>{@code userId}, {@code email} – registered account data.</li>
 *   <li>{@code tokenId}, {@code tokenClear} – required to build the verification link.</li>
 * </ul>
 *
 * <h2>Technologies</h2>
 * {@code Serializable} (for AMQP/Jackson deserialization), Lombok ({@code @Data}).
 */
@Data
@NoArgsConstructor
public class UserRegisteredEvent implements Serializable {

    private String type;
    private String eventId;
    private Instant occurredAt;
    private String userId;
    private String email;
    private String tokenId;
    private String tokenClear;
    private String correlationId;
}
