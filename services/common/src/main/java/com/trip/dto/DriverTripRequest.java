package com.trip.dto;

public class DriverTripRequest {
    private String tripId;
    private String driverId;
    private double pickupLat;
    private double pickupLng;
    private double dropoffLat;
    private double dropoffLng;
    private String pickupAddress;
    private String dropoffAddress;
    private double estimatedDistance;
    private double estimatedDuration;

    public DriverTripRequest() {}

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }
    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }
    public double getPickupLat() { return pickupLat; }
    public void setPickupLat(double pickupLat) { this.pickupLat = pickupLat; }
    public double getPickupLng() { return pickupLng; }
    public void setPickupLng(double pickupLng) { this.pickupLng = pickupLng; }
    public double getDropoffLat() { return dropoffLat; }
    public void setDropoffLat(double dropoffLat) { this.dropoffLat = dropoffLat; }
    public double getDropoffLng() { return dropoffLng; }
    public void setDropoffLng(double dropoffLng) { this.dropoffLng = dropoffLng; }
    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }
    public String getDropoffAddress() { return dropoffAddress; }
    public void setDropoffAddress(String dropoffAddress) { this.dropoffAddress = dropoffAddress; }
    public double getEstimatedDistance() { return estimatedDistance; }
    public void setEstimatedDistance(double estimatedDistance) { this.estimatedDistance = estimatedDistance; }
    public double getEstimatedDuration() { return estimatedDuration; }
    public void setEstimatedDuration(double estimatedDuration) { this.estimatedDuration = estimatedDuration; }
}
