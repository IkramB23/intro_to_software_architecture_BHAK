package com.bhak.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// configuration rabbitmq cote notification
// declare les memes files que le service auth pour que spring puisse les creer si besoin
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "auth.events";
    public static final String DLX = "auth.events.dlx";
    public static final String QUEUE = "notification.user-registered";
    public static final String QUEUE_ANALYTICS = "analytics.email-verified";
    public static final String DLQ = "notification.user-registered.dlq";

    @Bean
    public TopicExchange authEventsExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DLX);
    }

    @Bean
    public Queue userRegisteredQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", "dlq." + QUEUE)
                .build();
    }

    @Bean
    public Queue userRegisteredDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding userRegisteredBinding(Queue userRegisteredQueue, TopicExchange authEventsExchange) {
        return BindingBuilder.bind(userRegisteredQueue)
                .to(authEventsExchange)
                .with("auth.user-registered");
    }

    @Bean
    public Binding dlqBinding(Queue userRegisteredDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(userRegisteredDlq)
                .to(deadLetterExchange)
                .with("dlq." + QUEUE);
    }

    // file analytics pour les evenements EmailVerified
    @Bean
    public Queue analyticsEmailVerifiedQueue() {
        return QueueBuilder.durable(QUEUE_ANALYTICS).build();
    }

    // binding : exchange principal -> file analytics
    @Bean
    public Binding analyticsEmailVerifiedBinding(Queue analyticsEmailVerifiedQueue, TopicExchange authEventsExchange) {
        return BindingBuilder.bind(analyticsEmailVerifiedQueue)
                .to(authEventsExchange)
                .with("auth.email-verified");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
