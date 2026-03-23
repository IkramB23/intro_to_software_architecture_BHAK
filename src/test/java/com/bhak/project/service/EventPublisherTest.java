package com.bhak.project.service;

import com.bhak.project.dto.EmailVerifiedEvent;
import com.bhak.project.dto.UserRegisteredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link EventPublisher} service (RabbitMQ publishing).
 *
 * <h2>Purpose</h2>
 * Verifies that business events ({@code UserRegistered}, {@code EmailVerified})
 * are correctly sent via the {@code RabbitTemplate} with the right exchange,
 * routing key, and custom headers ({@code x-correlation-id},
 * {@code x-schema-version}).
 *
 * <h2>Approach</h2>
 * <ul>
 *   <li>The {@code RabbitTemplate} is mocked → no RabbitMQ broker is needed.</li>
 *   <li>Properties injected via {@code @Value} are set using
 *       {@code ReflectionTestUtils} to bypass the absence of a Spring context.</li>
 *   <li>An {@code ArgumentCaptor<MessagePostProcessor>} captures the post-processor
 *       to verify the headers added to the message.</li>
 * </ul>
 *
 * <h2>Tested scenarios</h2>
 * <ul>
 *   <li>publishUserRegistered sends to the correct exchange and routing key.</li>
 *   <li>publishUserRegistered adds {@code x-correlation-id} and {@code x-schema-version} headers.</li>
 *   <li>publishEmailVerified sends to the correct exchange and routing key.</li>
 * </ul>
 *
 * <h2>Technologies</h2>
 * JUnit 5, Mockito, Spring {@code ReflectionTestUtils}, AssertJ.
 */
@ExtendWith(MockitoExtension.class)
class EventPublisherTest {

    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks private EventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(eventPublisher, "exchange", "auth.events");
        ReflectionTestUtils.setField(eventPublisher, "userRegisteredRk", "auth.user-registered");
        ReflectionTestUtils.setField(eventPublisher, "emailVerifiedRk", "auth.email-verified");
    }

    @Test
    void publishUserRegistered_sendsToCorrectExchangeAndRoutingKey() {
        UserRegisteredEvent event = new UserRegisteredEvent();
        event.setType("UserRegistered");
        event.setEventId("evt-1");
        event.setOccurredAt(Instant.now());
        event.setUserId("1");
        event.setEmail("a@b.com");
        event.setTokenId("tok_123");
        event.setTokenClear("clear");
        event.setCorrelationId("corr-1");

        eventPublisher.publishUserRegistered(event);

        verify(rabbitTemplate).convertAndSend(
                eq("auth.events"),
                eq("auth.user-registered"),
                eq(event),
                any(MessagePostProcessor.class));
    }

    @Test
    void publishUserRegistered_setsHeaders() {
        UserRegisteredEvent event = new UserRegisteredEvent();
        event.setEventId("evt-1");
        event.setUserId("1");
        event.setCorrelationId("corr-1");

        // capture le MessagePostProcessor
        ArgumentCaptor<MessagePostProcessor> mppCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);

        eventPublisher.publishUserRegistered(event);

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), eq(event), mppCaptor.capture());

        // simuler l'appel du postProcessor pour verifier les headers
        MessageProperties props = new MessageProperties();
        Message message = new Message(new byte[0], props);
        mppCaptor.getValue().postProcessMessage(message);

        assertThat((String) props.getHeader("x-correlation-id")).isEqualTo("corr-1");
        assertThat((int) props.getHeader("x-schema-version")).isEqualTo(1);
    }

    @Test
    void publishEmailVerified_sendsToCorrectExchangeAndRoutingKey() {
        EmailVerifiedEvent event = new EmailVerifiedEvent();
        event.setType("EmailVerified");
        event.setEventId("evt-2");
        event.setOccurredAt(Instant.now());
        event.setUserId("1");
        event.setEmail("a@b.com");
        event.setCorrelationId("corr-2");

        eventPublisher.publishEmailVerified(event);

        verify(rabbitTemplate).convertAndSend(
                eq("auth.events"),
                eq("auth.email-verified"),
                eq(event),
                any(MessagePostProcessor.class));
    }

    @Test
    void publishEmailVerified_setsHeaders() {
        EmailVerifiedEvent event = new EmailVerifiedEvent();
        event.setEventId("evt-2");
        event.setUserId("1");
        event.setCorrelationId("corr-2");

        ArgumentCaptor<MessagePostProcessor> mppCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);

        eventPublisher.publishEmailVerified(event);

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), eq(event), mppCaptor.capture());

        MessageProperties props = new MessageProperties();
        Message message = new Message(new byte[0], props);
        mppCaptor.getValue().postProcessMessage(message);

        assertThat((String) props.getHeader("x-correlation-id")).isEqualTo("corr-2");
        assertThat((int) props.getHeader("x-schema-version")).isEqualTo(1);
    }
}
