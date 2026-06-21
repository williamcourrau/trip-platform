package com.trip.event;

public enum PaymentEvent {
    SESSION_CREATED("payment.event.session_created"),
    SUCCESS("payment.event.success"),
    FAILED("payment.event.failed");

    private final String routingKey;

    PaymentEvent(String routingKey) {
        this.routingKey = routingKey;
    }

    public String getRoutingKey() {
        return routingKey;
    }
}
