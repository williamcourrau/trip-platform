package com.trip.event;

public enum DriverCommand {
    TRIP_REQUEST("driver.cmd.trip_request"),
    TRIP_ACCEPT("driver.cmd.trip_accept"),
    TRIP_DECLINE("driver.cmd.trip_decline");

    private final String routingKey;

    DriverCommand(String routingKey) {
        this.routingKey = routingKey;
    }

    public String getRoutingKey() {
        return routingKey;
    }
}
