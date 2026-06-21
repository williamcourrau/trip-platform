# Trip Platform API Specification

## Architecture Overview

```
Client  <->  API Gateway (8080)  <->  gRPC  <->  Trip Service (8081)
                                     gRPC  <->  Driver Service (8082)
                                     gRPC  <->  Payment Service (8083)

Services communicate asynchronously via RabbitMQ (trip.exchange, payment.exchange)
```

### Communication Patterns

| Pattern | Protocol | Use Case |
|---|---|---|
| Synchronous request-response | gRPC | API Gateway to any backend service |
| Asynchronous event broadcast | RabbitMQ topic exchange | Service to service (trip created, driver assigned, payment success) |
| Real-time push | WebSocket | API Gateway to connected clients |
| External webhook | HTTP | Stripe to API Gateway |

---

## gRPC Service Definitions

These proto files live in `common/src/main/proto/` and are shared across all services.

### trip.proto

```protobuf
syntax = "proto3";

package trip;

option java_package = "com.trip.proto";
option java_multiple_files = true;

service TripService {
  rpc CreateTrip (CreateTripRequest) returns (CreateTripResponse);
  rpc GetTrip (GetTripRequest) returns (Trip);
  rpc PreviewTrip (PreviewTripRequest) returns (PreviewTripResponse);
  rpc CancelTrip (CancelTripRequest) returns (CancelTripResponse);
}

message CreateTripRequest {
  string user_id = 1;
  double pickup_lat = 2;
  double pickup_lng = 3;
  double dropoff_lat = 4;
  double dropoff_lng = 5;
  string pickup_address = 6;
  string dropoff_address = 7;
}

message CreateTripResponse {
  string trip_id = 1;
  string status = 2;
}

message GetTripRequest {
  string trip_id = 1;
}

message Trip {
  string id = 1;
  string user_id = 2;
  string driver_id = 3;
  double pickup_lat = 4;
  double pickup_lng = 5;
  double dropoff_lat = 6;
  double dropoff_lng = 7;
  string pickup_address = 8;
  string dropoff_address = 9;
  string status = 10;
  double estimated_distance = 11;
  double estimated_duration = 12;
  double fare = 13;
  string created_at = 14;
}

message PreviewTripRequest {
  double pickup_lat = 1;
  double pickup_lng = 2;
  double dropoff_lat = 3;
  double dropoff_lng = 4;
}

message PreviewTripResponse {
  double estimated_distance = 1;
  double estimated_duration = 2;
  double estimated_fare = 3;
  repeated Coordinate route = 4;
}

message Coordinate {
  double lat = 1;
  double lng = 2;
}

message CancelTripRequest {
  string trip_id = 1;
}

message CancelTripResponse {
  string status = 1;
}
```

### driver.proto

```protobuf
syntax = "proto3";

package trip;

option java_package = "com.trip.proto";
option java_multiple_files = true;

service DriverService {
  rpc GetDriver (GetDriverRequest) returns (Driver);
  rpc UpdateLocation (UpdateLocationRequest) returns (UpdateLocationResponse);
}

message GetDriverRequest {
  string driver_id = 1;
}

message Driver {
  string id = 1;
  string name = 2;
  string phone = 3;
  string email = 4;
  double current_lat = 5;
  double current_lng = 6;
  bool available = 7;
}

message UpdateLocationRequest {
  string driver_id = 1;
  double lat = 2;
  double lng = 3;
}

message UpdateLocationResponse {
  bool success = 1;
}
```

### payment.proto

```protobuf
syntax = "proto3";

package trip;

option java_package = "com.trip.proto";
option java_multiple_files = true;

service PaymentService {
  rpc GetPaymentSession (GetPaymentSessionRequest) returns (PaymentSession);
}

message GetPaymentSessionRequest {
  string trip_id = 1;
}

message PaymentSession {
  string session_id = 1;
  string payment_url = 2;
  double amount = 3;
  string status = 4;
}
```

---

## gRPC Service Ownership

| Service | gRPC Server | gRPC Client(s) |
|---|---|---|
| Trip Service | `TripService` server on port 9091 | None (API Gateway calls it) |
| Driver Service | `DriverService` server on port 9092 | None (API Gateway calls it) |
| Payment Service | `PaymentService` server on port 9093 | None (API Gateway calls it) |
| API Gateway | None | All three (`TripService`, `DriverService`, `PaymentService`) |

### gRPC Port Mapping

| Service | gRPC Port | REST Port |
|---|---|---|
| api-gateway | none | 8080 |
| trip-service | 9091 | 8081 |
| driver-service | 9092 | 8082 |
| payment-service | 9093 | 8083 |

---

## RabbitMQ Events

### Exchanges

| Exchange Name | Type | Purpose |
|---|---|---|
| `trip.exchange` | topic | Trip lifecycle events and driver commands |
| `payment.exchange` | topic | Payment lifecycle events |

### Trip Exchange Events

