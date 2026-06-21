# Data Model and MongoDB Design

## Architecture Principle: Database per Service

Each microservice owns its own MongoDB database. No service directly accesses another service's database. Cross-service data sharing happens only through gRPC calls or RabbitMQ events.

| Service | Database | Collections |
|---|---|---|
| trip-service | `trip_db` | `trips`, `deduplication_log` |
| driver-service | `driver_db` | `drivers`, `driver_sessions` |
| payment-service | `payment_db` | `payments`, `deduplication_log` |
| api-gateway | none | No persistent storage |

---

## Connection Configuration

### Local Development (Docker Compose)

```yaml
services:
  mongodb:
    image: mongo:7
    ports:
      - "27017:27017"
    volumes:
      - mongo_data:/data/db
    healthcheck:
      test: echo 'db.runCommand("ping").ok' | mongosh --quiet
      interval: 10s
      retries: 5
```

### application.yml per Service

**trip-service:**
```yaml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/trip_db}
      auto-index-creation: true
```

**driver-service:**
```yaml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/driver_db}
      auto-index-creation: true
```

**payment-service:**
```yaml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/payment_db}
      auto-index-creation: true
```

### K8s ConfigMap

```yaml
data:
  MONGODB_URI: mongodb://mongodb-service:27017
```

Each service appends its own database name to the base URI in code or via a separate environment variable.

---

## Collections and Document Schemas

### trip-service: `trips` collection

```json
{
  "_id": "uuid",
  "userId": "string",
  "driverId": "string | null",
  "pickup": {
    "type": "Point",
    "coordinates": [-122.4194, 37.7749]
  },
  "dropoff": {
    "type": "Point",
    "coordinates": [-122.4312, 37.7852]
  },
  "pickupAddress": "string",
  "dropoffAddress": "string",
  "status": "PENDING",
  "route": {
    "estimatedDistance": 3.2,
    "estimatedDuration": 8.5,
    "estimatedFare": 12.50,
    "polyline": [
      {"lat": 37.7749, "lng": -122.4194},
      {"lat": 37.7800, "lng": -122.4250},
      {"lat": 37.7852, "lng": -122.4312}
    ]
  },
  "paymentStatus": "PENDING",
  "createdAt": "2026-06-21T12:00:00Z",
  "updatedAt": "2026-06-21T12:05:00Z",
  "version": 1
}
```

**Indexes:**

| Index | Fields | Type | Purpose |
|---|---|---|---|
| `pk_trip_id` | `_id` | unique | Primary key lookup |
| `idx_user_id` | `userId` | single | Find trips by user |
| `idx_driver_id` | `driverId` | single | Find trips by driver |
| `idx_status` | `status` | single | Filter by status |
| `idx_created_at` | `createdAt` | single | Sort by date |
| `idx_pickup_geo` | `pickup` | 2dsphere | Geospatial queries for nearby pickups |
| `idx_dropoff_geo` | `dropoff` | 2dsphere | Geospatial queries for nearby dropoffs |
| `idx_user_status` | `userId`, `status` | compound | User's trips by status |

### trip-service: `deduplication_log` collection

```json
{
  "_id": "dedup_key",
  "eventType": "string",
  "processedAt": "2026-06-21T12:00:00Z"
}
```

**Indexes:**

| Index | Fields | Type | Purpose |
|---|---|---|---|
| `pk_dedup_key` | `_id` | unique | Idempotency check (tripId + driverId + event type) |
| `idx_processed_at` | `processedAt` | single | TTL cleanup after 24 hours |
| `idx_event_type` | `eventType` | single | Filter dedup logs by event type |

TTL index on `processedAt` automatically deletes records older than 24 hours.

### driver-service: `drivers` collection

```json
{
  "_id": "uuid",
  "name": "string",
  "phone": "string",
  "email": "string",
  "location": {
    "type": "Point",
    "coordinates": [-122.4194, 37.7749]
  },
  "lastLocationUpdate": "2026-06-21T12:00:00Z",
  "available": true,
  "currentTripId": "string | null",
  "status": "ONLINE",
  "rating": 4.8,
  "totalTrips": 342,
  "createdAt": "2025-01-15T08:00:00Z"
}
```

**Indexes:**

