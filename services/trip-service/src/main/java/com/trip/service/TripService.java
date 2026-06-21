package com.trip.service;

import com.trip.dto.DriverAssignmentEvent;
import com.trip.dto.TripCreatedEvent;
import com.trip.event.TripEvent;
import com.trip.model.Trip;
import com.trip.repository.TripRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.trip.config.RabbitMQConfig.TRIP_EXCHANGE;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final RabbitTemplate rabbitTemplate;

    public TripService(TripRepository tripRepository, RabbitTemplate rabbitTemplate) {
        this.tripRepository = tripRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public Trip createTrip(double pickupLat, double pickupLng,
                           double dropoffLat, double dropoffLng,
                           String pickupAddress, String dropoffAddress,
                           String userId) {
        Trip trip = new Trip();
        trip.setId(UUID.randomUUID().toString());
        trip.setUserId(userId);
        trip.setPickup(new GeoJsonPoint(pickupLng, pickupLat));
        trip.setDropoff(new GeoJsonPoint(dropoffLng, dropoffLat));
        trip.setPickupAddress(pickupAddress);
        trip.setDropoffAddress(dropoffAddress);
        trip.setStatus(Trip.TripStatus.PENDING);

        trip = tripRepository.save(trip);

        TripCreatedEvent event = new TripCreatedEvent(
            trip.getId(), trip.getUserId(),
            trip.getPickup().getY(), trip.getPickup().getX(),
            trip.getDropoff().getY(), trip.getDropoff().getX()
        );
        event.setPickupAddress(trip.getPickupAddress());
        event.setDropoffAddress(trip.getDropoffAddress());

        rabbitTemplate.convertAndSend(TRIP_EXCHANGE, TripEvent.CREATED.getRoutingKey(), event);

        return trip;
    }

    public void assignDriver(String tripId, String driverId, String driverName) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() ->
            new RuntimeException("Trip not found: " + tripId));
        trip.setDriverId(driverId);
        trip.setStatus(Trip.TripStatus.DRIVER_ASSIGNED);
        trip.setUpdatedAt(LocalDateTime.now());
        tripRepository.save(trip);

        DriverAssignmentEvent event = new DriverAssignmentEvent(tripId, driverId, driverName, "ASSIGNED");
        rabbitTemplate.convertAndSend(TRIP_EXCHANGE, TripEvent.DRIVER_ASSIGNED.getRoutingKey(), event);
    }

    public void cancelTrip(String tripId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() ->
            new RuntimeException("Trip not found: " + tripId));
        trip.setStatus(Trip.TripStatus.CANCELLED);
        trip.setUpdatedAt(LocalDateTime.now());
        tripRepository.save(trip);

        rabbitTemplate.convertAndSend(TRIP_EXCHANGE, TripEvent.CANCELLED.getRoutingKey(), tripId);
    }

    public Trip getTrip(String tripId) {
        return tripRepository.findById(tripId).orElseThrow(() ->
            new RuntimeException("Trip not found: " + tripId));
    }
}
