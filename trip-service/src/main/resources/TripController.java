package com.trip.controller;

import com.trip.dto.DriverTripResponse;
import com.trip.dto.PaymentStatusEvent;
import com.trip.model.Trip;
import com.trip.service.TripService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping
    public ResponseEntity<Trip> createTrip(@RequestBody Map<String, Object> request) {
        Trip trip = tripService.createTrip(
            (double) request.get("pickupLat"),
            (double) request.get("pickupLng"),
            (double) request.get("dropoffLat"),
            (double) request.get("dropoffLng"),
            (String) request.get("pickupAddress"),
            (String) request.get("dropoffAddress"),
            (String) request.get("userId")
        );
        return ResponseEntity.ok(trip);
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<Trip> getTrip(@PathVariable String tripId) {
        return ResponseEntity.ok(tripService.getTrip(tripId));
    }

    @PostMapping("/{tripId}/cancel")
    public ResponseEntity<Void> cancelTrip(@PathVariable String tripId) {
        tripService.cancelTrip(tripId);
        return ResponseEntity.ok().build();
    }
}
