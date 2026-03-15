package com.bhak.notification.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

// representation de l'evenement UserRegistered consomme depuis rabbitmq
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
