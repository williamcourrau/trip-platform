package com.trip.dto;

public class TripCreatedEvent {
    private String tripId;
    private String userId;
    private double pickupLat;
    private double pickupLng;
    private double dropoffLat;
    private double dropoffLng;
    private String pickupAddress;
    private String dropoffAddress;

    public TripCreatedEvent() {}

    public TripCreatedEvent(String tripId, String userId, double pickupLat, double pickupLng,
                            double dropoffLat, double dropoffLng) {
        this.tripId = tripId;
        this.userId = userId;
        this.pickupLat = pickupLat;
        this.pickupLng = pickupLng;
        this.dropoffLat = dropoffLat;
        this.dropoffLng = dropoffLng;
    }

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
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
}
