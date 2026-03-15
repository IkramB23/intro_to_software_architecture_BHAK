package com.bhak.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

// evenement recu apres verification reussie de l'email
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
