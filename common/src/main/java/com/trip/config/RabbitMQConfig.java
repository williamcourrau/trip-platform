package com.trip.config;

public class RabbitMQConfig {
    public static final String TRIP_EXCHANGE = "trip.exchange";
    public static final String PAYMENT_EXCHANGE = "payment.exchange";

    public static final String QUEUE_FIND_AVAILABLE_DRIVERS = "find_available_drivers";
    public static final String QUEUE_NOTIFY_NEW_TRIP = "notify_new_trip";
    public static final String QUEUE_NOTIFY_DRIVER_ASSIGNMENT = "notify_driver_assignment";
    public static final String QUEUE_NOTIFY_NO_DRIVERS_FOUND = "notify_driver_no_drivers_found";
    public static final String QUEUE_DRIVER_CMD_TRIP_REQUEST = "driver_cmd_trip_request";
    public static final String QUEUE_DRIVER_TRIP_RESPONSE = "driver_trip_response";
    public static final String QUEUE_CREATE_PAYMENT_SESSION = "create_payment_session";
    public static final String QUEUE_NOTIFY_PAYMENT_STATUS = "notify_payment_status";
    public static final String QUEUE_PAYMENT_SUCCESS_TRIP_UPDATE = "payment_success_trip_update";
}
