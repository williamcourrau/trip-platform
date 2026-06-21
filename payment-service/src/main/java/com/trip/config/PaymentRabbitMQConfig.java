package com.trip.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.trip.config.RabbitMQConfig.*;

@Configuration
public class PaymentRabbitMQConfig {

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public Queue createPaymentSessionQueue() {
        return new Queue(QUEUE_CREATE_PAYMENT_SESSION, true);
    }

    @Bean
    public Queue notifyPaymentStatusQueue() {
        return new Queue(QUEUE_NOTIFY_PAYMENT_STATUS, true);
    }

    @Bean
    public Binding createPaymentSessionBinding(TopicExchange tripExchange, Queue createPaymentSessionQueue) {
        return BindingBuilder.bind(createPaymentSessionQueue)
            .to(tripExchange)
            .with("trip.event.driver_assigned");
    }

    @Bean
    public Binding notifyPaymentStatusBinding(TopicExchange paymentExchange, Queue notifyPaymentStatusQueue) {
        return BindingBuilder.bind(notifyPaymentStatusQueue)
            .to(paymentExchange)
            .with("payment.event.*");
    }
}
