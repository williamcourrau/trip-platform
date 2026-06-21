package com.trip.dto;

public class PaymentSessionEvent {
    private String tripId;
    private String sessionId;
    private String paymentUrl;
    private double amount;

    public PaymentSessionEvent() {}

    public PaymentSessionEvent(String tripId, String sessionId, String paymentUrl, double amount) {
        this.tripId = tripId;
        this.sessionId = sessionId;
        this.paymentUrl = paymentUrl;
        this.amount = amount;
    }

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}