| Index | Fields | Type | Purpose |
|---|---|---|---|
| `pk_driver_id` | `_id` | unique | Primary key lookup |
| `idx_available` | `available` | single | Find available drivers |
| `idx_location_geo` | `location` | 2dsphere | Find nearby drivers |
| `idx_status` | `status` | single | Filter by online/offline |
| `idx_avail_location` | `available`, `location` | compound | Available + nearby (covered query) |

### driver-service: `driver_sessions` collection

```json
{
  "_id": "session_token",
  "driverId": "uuid",
  "deviceInfo": "string",
  "ipAddress": "string",
  "connectedAt": "2026-06-21T10:00:00Z",
  "lastHeartbeat": "2026-06-21T11:55:00Z"
}
```

**Indexes:**

| Index | Fields | Type | Purpose |
|---|---|---|---|
| `pk_session` | `_id` | unique | Session token lookup |
| `idx_driver_id` | `driverId` | single | Find active sessions for driver |
| `idx_heartbeat` | `lastHeartbeat` | single | TTL cleanup for stale sessions |

### payment-service: `payments` collection

```json
{
  "_id": "uuid",
  "tripId": "uuid",
  "sessionId": "cs_test_xxx",
  "paymentIntentId": "pi_xxx",
  "amount": 25.00,
  "currency": "usd",
  "status": "SUCCESS",
  "paymentMethod": "card",
  "stripeEventId": "evt_xxx",
  "metadata": {
    "tripId": "uuid",
    "driverId": "uuid"
  },
  "createdAt": "2026-06-21T12:00:00Z",
  "updatedAt": "2026-06-21T12:01:00Z"
}
```

**Indexes:**

| Index | Fields | Type | Purpose |
|---|---|---|---|
| `pk_payment_id` | `_id` | unique | Primary key lookup |
| `idx_trip_id` | `tripId` | unique | One payment per trip |
| `idx_session_id` | `sessionId` | unique | Stripe session lookup |
| `idx_payment_intent` | `paymentIntentId` | unique | Stripe intent lookup |
| `idx_status` | `status` | single | Filter by status |
| `idx_stripe_event` | `stripeEventId` | unique | Idempotency for Stripe webhooks |

### payment-service: `deduplication_log` collection

Same structure as trip-service dedup collection. TTL index for 24-hour auto-cleanup.

---

## Data Flow Across Services

### Trip Creation

```
API Gateway                     Trip Service                    MongoDB (trip_db)
    │                               │                               │
    │  gRPC: CreateTrip()           │                               │
    ├──────────────────────────────►│                               │
    │                               │  Insert trip document         │
    │                               ├──────────────────────────────►│
    │                               │  ◄─── ok ─────────────────────│
    │                               │                               │
    │                               │  Publish trip.event.created   │
    │                               │  (async via RabbitMQ)         │
    │  ◄─── Trip ──────────────────│                               │
```

### Driver Assignment

```
Driver Service                  MongoDB (driver_db)         Trip Service (via gRPC)
    │                               │                            │
    │  Find available drivers       │                            │
    │  ├── db.drivers.find(         │                            │
    │  │   { available: true,       │                            │
    │  │     location: {            │                            │
    │  │       $nearSphere: ...     │                            │
    │  │     }                      │                            │
    │  │   })                       │                            │
    │  │◄── drivers ────────────────│                            │
    │                                                             │
    │  For each driver found:                                     │
    │    Publish driver.cmd.trip_request (RabbitMQ)               │
    │                                                             │
    │  Driver responds ACCEPT:                                    │
    │    └── gRPC: TripService.AssignDriver()                     │
    │                                        ├── trip_db.trips    │
    │                                        │   .updateOne(      │
    │                                        │     { _id: tripId },│
    │                                        │     { $set: {      │
    │                                        │       driverId,    │
    │                                        │       status:      │
    │                                        │        "DRIVER_    │
    │                                        │         ASSIGNED", │
    │                                        │       version: 2   │
    │                                        │     }}             │
    │                                        │   )                │
    │                                        ├── Publish event    │
```

### Payment Flow

