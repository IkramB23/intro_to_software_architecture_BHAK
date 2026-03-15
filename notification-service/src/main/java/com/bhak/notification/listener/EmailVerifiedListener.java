package com.bhak.notification.listener;

import com.bhak.notification.dto.EmailVerifiedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.util.concurrent.atomic.AtomicLong;

// consumer analytics minimal : compte le nombre de verifications d'email
@Component
@Slf4j
public class EmailVerifiedListener {

    private final AtomicLong verifiedCount = new AtomicLong(0);

    @RabbitListener(queues = "analytics.email-verified")
    public void onEmailVerified(EmailVerifiedEvent event) {
        long count = verifiedCount.incrementAndGet();
        log.info("[Analytics] EmailVerified recu [eventId={}, userId={}, email={}, correlationId={}] — Total verifications: {}",
                event.getEventId(), event.getUserId(), event.getEmail(), event.getCorrelationId(), count);
    }

    public long getVerifiedCount() {
        return verifiedCount.get();
    }
}
