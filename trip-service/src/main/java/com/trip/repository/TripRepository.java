package com.trip.repository;

import com.trip.model.Trip;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface TripRepository extends MongoRepository<Trip, String> {

    List<Trip> findByUserId(String userId);

    List<Trip> findByDriverId(String driverId);

    List<Trip> findByStatus(Trip.TripStatus status);
}
