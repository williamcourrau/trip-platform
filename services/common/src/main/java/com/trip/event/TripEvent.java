package com.trip.event;

public enum TripEvent {
    CREATED("trip.event.created"),
    DRIVER_ASSIGNED("trip.event.driver_assigned"),
    NO_DRIVERS_FOUND("trip.event.no_drivers_found"),
    CANCELLED("trip.event.cancelled");

    private final String routingKey;

    TripEvent(String routingKey) {
        this.routingKey = routingKey;
    }

    public String getRoutingKey() {
        return routingKey;
    }
}
