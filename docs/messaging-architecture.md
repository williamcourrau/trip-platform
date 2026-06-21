# Messaging Architecture

## Why RabbitMQ Exists Alongside gRPC

Two communication protocols exist in this platform because they solve different problems.

### gRPC for Synchronous Request-Response

Used when the client needs an immediate answer.

| Scenario | Why gRPC |
|---|---|
| API Gateway calls Trip Service to create a trip | Client is waiting for the trip ID and status |
| API Gateway calls Driver Service to get driver details | Client is waiting for driver info on screen |
| API Gateway previews a trip route and fare | Client is waiting for the estimate before confirming |

These are blocking calls. The calling service cannot proceed without the response. gRPC is best here because it is fast, typed, and streams efficiently.

### RabbitMQ for Asynchronous Event Distribution

Used when the caller does not need an immediate answer or when multiple consumers must react.

| Scenario | Why RabbitMQ |
|---|---|
| Trip created needs to notify available drivers | Trip service should not wait for every driver to be found |
| Driver assigned needs to trigger payment creation | Payment service should react independently, not as part of the assign request |
| Events must reach multiple services | Trip created goes to both Driver Service and API Gateway (WebSocket) |
| Services must remain decoupled | Driver Service can fail without taking down Trip Service |

These are fire-and-forget or broadcast patterns. RabbitMQ decouples the producers and consumers, adds durability guarantees, and allows multiple independent subscribers.

### Decision Matrix

| Criteria | Use gRPC | Use RabbitMQ |
|---|---|---|
| Caller needs a response | Yes | No |
| Multiple consumers needed | No | Yes |
| Service decoupling required | Low | High |
| Failures should not propagate | No | Yes |
| Latency tolerance | Low | Medium |
| Throughput priority | Medium | High |

---

## RabbitMQ Topology

### Exchanges

A topic exchange routes messages to queues based on routing key pattern matching.

| Exchange | Type | Durability | Purpose |
|---|---|---|---|
| `trip.exchange` | topic | durable | All trip lifecycle events and driver commands |
| `payment.exchange` | topic | durable | All payment lifecycle events |

### Queues and Bindings

#### Trip Exchange Bindings

```
trip.exchange (topic)
├── trip.event.created ──────────────────────────────────┐
│   ├── Binding: "trip.event.created"                    │
│   │   → find_available_drivers (Driver Service)        │
│   └── Binding: "trip.event.created"                    │
│       → notify_new_trip (API Gateway)                  │
├── trip.event.driver_assigned ──────────────────────────┤
│   ├── Binding: "trip.event.driver_assigned"            │
│   │   → notify_driver_assignment (API Gateway)         │
│   └── Binding: "trip.event.driver_assigned"            │
│       → create_payment_session (Payment Service)       │
├── trip.event.no_drivers_found ─────────────────────────┤
│   └── Binding: "trip.event.no_drivers_found"           │
│       → notify_driver_no_drivers_found (API Gateway)   │
├── driver.cmd.* ────────────────────────────────────────┤
│   └── Binding: "driver.cmd.*"                          │
│       → driver_trip_response (Trip Service)            │
└── driver.cmd.trip_request ─────────────────────────────┤
    └── Binding: "driver.cmd.trip_request"               │
        → driver_cmd_trip_request (Driver Service)       │
```

#### Payment Exchange Bindings

```
payment.exchange (topic)
├── payment.event.* ─────────────────────────────────────┐
│   └── Binding: "payment.event.*"                       │
│       → notify_payment_status (API Gateway)            │
└── payment.event.success ───────────────────────────────┤
    └── Binding: "payment.event.success"                 │
        → payment_success_trip_update (Trip Service)     │
```

### Routing Key Patterns

| Pattern | Meaning | Example Match |
|---|---|---|
| `trip.event.created` | Exact match | `trip.event.created` |
| `driver.cmd.*` | Wildcard, one word | `driver.cmd.trip_accept`, `driver.cmd.trip_decline` |
| `payment.event.*` | Wildcard, one word | `payment.event.success`, `payment.event.failed` |

