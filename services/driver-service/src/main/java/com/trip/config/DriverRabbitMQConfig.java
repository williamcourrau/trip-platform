package com.trip.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.trip.config.RabbitMQConfig.*;

@Configuration
public class DriverRabbitMQConfig {

    @Bean
    public Queue findAvailableDriversQueue() {
        return new Queue(QUEUE_FIND_AVAILABLE_DRIVERS, true);
    }

    @Bean
    public Queue driverCmdTripRequestQueue() {
        return new Queue(QUEUE_DRIVER_CMD_TRIP_REQUEST, true);
    }

    @Bean
    public Binding findAvailableDriversBinding(TopicExchange tripExchange, Queue findAvailableDriversQueue) {
        return BindingBuilder.bind(findAvailableDriversQueue)
            .to(tripExchange)
            .with("trip.event.created");
    }

    @Bean
    public Binding driverCmdTripRequestBinding(TopicExchange tripExchange, Queue driverCmdTripRequestQueue) {
        return BindingBuilder.bind(driverCmdTripRequestQueue)
            .to(tripExchange)
            .with("driver.cmd.trip_request");
    }
}
