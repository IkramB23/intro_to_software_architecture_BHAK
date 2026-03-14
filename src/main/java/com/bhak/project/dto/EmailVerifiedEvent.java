package com.bhak.project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

// evenement publie dans rabbitmq apres verification reussie de l'email
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
