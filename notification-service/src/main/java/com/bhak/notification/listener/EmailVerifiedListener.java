package com.bhak.notification.listener;

import com.bhak.notification.dto.EmailVerifiedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.util.concurrent.atomic.AtomicLong;

/**
 * RabbitMQ analytics consumer for {@code EmailVerified} events.
 *
 * <h2>Purpose</h2>
 * Listens to the {@code analytics.email-verified} queue and counts the number
 * of email verifications received (minimal consumer for demonstration purposes).
 *
 * <h2>How it works</h2>
 * An atomic counter ({@link java.util.concurrent.atomic.AtomicLong})
 * is incremented for each received event. Event details
 * ({@code eventId}, {@code userId}, {@code email}, {@code correlationId})
 * are logged for traceability.
 *
 * <h2>Extensibility</h2>
 * This listener can be extended to feed a dashboard,
 * an analytics database, or trigger other post-verification processes.
 *
 * <h2>Technologies</h2>
 * {@code @RabbitListener}, {@code AtomicLong} (thread-safe), Lombok ({@code @Slf4j}).
 */
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
