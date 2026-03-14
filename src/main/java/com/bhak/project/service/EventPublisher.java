package com.bhak.project.service;

import com.bhak.project.dto.EmailVerifiedEvent;
import com.bhak.project.dto.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// publie les evenements metier dans rabbitmq
@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.mq.exchange}")
    private String exchange;

    @Value("${app.mq.rk.userRegistered}")
    private String userRegisteredRk;

    @Value("${app.mq.rk.emailVerified}")
    private String emailVerifiedRk;

    // publie l'evenement UserRegistered sur l'echange auth.events
    public void publishUserRegistered(UserRegisteredEvent event) {
        log.info("Publication UserRegistered [eventId={}, userId={}, correlationId={}]",
                event.getEventId(), event.getUserId(), event.getCorrelationId());
        rabbitTemplate.convertAndSend(exchange, userRegisteredRk, event, message -> {
            message.getMessageProperties().setHeader("x-correlation-id", event.getCorrelationId());
            message.getMessageProperties().setHeader("x-schema-version", 1);
            return message;
        });
    }

    // publie l'evenement EmailVerified sur l'echange auth.events
    public void publishEmailVerified(EmailVerifiedEvent event) {
        log.info("Publication EmailVerified [eventId={}, userId={}, correlationId={}]",
                event.getEventId(), event.getUserId(), event.getCorrelationId());
        rabbitTemplate.convertAndSend(exchange, emailVerifiedRk, event, message -> {
            message.getMessageProperties().setHeader("x-correlation-id", event.getCorrelationId());
            message.getMessageProperties().setHeader("x-schema-version", 1);
            return message;
        });
    }
}