```
Payment Service                 Stripe                      MongoDB (payment_db)
    │                               │                            │
    │  Receive driver_assigned      │                            │
    │  event (RabbitMQ)             │                            │
    │                               │                            │
    │  Create Stripe Checkout       │                            │
    │  ├──────────────────────────►│                            │
    │  │◄── session_id ────────────│                            │
    │                               │                            │
    │  Insert payment document      │                            │
    │  ├──────────────────────────────────────────────────────►  │
    │                                                             │
    │  Receive Stripe webhook       │                            │
    │  ◄────────────────────────────│                            │
    │                               │                            │
    │  Check deduplication:         │                            │
    │  ├── dedup_log.find(          │                            │
    │  │   { _id: stripeEventId }) │                            │
    │  │◄── null (not processed)   │                            │
    │                               │                            │
    │  Update payment status        │                            │
    │  ├── payments.updateOne(     │                            │
    │  │   { sessionId },          │                            │
    │  │   { $set: { status:       │                            │
    │  │       "SUCCESS" }})       │                            │
    │  │◄── ok ────────────────────│                            │
    │                               │                            │
    │  Record deduplication         │                            │
    │  ├── dedup_log.insertOne(    │                            │
    │  │   { _id: stripeEventId }) │                            │
    │                               │                            │
    │  Publish payment.event.success (RabbitMQ)                  │
```

---

## MongoDB Java Mapping

### Entity Classes

**Trip entity (trip-service):**

```java
@Document(collection = "trips")
public class Trip {

    @Id
    private String id;

    private String userId;
    private String driverId;

    private GeoJsonPoint pickup;
    private GeoJsonPoint dropoff;

    private String pickupAddress;
    private String dropoffAddress;

    @Enumerated(EnumType.STRING)
    private TripStatus status;

    private RouteInfo route;

    @Field("paymentStatus")
    private String paymentStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Version
    private Integer version;
}
```

**RouteInfo embedded document:**

```java
public class RouteInfo {
    private double estimatedDistance;
    private double estimatedDuration;
    private double estimatedFare;
    private List<Coordinate> polyline;
}
```

**Geospatial query example:**

```java
@Repository
public interface DriverRepository extends MongoRepository<Driver, String> {

    @Query("{ 'available': true, " +
           "'location': { " +
           "  $nearSphere: { " +
           "    $geometry: { " +
           "      type: 'Point', " +
           "      coordinates: [?0, ?1] " +
           "    }, " +
           "    $maxDistance: 5000 " +
           "  } " +
           "}}")
    List<Driver> findAvailableDriversNear(double lng, double lat, int maxDistance);
}
```

---

## Idempotency and Deduplication

### Why It Is Needed

RabbitMQ guarantees at-least-once delivery. A consumer may receive the same message multiple times if the broker detects a delivery failure. Each service must handle duplicate events without side effects.

### Pattern

```java
@Service
public class DeduplicationService {

    private final MongoTemplate mongoTemplate;

    public DeduplicationService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public boolean alreadyProcessed(String dedupKey) {
        return mongoTemplate.exists(
            Query.query(Criteria.where("_id").is(dedupKey)),
            "deduplication_log"
        );
    }

    public void markProcessed(String dedupKey, String eventType) {
        Document doc = new Document("_id", dedupKey)
            .append("eventType", eventType)
            .append("processedAt", new Date());
        mongoTemplate.insert(doc, "deduplication_log");
    }
}
```

### Usage in a Consumer

```java
@RabbitListener(queues = QUEUE_DRIVER_TRIP_RESPONSE)
public void handleDriverResponse(DriverTripResponse response) {
    String dedupKey = response.getTripId() + ":" + response.getDriverId();

    if (deduplicationService.alreadyProcessed(dedupKey)) {
        log.info("Duplicate event ignored: {}", dedupKey);
        return;
    }

    try {
        tripService.assignDriver(response.getTripId(), response.getDriverId());
        deduplicationService.markProcessed(dedupKey, "driver_response");
    } catch (Exception e) {
        log.error("Failed to process event: {}", dedupKey, e);
        throw e; // triggers RabbitMQ retry → DLQ
    }
}
```

### TTL Cleanup

```java
@Bean
public MongoTemplate mongoTemplate(MongoDatabaseFactory factory) {
    MongoTemplate template = new MongoTemplate(factory);
    template.indexOps("deduplication_log")
        .ensureIndex(new Index()
            .on("processedAt", Sort.Direction.ASC)
            .expire(24, TimeUnit.HOURS));
    return template;
}
```

---

## Transactions

MongoDB transactions are used when a single service must update multiple documents atomically.

### When Transactions Are Needed

| Scenario | Service | Documents Involved |
|---|---|---|
| Driver accepts trip | trip-service | trips.update + deduplication_log.insert |
| Payment webhook received | payment-service | payments.update + deduplication_log.insert |
| Driver goes offline mid-trip | driver-service | drivers.update + currentTripId cleared |

