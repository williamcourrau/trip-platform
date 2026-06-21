package com.trip.dto;

public class DriverAssignmentEvent {
    private String tripId;
    private String driverId;
    private String driverName;
    private String driverPhone;
    private String status;

    public DriverAssignmentEvent() {}

    public DriverAssignmentEvent(String tripId, String driverId, String driverName, String status) {
        this.tripId = tripId;
        this.driverId = driverId;
        this.driverName = driverName;
        this.status = status;
    }

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }
    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public String getDriverPhone() { return driverPhone; }
    public void setDriverPhone(String driverPhone) { this.driverPhone = driverPhone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