All routing keys follow convention: `{domain}.{type}.{action}`

- `domain`: `trip`, `driver`, `payment`
- `type`: `event` or `cmd`
- `action`: verb describing what happened

---

## Reliability Patterns

### Dead Letter Queues

Every queue must have a corresponding dead letter queue where messages go after a failed processing attempt.

| Queue | Dead Letter Queue | Trigger |
|---|---|---|
| `find_available_drivers` | `find_available_drivers.dlq` | `spring.amqp.deserializationError` or consumer exception |
| `create_payment_session` | `create_payment_session.dlq` | Consumer exception |
| `driver_trip_response` | `driver_trip_response.dlq` | Consumer exception |

### Consumer Retry Configuration

Spring Boot consumer with retry and DLQ:

```java
@Bean
public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
        ConnectionFactory connectionFactory) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);

    factory.setAdviceChain(RetryInterceptorBuilder
        .stateless()
        .maxAttempts(3)
        .backOffOptions(1000, 2.0, 10000)  // 1s, 2s, 4s backoff
        .recoverer(new RejectAndDontRequeueRecoverer())
        .build());

    return factory;
}
```

After three failed attempts the message is rejected and routed to the DLQ.

### Idempotency

All event consumers must be idempotent because RabbitMQ guarantees at-least-once delivery.

Idempotency key strategy per event:

| Event | Idempotency Key |
|---|---|
| `trip.event.created` | tripId |
| `trip.event.driver_assigned` | tripId |
| `driver.cmd.trip_accept` | tripId + driverId |
| `payment.event.success` | sessionId |

Implementation approach in MongoDB:

```java
@RabbitListener(queues = QUEUE_DRIVER_TRIP_RESPONSE)
public void handleDriverResponse(DriverTripResponse response) {
    String dedupKey = response.getTripId() + ":" + response.getDriverId();

    // Check MongoDB for processed event
    if (deduplicationService.alreadyProcessed(dedupKey, "driver_response")) {
        return; // Skip duplicate
    }

    // Process the event
    tripService.assignDriver(response.getTripId(), response.getDriverId());

    // Record the processed event
    deduplicationService.recordProcessed(dedupKey, "driver_response");
}
```

### Message Ordering

RabbitMQ topic exchanges do not guarantee order across queues. Messages within a single queue are delivered in order as long as consumers process sequentially and do not use concurrent listeners.

Within this platform:
- `driver_trip_response` queue is consumed by a single Trip Service instance with `concurrency = 1`
- All other queues can use concurrent consumers (`concurrency = 3-5`) because ordering does not matter for independent trip events

---

## Event Payloads

### Trip Event Payloads

#### TripCreatedEvent (routing key: `trip.event.created`)

```json
{
  "tripId": "uuid",
  "userId": "string",
  "pickupLat": 123.456,
  "pickupLng": 789.012,
  "dropoffLat": 345.678,
  "dropoffLng": 901.234,
  "pickupAddress": "string",
  "dropoffAddress": "string"
}
```

#### DriverAssignmentEvent (routing key: `trip.event.driver_assigned`)

```json
{
  "tripId": "uuid",
  "driverId": "string",
  "driverName": "string",
  "driverPhone": "string",
  "status": "ASSIGNED"
}
```

#### DriverTripRequest (routing key: `driver.cmd.trip_request`)

```json
{
  "tripId": "uuid",
  "driverId": "string",
  "pickupLat": 123.456,
  "pickupLng": 789.012,
  "dropoffLat": 345.678,
  "dropoffLng": 901.234,
  "pickupAddress": "string",
  "dropoffAddress": "string",
  "estimatedDistance": 12.5,
  "estimatedDuration": 18.0
}
```

#### DriverTripResponse (routing key: `driver.cmd.trip_accept` or `driver.cmd.trip_decline`)

```json
{
  "tripId": "uuid",
  "driverId": "string",
  "action": "ACCEPT"
}
```

### Payment Event Payloads

