package com.trip.controller;

import com.trip.model.Driver;
import com.trip.repository.DriverRepository;
import com.trip.service.DriverService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverRepository driverRepository;
    private final DriverService driverService;

    public DriverController(DriverRepository driverRepository, DriverService driverService) {
        this.driverRepository = driverRepository;
        this.driverService = driverService;
    }

    @GetMapping
    public ResponseEntity<List<Driver>> getAllDrivers() {
        return ResponseEntity.ok(driverRepository.findAll());
    }

    @GetMapping("/available")
    public ResponseEntity<List<Driver>> getAvailableDrivers() {
        return ResponseEntity.ok(driverRepository.findByAvailableTrue());
    }

    @PostMapping
    public ResponseEntity<Driver> createDriver(@RequestBody Driver driver) {
        return ResponseEntity.ok(driverRepository.save(driver));
    }

    @PostMapping("/{driverId}/respond")
    public ResponseEntity<Void> respondToTrip(@PathVariable String driverId, @RequestBody Map<String, String> request) {
        driverService.handleDriverResponse(
            request.get("tripId"),
            driverId,
            request.get("action")
        );
        return ResponseEntity.ok().build();
    }
}
