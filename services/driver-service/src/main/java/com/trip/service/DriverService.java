package com.trip.service;

import com.trip.dto.DriverTripRequest;
import com.trip.dto.DriverTripResponse;
import com.trip.dto.TripCreatedEvent;
import com.trip.event.DriverCommand;
import com.trip.event.TripEvent;
import com.trip.model.Driver;
import com.trip.repository.DriverRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.trip.config.RabbitMQConfig.*;

@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final RabbitTemplate rabbitTemplate;

    public DriverService(DriverRepository driverRepository, RabbitTemplate rabbitTemplate) {
        this.driverRepository = driverRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = QUEUE_FIND_AVAILABLE_DRIVERS)
    public void handleFindAvailableDrivers(TripCreatedEvent event) {
        List<Driver> availableDrivers = driverRepository.findByAvailableTrue();

        if (availableDrivers.isEmpty()) {
            rabbitTemplate.convertAndSend(TRIP_EXCHANGE, TripEvent.NO_DRIVERS_FOUND.getRoutingKey(), event.getTripId());
            return;
        }

        for (Driver driver : availableDrivers) {
            DriverTripRequest request = new DriverTripRequest();
            request.setTripId(event.getTripId());
            request.setDriverId(driver.getId());
            request.setPickupLat(event.getPickupLat());
            request.setPickupLng(event.getPickupLng());
            request.setDropoffLat(event.getDropoffLat());
            request.setDropoffLng(event.getDropoffLng());
            request.setPickupAddress(event.getPickupAddress());
            request.setDropoffAddress(event.getDropoffAddress());

            rabbitTemplate.convertAndSend(TRIP_EXCHANGE, DriverCommand.TRIP_REQUEST.getRoutingKey(), request);
        }
    }

    @RabbitListener(queues = QUEUE_DRIVER_CMD_TRIP_REQUEST)
    public void handleDriverTripRequest(DriverTripRequest request) {
        rabbitTemplate.convertAndSend(TRIP_EXCHANGE, DriverCommand.TRIP_REQUEST.getRoutingKey(), request);
    }

    public void handleDriverResponse(String tripId, String driverId, String action) {
        DriverTripResponse response = new DriverTripResponse(tripId, driverId, action);
        rabbitTemplate.convertAndSend(TRIP_EXCHANGE, "driver.cmd." + action.toLowerCase(), response);
    }
}