### When Transactions Are NOT Needed

| Scenario | Reason |
|---|---|
| Trip creation (single insert) | Single document write, atomic by default |
| Driver location update | No other document depends on this value |
| Status query | Read-only, no consistency requirements |

### Transaction Example

```java
@Service
public class TripService {

    private final MongoTemplate mongoTemplate;

    @Transactional
    public void assignDriver(String tripId, String driverId, String dedupKey) {
        // Update trip
        Query tripQuery = Query.query(Criteria.where("_id").is(tripId));
        Update tripUpdate = new Update()
            .set("driverId", driverId)
            .set("status", TripStatus.DRIVER_ASSIGNED)
            .set("updatedAt", LocalDateTime.now())
            .inc("version", 1);
        mongoTemplate.updateFirst(tripQuery, tripUpdate, "trips");

        // Record deduplication in same transaction
        Document dedup = new Document("_id", dedupKey)
            .append("eventType", "driver_response")
            .append("processedAt", new Date());
        mongoTemplate.insert(dedup, "deduplication_log");
    }
}
```

---

## Index Management

### Auto-Creation (Development)

Spring Data MongoDB creates indexes automatically when `spring.data.mongodb.auto-index-creation: true` is set. Suitable for development only.

### Manual Creation (Production)

Indexes should be created as part of the deployment process using MongoDB migration scripts executed before the service starts.

```javascript
// migration_001_create_trip_indexes.js
db = db.getSiblingDB("trip_db");

db.trips.createIndex({ "userId": 1 }, { name: "idx_user_id" });
db.trips.createIndex({ "driverId": 1 }, { name: "idx_driver_id" });
db.trips.createIndex({ "status": 1 }, { name: "idx_status" });
db.trips.createIndex({ "createdAt": -1 }, { name: "idx_created_at" });
db.trips.createIndex({ "pickup": "2dsphere" }, { name: "idx_pickup_geo" });
db.trips.createIndex(
  { "userId": 1, "status": 1 },
  { name: "idx_user_status" }
);

db.deduplication_log.createIndex(
  { "processedAt": 1 },
  { name: "idx_processed_at", expireAfterSeconds: 86400 }
);
```

### Performance Considerations

| Index | Write Impact | Read Impact | Notes |
|---|---|---|---|
| `idx_pickup_geo` | Medium | High | Only created if geospatial queries are used |
| `idx_avail_location` | Medium | High | Compound index is more efficient than two separate indexes |
| `idx_stripe_event` | Low | High | Unique index prevents duplicate Stripe webhooks |
| `idx_processed_at` TTL | Low | Low | Auto-cleanup prevents unbounded dedup log growth |
| `idx_session_id` | Low | High | Unique index, used for every webhook callback |

---

## Replica Set Configuration

### Local Development

Single node, no replica set. Transactions are not available in standalone mode. To enable transactions locally:

```yaml
services:
  mongodb:
    image: mongo:7
    command: ["--replSet", "rs0"]
    ports:
      - "27017:27017"
    healthcheck:
      test: |
        mongosh --quiet --eval "
          try {
            rs.status().ok;
          } catch(e) {
            rs.initiate({ _id: 'rs0', members: [{ _id: 0, host: 'localhost:27017' }] }).ok;
          }
        "
      interval: 5s
      retries: 10
```

### Production Recommendations

- Replica set with minimum 3 members
- Read preference: `primary` for writes, `primaryPreferred` for reads
- Write concern: `majority` for payments, `acknowledged` for driver location updates
- Backup via `mongodump` or Atlas continuous backup
- Connection string with multiple hosts for high availability

---

## Local Development Quick Start

```bash
# Start MongoDB
docker compose -f infra/development/docker-compose.yml up -d mongodb

# Verify connection
docker exec -it trip-platform-mongodb-1 mongosh --quiet --eval "db.version()"

# Seed test data
docker exec -i trip-platform-mongodb-1 mongosh trip_db <<EOF
db.trips.insertOne({
  _id: "test-trip-001",
  userId: "user-001",
  pickup: { type: "Point", coordinates: [-122.4194, 37.7749] },
  dropoff: { type: "Point", coordinates: [-122.4312, 37.7852] },
  status: "PENDING",
  createdAt: new Date(),
  updatedAt: new Date()
});
EOF

# Query test data
docker exec -i trip-platform-mongodb-1 mongosh trip_db --eval "db.trips.find().pretty()"
```
