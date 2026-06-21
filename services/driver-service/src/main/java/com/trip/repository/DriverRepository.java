package com.trip.repository;

import com.trip.model.Driver;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface DriverRepository extends MongoRepository<Driver, String> {

    List<Driver> findByAvailableTrue();

    @Query("{ 'available': true, " +
           "'location': { " +
           "  $nearSphere: { " +
           "    $geometry: { type: 'Point', coordinates: [?0, ?1] }, " +
           "    $maxDistance: ?2 " +
           "  } " +
           "}}")
    List<Driver> findAvailableDriversNear(double lng, double lat, int maxDistance);
}
