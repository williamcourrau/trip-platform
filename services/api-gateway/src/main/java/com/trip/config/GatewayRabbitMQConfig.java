package com.trip.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.trip.config.RabbitMQConfig.*;

@Configuration
public class GatewayRabbitMQConfig {

    @Bean
    public TopicExchange tripExchange() {
        return new TopicExchange(TRIP_EXCHANGE);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public Queue notifyNewTripQueue() {
        return new Queue(QUEUE_NOTIFY_NEW_TRIP, true);
    }

    @Bean
    public Queue notifyDriverAssignmentQueue() {
        return new Queue(QUEUE_NOTIFY_DRIVER_ASSIGNMENT, true);
    }

    @Bean
    public Queue notifyNoDriversFoundQueue() {
        return new Queue(QUEUE_NOTIFY_NO_DRIVERS_FOUND, true);
    }

    @Bean
    public Queue notifyPaymentStatusQueue() {
        return new Queue(QUEUE_NOTIFY_PAYMENT_STATUS, true);
    }

    @Bean
    public Binding notifyNewTripBinding(TopicExchange tripExchange, Queue notifyNewTripQueue) {
        return BindingBuilder.bind(notifyNewTripQueue)
            .to(tripExchange)
            .with("trip.event.created");
    }

    @Bean
    public Binding notifyDriverAssignmentBinding(TopicExchange tripExchange, Queue notifyDriverAssignmentQueue) {
        return BindingBuilder.bind(notifyDriverAssignmentQueue)
            .to(tripExchange)
            .with("trip.event.driver_assigned");
    }

    @Bean
    public Binding notifyNoDriversFoundBinding(TopicExchange tripExchange, Queue notifyNoDriversFoundQueue) {
        return BindingBuilder.bind(notifyNoDriversFoundQueue)
            .to(tripExchange)
            .with("trip.event.no_drivers_found");
    }

    @Bean
    public Binding notifyPaymentStatusBinding(TopicExchange paymentExchange, Queue notifyPaymentStatusQueue) {
        return BindingBuilder.bind(notifyPaymentStatusQueue)
            .to(paymentExchange)
            .with("payment.event.*");
    }
}
