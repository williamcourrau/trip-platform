package com.trip.dto;

public class DriverTripResponse {
    private String tripId;
    private String driverId;
    private String action;

    public DriverTripResponse() {}

    public DriverTripResponse(String tripId, String driverId, String action) {
        this.tripId = tripId;
        this.driverId = driverId;
        this.action = action;
    }

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }
    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
