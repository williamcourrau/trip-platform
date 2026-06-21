package com.trip.messaging;

import com.trip.dto.DriverTripResponse;
import com.trip.dto.PaymentStatusEvent;
import com.trip.service.TripService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.trip.config.RabbitMQConfig.QUEUE_DRIVER_TRIP_RESPONSE;
import static com.trip.config.RabbitMQConfig.QUEUE_PAYMENT_SUCCESS_TRIP_UPDATE;

@Component
public class TripEventConsumer {

    private final TripService tripService;

    public TripEventConsumer(TripService tripService) {
        this.tripService = tripService;
    }

    @RabbitListener(queues = QUEUE_DRIVER_TRIP_RESPONSE)
    public void handleDriverResponse(DriverTripResponse response) {
        if ("ACCEPT".equalsIgnoreCase(response.getAction())) {
            tripService.assignDriver(response.getTripId(), response.getDriverId(), response.getDriverId());
        }
    }

    @RabbitListener(queues = QUEUE_PAYMENT_SUCCESS_TRIP_UPDATE)
    public void handlePaymentSuccess(PaymentStatusEvent event) {
        tripService.getTrip(event.getTripId());
    }
}