#### PaymentSessionEvent (routing key: `payment.event.session_created`)

```json
{
  "tripId": "uuid",
  "sessionId": "cs_test_xxx",
  "paymentUrl": "https://checkout.stripe.com/pay/cs_test_xxx",
  "amount": 25.00
}
```

#### PaymentStatusEvent (routing key: `payment.event.success` or `payment.event.failed`)

```json
{
  "tripId": "uuid",
  "sessionId": "cs_test_xxx",
  "status": "SUCCESS"
}
```

---

## Configuration Reference

### application.yml (shared pattern for all services)

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    listener:
      simple:
        retry:
          enabled: true
          initial-interval: 1000
          max-attempts: 3
          multiplier: 2.0
          max-interval: 10000
        default-requeue-rejected: false
```

### Java configuration for each service

Each service must declare its own queues and bindings using the exchange from the common module:

```java
@Configuration
public class TripServiceRabbitMQConfig {

    @Bean
    public Queue driverTripResponseQueue() {
        return QueueBuilder.durable(QUEUE_DRIVER_TRIP_RESPONSE)
            .deadLetterExchange("")
            .deadLetterRoutingKey(QUEUE_DRIVER_TRIP_RESPONSE + ".dlq")
            .build();
    }

    @Bean
    public Queue driverTripResponseDlq() {
        return QueueBuilder.durable(QUEUE_DRIVER_TRIP_RESPONSE + ".dlq").build();
    }

    @Bean
    public Binding driverTripResponseBinding(TopicExchange tripExchange) {
        return BindingBuilder.bind(driverTripResponseQueue())
            .to(tripExchange)
            .with("driver.cmd.*");
    }
}
```

---

## What Happens When a Service Is Down

### Scenario: Driver Service is unavailable when a trip is created

1. Trip Service publishes `trip.event.created` to RabbitMQ
2. RabbitMQ holds the message in `find_available_drivers` queue
3. Driver Service comes back online after 30 seconds
4. Driver Service starts consuming from `find_available_drivers`
5. RabbitMQ delivers the queued message
6. Driver Service processes normally
7. No data lost, no recovery code needed

### Scenario: Payment Service is unavailable when driver is assigned

1. Trip Service publishes `trip.event.driver_assigned`
2. RabbitMQ holds the message in `create_payment_session` queue
3. Payment Service recovers, consumes the message
4. Payment Service calls Stripe, creates the session
5. Trip is already assigned, payment is created retroactively

### Scenario: API Gateway is down during event broadcast

1. Events accumulate in `notify_new_trip`, `notify_driver_assignment` queues
2. API Gateway recovers and drains the queues
3. WebSocket clients may miss events that occurred during downtime
4. Clients should poll `/api/trips/{tripId}` on reconnection to get current state

---

## gRPC vs RabbitMQ: When to Add a New Communication Path

### Add a new gRPC endpoint when

- The calling service needs a synchronous response before proceeding
- The data is requested on demand, not pushed
- Low latency is required
- Only one consumer needs the data

### Add a new RabbitMQ event when

- The producer should not wait for consumers
- Multiple services must react to the same event
- The consumer may be unavailable temporarily
- The event represents a state change that should be recorded

### Current communication summary

| From | To | Via | Why |
|---|---|---|---|
| API Gateway | Trip Service | gRPC | Client waiting for trip ID |
| API Gateway | Driver Service | gRPC | Client waiting for driver info |
| API Gateway | Payment Service | gRPC | Client waiting for session URL |
| Trip Service | Driver Service | RabbitMQ | Trip created, drivers must be found async |
| Trip Service | Payment Service | RabbitMQ | Driver assigned, payment should start async |
| Trip Service | API Gateway | RabbitMQ | Multiple events must reach WebSocket clients |
| Driver Service | API Gateway | RabbitMQ | Driver actions must reach WebSocket clients |
| Payment Service | API Gateway | RabbitMQ | Payment status must reach WebSocket clients |
| Payment Service | Trip Service | RabbitMQ | Payment success must update trip status |
