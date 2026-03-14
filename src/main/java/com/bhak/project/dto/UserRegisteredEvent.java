package com.bhak.project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

// evenement publie dans rabbitmq apres une inscription
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
