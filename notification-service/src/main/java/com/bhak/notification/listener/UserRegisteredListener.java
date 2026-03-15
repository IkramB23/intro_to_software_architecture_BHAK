package com.bhak.notification.listener;

import com.bhak.notification.dto.UserRegisteredEvent;
import com.bhak.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

// consomme les evenements UserRegistered depuis la file rabbitmq
// en cas d'exception, le message sera redirige vers la DLQ
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
