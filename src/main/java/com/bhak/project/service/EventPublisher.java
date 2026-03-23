package com.bhak.project.service;

import com.bhak.project.dto.EmailVerifiedEvent;
import com.bhak.project.dto.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for publishing business events to RabbitMQ.
 *
 * <h2>Purpose</h2>
 * Serves as the single exit point to the message broker. It publishes two
 * types of events on the {@code auth.events} exchange (<em>topic</em> type):
 * <ul>
 *   <li>{@code UserRegistered} (routing key {@code auth.user-registered}) – emitted after
 *       a registration to trigger a verification email.</li>
 *   <li>{@code EmailVerified} (routing key {@code auth.email-verified}) – emitted after
 *       verification link validation to feed analytics consumers.</li>
 * </ul>
 *
 * <h2>How it works</h2>
 * Uses the {@link org.springframework.amqp.rabbit.core.RabbitTemplate} configured
 * with a {@code Jackson2JsonMessageConverter} to send objects as JSON.
 * Each message is enriched with custom headers ({@code x-correlation-id},
 * {@code x-schema-version}) via a {@code MessagePostProcessor}, facilitating
 * distributed tracing and schema versioning.
 *
 * <h2>Technologies</h2>
 * Spring AMQP ({@code RabbitTemplate}), {@code @Value} for injecting
 * exchange and routing key names, Lombok ({@code @RequiredArgsConstructor}, {@code @Slf4j}).
 */
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
