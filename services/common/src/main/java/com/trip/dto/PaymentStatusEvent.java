package com.trip.dto;

public class PaymentStatusEvent {
    private String tripId;
    private String sessionId;
    private String status;

    public PaymentStatusEvent() {}

    public PaymentStatusEvent(String tripId, String sessionId, String status) {
        this.tripId = tripId;
        this.sessionId = sessionId;
        this.status = status;
    }

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