| Event (routing key) | Publisher | Consumer Queue | Consumer | Payload |
|---|---|---|---|---|
| `trip.event.created` | Trip Service | `find_available_drivers` | Driver Service | `TripCreatedEvent` |
| `trip.event.created` | Trip Service | `notify_new_trip` | API Gateway | `TripCreatedEvent` |
| `trip.event.driver_assigned` | Trip Service | `notify_driver_assignment` | API Gateway | `DriverAssignmentEvent` |
| `trip.event.driver_assigned` | Trip Service | `create_payment_session` | Payment Service | trip_id (string) |
| `trip.event.no_drivers_found` | Driver Service | `notify_driver_no_drivers_found` | API Gateway | trip_id (string) |
| `trip.event.cancelled` | Trip Service | (broadcast) | All | trip_id (string) |
| `driver.cmd.trip_request` | Driver Service | `driver_cmd_trip_request` | Driver Service | `DriverTripRequest` |
| `driver.cmd.trip_accept` | Driver (via client) | `driver_trip_response` | Trip Service | `DriverTripResponse` |
| `driver.cmd.trip_decline` | Driver (via client) | `driver_trip_response` | Trip Service | `DriverTripResponse` |

### Payment Exchange Events

| Event (routing key) | Publisher | Consumer Queue | Consumer | Payload |
|---|---|---|---|---|
| `payment.event.session_created` | Payment Service | `notify_payment_status` | API Gateway | `PaymentSessionEvent` |
| `payment.event.success` | Payment Service | `payment_success_trip_update` | Trip Service | `PaymentStatusEvent` |
| `payment.event.failed` | Payment Service | `payment_success_trip_update` | Trip Service | `PaymentStatusEvent` |

---

## REST Endpoints

All REST endpoints are exposed **only** through the API Gateway at `http://localhost:8080`. Backend services are not directly accessible from clients.

### Trip Endpoints (proxied to Trip Service)

| Method | Path | Request Body | Response | Description |
|---|---|---|---|---|
| POST | `/api/trips` | `CreateTripRequest` | `Trip` | Create a new trip |
| GET | `/api/trips/{tripId}` | - | `Trip` | Get trip details |
| POST | `/api/trips/{tripId}/cancel` | - | 200 OK | Cancel a trip |
| POST | `/api/trips/preview` | `PreviewTripRequest` | `PreviewTripResponse` | Preview route and fare |

### Driver Endpoints (proxied to Driver Service)

| Method | Path | Request Body | Response | Description |
|---|---|---|---|---|
| GET | `/api/drivers` | - | `List<Driver>` | List all drivers |
| GET | `/api/drivers/available` | - | `List<Driver>` | List available drivers |
| GET | `/api/drivers/{driverId}` | - | `Driver` | Get driver details |
| POST | `/api/drivers` | `Driver` | `Driver` | Register a new driver |
| PATCH | `/api/drivers/{driverId}/location` | `{ lat, lng }` | 200 OK | Update driver location |
| POST | `/api/drivers/{driverId}/respond` | `{ tripId, action }` | 200 OK | Accept or decline a trip |

### Payment Endpoints (proxied to Payment Service)

| Method | Path | Request Body | Response | Description |
|---|---|---|---|---|
| GET | `/api/payments/session/{tripId}` | - | `PaymentSession` | Get payment session for a trip |
| POST | `/api/payments/webhook/stripe` | Stripe webhook payload | 200 OK | Stripe webhook receiver |

---

## WebSocket

### Connection

```
ws://localhost:8080/ws/trips
```

### Messages Sent by Server

Events broadcasted to all connected clients:

| Event | Payload Example | Trigger |
|---|---|---|
| New trip created | `{ "tripId": "...", "userId": "...", "pickupLat": ..., "pickupLng": ..., "dropoffLat": ..., "dropoffLng": ... }` | `trip.event.created` |
| Driver assigned | `{ "tripId": "...", "driverId": "...", "status": "ASSIGNED" }` | `trip.event.driver_assigned` |
| No drivers found | `{ "type": "no_drivers_found", "tripId": "..." }` | `trip.event.no_drivers_found` |
| Payment session ready | `{ "tripId": "...", "sessionId": "...", "paymentUrl": "...", "amount": 25.0 }` | `payment.event.session_created` |

---

## gRPC to RabbitMQ Flow Sequences

### Create Trip Flow

```
1. Client               POST /api/trips                     API Gateway
2. API Gateway          gRPC CreateTrip                     Trip Service
3. Trip Service         stores trip in MongoDB               local
4. Trip Service         publishes trip.event.created          RabbitMQ (trip.exchange)
5. Driver Service       consumes from find_available_drivers  RabbitMQ
6. Driver Service       finds available drivers in MongoDB    local
7. Driver Service       publishes driver.cmd.trip_request     RabbitMQ (trip.exchange)
8. API Gateway          consumes from notify_new_trip         RabbitMQ
9. API Gateway          broadcasts via WebSocket              connected clients
```

### Driver Response Flow

