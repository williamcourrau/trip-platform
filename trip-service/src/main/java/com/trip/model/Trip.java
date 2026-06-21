package com.trip.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "trips")
public class Trip {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String driverId;

    @GeoSpatialIndexed(type = GeoSpatialIndexed.Type.GEO_2DSPHERE)
    private GeoJsonPoint pickup;

    @GeoSpatialIndexed(type = GeoSpatialIndexed.Type.GEO_2DSPHERE)
    private GeoJsonPoint dropoff;

    private String pickupAddress;
    private String dropoffAddress;

    @Indexed
    private TripStatus status;

    private double estimatedDistance;
    private double estimatedDuration;
    private double fare;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum TripStatus {
        PENDING, DRIVER_ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED
    }

    public Trip() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }
    public GeoJsonPoint getPickup() { return pickup; }
    public void setPickup(GeoJsonPoint pickup) { this.pickup = pickup; }
    public GeoJsonPoint getDropoff() { return dropoff; }
    public void setDropoff(GeoJsonPoint dropoff) { this.dropoff = dropoff; }
    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }
    public String getDropoffAddress() { return dropoffAddress; }
    public void setDropoffAddress(String dropoffAddress) { this.dropoffAddress = dropoffAddress; }
    public TripStatus getStatus() { return status; }
    public void setStatus(TripStatus status) { this.status = status; }
    public double getEstimatedDistance() { return estimatedDistance; }
    public void setEstimatedDistance(double estimatedDistance) { this.estimatedDistance = estimatedDistance; }
    public double getEstimatedDuration() { return estimatedDuration; }
    public void setEstimatedDuration(double estimatedDuration) { this.estimatedDuration = estimatedDuration; }
    public double getFare() { return fare; }
    public void setFare(double fare) { this.fare = fare; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
