package com.bhak.notification.listener;

import com.bhak.notification.dto.UserRegisteredEvent;
import com.bhak.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ consumer for {@code UserRegistered} events.
 *
 * <h2>Purpose</h2>
 * Listens to the {@code notification.user-registered} queue and, for each received
 * message, delegates sending the verification email to {@link com.bhak.notification.service.EmailService}.
 *
 * <h2>How it works</h2>
 * <ol>
 *   <li>The JSON message is automatically deserialized into a
 *       {@link com.bhak.notification.dto.UserRegisteredEvent} thanks to the
 *       {@code Jackson2JsonMessageConverter} configured in {@link com.bhak.notification.config.RabbitMQConfig}.</li>
 *   <li>The listener calls {@code emailService.sendVerificationEmail(event)}.</li>
 *   <li>If an exception is thrown during processing, RabbitMQ re-routes
 *       the message to the Dead Letter Queue ({@code notification.user-registered.dlq})
 *       to prevent message loss.</li>
 * </ol>
 *
 * <h2>Technologies</h2>
 * {@code @RabbitListener}, Spring AMQP, Lombok ({@code @RequiredArgsConstructor}, {@code @Slf4j}).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegisteredListener {

    private final EmailService emailService;

    @RabbitListener(queues = "notification.user-registered")
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Evenement recu: UserRegistered [eventId={}, userId={}, correlationId={}]",
                event.getEventId(), event.getUserId(), event.getCorrelationId());

        emailService.sendVerificationEmail(event);

        log.info("Traitement termine pour eventId={}", event.getEventId());
    }
}
