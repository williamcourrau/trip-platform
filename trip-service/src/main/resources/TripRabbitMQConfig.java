package com.trip.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.trip.config.RabbitMQConfig.*;

@Configuration
public class TripRabbitMQConfig {

    @Bean
    public TopicExchange tripExchange() {
        return new TopicExchange(TRIP_EXCHANGE);
    }

    @Bean
    public Queue driverTripResponseQueue() {
        return new Queue(QUEUE_DRIVER_TRIP_RESPONSE, true);
    }

    @Bean
    public Queue paymentSuccessTripUpdateQueue() {
        return new Queue(QUEUE_PAYMENT_SUCCESS_TRIP_UPDATE, true);
    }

    @Bean
    public Binding driverTripResponseBinding(TopicExchange tripExchange, Queue driverTripResponseQueue) {
        return BindingBuilder.bind(driverTripResponseQueue)
            .to(tripExchange)
            .with("driver.cmd.*");
    }

    @Bean
    public Binding paymentSuccessTripUpdateBinding(TopicExchange tripExchange, Queue paymentSuccessTripUpdateQueue) {
        return BindingBuilder.bind(paymentSuccessTripUpdateQueue)
            .to(tripExchange)
            .with("payment.event.success");
    }
}