```
1. Driver Client        POST /api/drivers/{id}/respond       API Gateway
2. API Gateway          gRPC (proxy) or RabbitMQ publish     depends on impl
3. RabbitMQ             driver.cmd.trip_accept received       trip.exchange
4. Trip Service         consumes from driver_trip_response   RabbitMQ
5. Trip Service         updates trip status in MongoDB        local
6. Trip Service         publishes trip.event.driver_assigned  RabbitMQ (trip.exchange)
7. Payment Service      consumes create_payment_session       RabbitMQ
8. Payment Service      creates Stripe Checkout Session       Stripe API
9. Payment Service      stores session in MongoDB             local
10. Payment Service     publishes payment.event.session_created RabbitMQ (payment.exchange)
11. API Gateway         consumes notify_driver_assignment     RabbitMQ
12. API Gateway         broadcasts via WebSocket              connected clients
13. API Gateway         consumes notify_payment_status        RabbitMQ
14. API Gateway         broadcasts session URL via WebSocket connected clients
```

### Payment Completion Flow

```
1. User                 completes payment in Stripe           Stripe Checkout
2. Stripe               POST webhook /api/payments/webhook/stripe  API Gateway
3. API Gateway          proxies to Payment Service REST       8081 / internal
4. Payment Service      updates payment status in MongoDB     local
5. Payment Service      publishes payment.event.success       RabbitMQ (payment.exchange)
6. Trip Service         consumes payment_success_trip_update  RabbitMQ
7. Trip Service         updates trip to COMPLETED             MongoDB
```

---

## Configuration

### Kafka / RabbitMQ Config per Service

| Property | api-gateway | trip-service | driver-service | payment-service |
|---|---|---|---|---|
| `spring.rabbitmq.host` | rabbitmq | rabbitmq | rabbitmq | rabbitmq |
| `spring.rabbitmq.port` | 5672 | 5672 | 5672 | 5672 |
| `spring.data.mongodb.uri` | - | mongodb://mongodb:27017/trip | mongodb://mongodb:27017/driver | mongodb://mongodb:27017/payment |
| `grpc.server.port` | - | 9091 | 9092 | 9093 |
| `grpc.client.trip-service.address` | static://trip-service:9091 | - | - | - |
| `grpc.client.driver-service.address` | static://driver-service:9092 | - | - | - |
| `grpc.client.payment-service.address` | static://payment-service:9093 | - | - | - |

### Application Properties per Service

#### api-gateway (application.yml)
```yaml
server:
  port: 8080

spring:
  rabbitmq:
    host: localhost
    port: 5672

grpc:
  client:
    trip-service:
      address: static://localhost:9091
      negotiation-type: plaintext
    driver-service:
      address: static://localhost:9092
      negotiation-type: plaintext
    payment-service:
      address: static://localhost:9093
      negotiation-type: plaintext
```

#### trip-service (application.yml)
```yaml
server:
  port: 8081

spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/trip
  rabbitmq:
    host: localhost
    port: 5672

grpc:
  server:
    port: 9091
```

#### driver-service (application.yml)
```yaml
server:
  port: 8082

spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/driver
  rabbitmq:
    host: localhost
    port: 5672

grpc:
  server:
    port: 9092
```

#### payment-service (application.yml)
```yaml
server:
  port: 8083

spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/payment
  rabbitmq:
    host: localhost
    port: 5672

grpc:
  client:
    trip-service:
      address: static://localhost:8081
      negotiation-type: plaintext

stripe:
  api-key: sk_test_placeholder
```

---

## Dead Letter Queue Configuration

Each service should configure a Dead Letter Queue (DLQ) for RabbitMQ to handle processing failures.

```java
@Bean
public Queue tripEventDlq() {
    return QueueBuilder.durable("trip.event.dlq")
        .build();
}

@Bean
public Queue tripEventQueue() {
    return QueueBuilder.durable("trip.event.queue")
        .deadLetterExchange("")
        .deadLetterRoutingKey("trip.event.dlq")
        .build();
}
```

### DLQ Retry Policy

| Attempt | Action |
|---|---|
| 1st failure | Message sent to DLQ |
| DLQ consumer | Log error, send to alert channel |
| Manual replay | Re-publish from DLQ to original queue via admin tool |

---

## Error Codes

| Code | HTTP Status | Description |
|---|---|---|
| TRIP_NOT_FOUND | 404 | Trip ID does not exist |
| DRIVER_NOT_FOUND | 404 | Driver ID does not exist |
| TRIP_ALREADY_ASSIGNED | 409 | Trip already has a driver |
| TRIP_CANCELLED | 400 | Trip is already cancelled |
| NO_AVAILABLE_DRIVERS | 404 | No drivers available |
| PAYMENT_FAILED | 402 | Payment processing failed |
| INVALID_TRIP_STATUS | 400 | Cannot perform action in current status |
| VALIDATION_ERROR | 400 | Request validation failed |
| INTERNAL_ERROR | 500 | Unexpected server error |
