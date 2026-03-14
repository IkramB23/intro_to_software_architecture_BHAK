package com.bhak.project.configuration;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// configuration rabbitmq : exchange, files, DLQ et convertisseur json
@Configuration
public class RabbitMQConfig {

    @Value("${app.mq.exchange}")
    private String exchangeName;

    public static final String QUEUE_USER_REGISTERED = "notification.user-registered";
    public static final String QUEUE_ANALYTICS_VERIFIED = "analytics.email-verified";
    public static final String DLX_NAME = "auth.events.dlx";
    public static final String DLQ_USER_REGISTERED = "notification.user-registered.dlq";

    // echange principal de type topic
    @Bean
    public TopicExchange authEventsExchange() {
        return new TopicExchange(exchangeName);
    }

    // dead letter exchange
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DLX_NAME);
    }

    // file principale avec redirection vers la DLX en cas d'echec
    @Bean
    public Queue userRegisteredQueue() {
        return QueueBuilder.durable(QUEUE_USER_REGISTERED)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", "dlq." + QUEUE_USER_REGISTERED)
                .build();
    }

    // dead letter queue
    @Bean
    public Queue userRegisteredDlq() {
        return QueueBuilder.durable(DLQ_USER_REGISTERED).build();
    }

    // binding : exchange principal -> file principale
    @Bean
    public Binding userRegisteredBinding(Queue userRegisteredQueue, TopicExchange authEventsExchange) {
        return BindingBuilder.bind(userRegisteredQueue)
                .to(authEventsExchange)
                .with("auth.user-registered");
    }

    // binding : DLX -> DLQ
    @Bean
    public Binding dlqBinding(Queue userRegisteredDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(userRegisteredDlq)
                .to(deadLetterExchange)
                .with("dlq." + QUEUE_USER_REGISTERED);
    }

    // file analytics pour les evenements EmailVerified
    @Bean
    public Queue analyticsEmailVerifiedQueue() {
        return QueueBuilder.durable(QUEUE_ANALYTICS_VERIFIED).build();
    }

    // binding : exchange principal -> file analytics
    @Bean
    public Binding analyticsEmailVerifiedBinding(Queue analyticsEmailVerifiedQueue, TopicExchange authEventsExchange) {
        return BindingBuilder.bind(analyticsEmailVerifiedQueue)
                .to(authEventsExchange)
                .with("auth.email-verified");
    }

    // serialisation/deserialisation json des messages
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
