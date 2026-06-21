# ATE System Architecture

## System Overview

The Automated Test Equipment system orchestrates semiconductor validation by managing the lifecycle of test programs across physical test station machines. Engineers interact through a desktop application where they load test program paths, configure parameters, and start execution. The core engine communicates with worker agents on test station machines via gRPC and distributes events and logs through RabbitMQ.

The core engine also exposes a gRPC server that external services can call to submit test jobs, query status, and retrieve results programmatically. This enables integration with factory automation systems, CI/CD pipelines, reporting tools, and other enterprise services without requiring a human operator at the desktop.

The system is entirely on premise: no cloud dependencies. It ships as a combined installer that provisions RabbitMQ, a .NET runtime, and all required services on engineer workstations and test station machines.

---

## High Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│  Engineer Workstation                                                │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Visual Studio Extension (OTPL Language Support)             │   │
│  │  ┌───────────────────────────────────────────────────────┐  │   │
│  │  │  OTPL Editor, Syntax Validator, Semantic Analyzer     │  │   │
│  │  └───────────────────────────────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Desktop Application (Core Engine)                           │   │
│  │                                                              │   │
│  │  ┌─────────────────────┐  ┌──────────────────────────────┐ │   │
│  │  │  Test Lifecycle     │  │  gRPC Server (External API)  │ │   │
│  │  │  State Machine      │  │  Port: 50051                 │ │   │
│  │  │                     │  │                              │ │   │
│  │  │  Load → Execute →   │  │  • SubmitTestJob()           │ │   │
│  │  │  Unload             │  │  • GetStatus()               │ │   │
│  │  │                     │  │  • GetResults()              │ │   │
│  │  └─────────────────────┘  │  • CancelJob()               │ │   │
│  │                           │  • ListStations()            │ │   │
│  │                           └──────────┬───────────────────┘ │   │
│  └──────────────────────────────────────┼──────────────────────┘   │
│                                         │                           │
│              ┌──────────────────────────┼──────────────┐            │
│              │ gRPC (to workers)        │ gRPC (server)│ RabbitMQ   │
│              ▼                          ▼              ▼            │
│        ┌──────────┐           ┌──────────────┐  ┌──────────────┐   │
│        │ Workers  │           │ External     │  │ RabbitMQ     │   │
│        │ (gRPC    │           │ Services     │  │ (events,     │   │
│        │  client) │           │ (gRPC        │  │  logs)       │   │
│        └──────────┘           │  clients)    │  └──────────────┘   │
│                               └──────────────┘                     │
└─────────────────────────────────────────────────────────────────────┘
              │
              │ gRPC
              ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Test Station Machines                                              │
│  (one per DUT station, physical hardware connected)                 │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Worker Agent (Windows Service)                               │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │   │
│  │  │ DUT Control  │  │ Measurement  │  │ Result Reporting │  │   │
│  │  │ Interface    │  │ Instruments  │  │ & Logging        │  │   │
│  │  └──────┬───────┘  └──────┬───────┘  └──────────────────┘  │   │
│  └─────────┼─────────────────┼────────────────────────────────┘   │
│            │                 │                                      │
│            ▼                 ▼                                      │
│      ┌──────────┐     ┌──────────┐                                 │
│      │ DUT #1   │     │ DUT #2   │  (physical devices)             │
│      │ (Device  │     │ (Device  │                                 │
│      │  Under   │     │  Under   │                                 │
│      │  Test)   │     │  Test)   │                                 │
│      └──────────┘     └──────────┘                                 │
└─────────────────────────────────────────────────────────────────────┘
```

---

## How Engineers Use the System

### Workflow

```
1. Engineer opens the desktop application
2. Loads a compiled test program binary (from OTPL compilation via VS extension)
3. Selects or configures the target DUT from a list of available test stations
4. Optionally overrides test parameters
5. Clicks Start

   The desktop app (Core Engine) handles the rest:
   • Sends the program to the Worker Agent on the target test station
   • Walks through the test lifecycle: Load → Start Unit → Execute → End Unit → Unload
   • Displays real-time logs and progress in the output panel
   • Shows pass/fail results per test method
   • Saves a summary report locally

6. Engineer reviews results, exports reports, or reloads with modified parameters
```

### Desktop Application Interface

```
┌──────────────────────────────────────────────────────────────────┐
│  ATE Test Controller - [ChipValidator v2.1]                       │
├──────────────────────────────────────────────────────────────────┤
│  File  Tools  Help                                                │
├──────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌──────────────────────────────────────────┐  │
│  │ Test Program │  │  Output / Logs                          │  │
│  │ Path:        │  │  [14:32:01] Program loaded              │  │
│  │ [.../tests/  │  │  [14:32:02] Unit A initialized          │  │
│  │  chip.otpl]  │  │  [14:32:03] Executing: measureVoltage  │  │
│  │              │  │  [14:32:05]   PASS: VDD = 1.81V        │  │
│  │ DUT Config:  │  │  [14:32:06] Executing: cycleTest       │  │
│  │ [Station 3]  │  │  [14:32:10]   PASS: Cycles 100/100    │  │
│  │              │  │  [14:32:11] Unit completed              │  │
│  │ Parameters:  │  │  [14:32:12] Report saved               │  │
│  │ VDD=1.8V    │  │  ──────────────────────────────────────  │  │
│  │ Cycles=100  │  │                                           │  │
│  │              │  │                                           │  │
│  │ [START]  [▼] │  │                                           │  │
│  └─────────────┘  └──────────────────────────────────────────┘  │
├──────────────────────────────────────────────────────────────────┤
│  Status: RUNNING  |  Unit: UnitA  |  Progress: 4/12 methods     │
└──────────────────────────────────────────────────────────────────┘
```

---

## Test Core Engine Lifecycle

The engine runs inside the desktop application. It is a state machine with five phases.

```
LOAD PROGRAM
┌────────────────────────────────────────────────────────────┐
│ 1. Read compiled test program binary from disk             │
│ 2. Resolve method references against available libraries   │
│ 3. Send program to Worker Agent via gRPC                   │
│ 4. Worker Agent prepares the DUT connection                │
│ 5. Publish: test.event.program_loaded                      │
└────────────────────────────────────────────────────────────┘
          │
          ▼
START UNIT
┌────────────────────────────────────────────────────────────┐
│ 1. Worker Agent initializes the physical DUT               │
│ 2. Apply pin configurations (voltages, clocks)             │
│ 3. Run pre-test calibration checks                         │
│ 4. Publish: test.event.unit_started                        │
└────────────────────────────────────────────────────────────┘
          │
          ▼
EXECUTE TEST
┌────────────────────────────────────────────────────────────┐
│ 1. Engine walks the test workflow tree                     │
│ 2. For each test method:                                   │
│    a. Send method + parameters to Worker Agent via gRPC    │
│    b. Worker executes on the physical DUT                  │
│    c. Worker streams logs back via RabbitMQ                │
│    d. Worker returns pass/fail + measurement data          │
│    e. Engine evaluates workflow conditions (IF/ELSE)       │
│ 3. Handle custom test methods (engineer defined in OTPL)   │
│ 4. Publish: test.event.test_completed per method           │
└────────────────────────────────────────────────────────────┘
          │
          ▼
END UNIT
┌────────────────────────────────────────────────────────────┐
│ 1. Worker Agent tears down DUT connection                  │
│ 2. Remove pin configurations                               │
│ 3. Collate all test results                                │
│ 4. Generate unit summary                                   │
│ 5. Publish: test.event.unit_completed                      │
└────────────────────────────────────────────────────────────┘
          │
          ▼
UNLOAD PROGRAM
┌────────────────────────────────────────────────────────────┐
│ 1. Release program resources on Worker Agent               │
│ 2. Finalize log stream                                     │
│ 3. Save report to disk                                     │
│ 4. Publish: test.event.program_unloaded                    │
└────────────────────────────────────────────────────────────┘

On ERROR at any phase:
  1. Publish: test.event.error with phase + exception details
  2. Worker Agent attempts graceful DUT cleanup
  3. Engine transitions to UNLOADED state
  4. Display error in the desktop output panel
```

---

## gRPC Service Definitions

### Core Engine External API (gRPC Server)

The Core Engine exposes a gRPC server on port 50051 that external services can call. This allows programmatic access to the test system without requiring a human at the desktop.

```protobuf
service TestEngine {
  // Submit a test job programmatically
  rpc SubmitTestJob (TestJobRequest) returns (TestJobResponse);

  // Query job status
  rpc GetJobStatus (JobStatusRequest) returns (JobStatusResponse);

  // Retrieve test results
  rpc GetTestResults (TestResultsRequest) returns (TestResultsResponse);

  // Cancel a running job
  rpc CancelJob (CancelJobRequest) returns (CancelJobResponse);

  // List available test stations
  rpc ListStations (ListStationsRequest) returns (ListStationsResponse);

  // Stream real-time logs for a job
  rpc StreamJobLogs (JobLogsRequest) returns (stream LogEntry);

  // Subscribe to job status changes
  rpc WatchJobStatus (JobStatusRequest) returns (stream JobStatusEvent);
}

message TestJobRequest {
  string job_id = 1;
  string program_path = 2;              // path to .otplb on the workstation
  string station_id = 3;                // target test station
  string unit_id = 4;
  map<string, string> parameters = 5;   // parameter overrides
  PinConfig pin_config = 6;
  int32 priority = 7;                   // scheduling priority
}

message TestJobResponse {
  string job_id = 1;
  string status = 2;                    // SUBMITTED, QUEUED, RUNNING, COMPLETED, FAILED
  string message = 3;
}

message JobStatusRequest {
  string job_id = 1;
}

message JobStatusResponse {
  string job_id = 1;
  string status = 2;
  string current_phase = 3;             // LOADING, EXECUTING, UNLOADING
  string current_method = 4;
  int32 completed_methods = 5;
  int32 total_methods = 6;
  double progress_percent = 7;
  string started_at = 8;
  string completed_at = 9;
  string error_message = 10;
}

message TestResultsRequest {
  string job_id = 1;
  bool include_measurements = 2;
}

message TestResultsResponse {
  string job_id = 1;
  string status = 2;
  bool overall_pass = 3;
  repeated MethodResult methods = 4;
  string report_path = 5;               // path to saved report file
}

message MethodResult {
  string method_id = 1;
  string method_name = 2;
  bool passed = 3;
  map<string, double> measurements = 4;
  string error_message = 5;
  double duration_ms = 6;
}

message ListStationsRequest {
  // empty
}

message ListStationsResponse {
  repeated StationInfo stations = 1;
}

message StationInfo {
  string station_id = 1;
  string status = 2;                    // ONLINE, OFFLINE, BUSY
  string current_job_id = 3;
  string hostname = 4;
  string worker_version = 5;
}

message JobLogsRequest {
  string job_id = 1;
  string level_filter = 2;              // INFO, WARNING, ERROR, DEBUG
}

message LogEntry {
  string timestamp = 1;
  string level = 2;
  string source = 3;
  string message = 4;
  string job_id = 5;
  string method_id = 6;
}

message JobStatusEvent {
  string job_id = 1;
  string previous_status = 2;
  string new_status = 3;
  string message = 4;
  string timestamp = 5;
}

message CancelJobRequest {
  string job_id = 1;
  string reason = 2;
}

message CancelJobResponse {
  bool success = 1;
  string message = 2;
}

message PinConfig {
  map<string, double> voltages = 1;     // pin name → voltage
  map<string, double> currents = 2;
  double clock_frequency_mhz = 3;
  string temperature_profile = 4;
}
```

### Core Engine to Worker Agent (gRPC Client)

The desktop app calls these directly on the Worker Agent running on the target test station. This is internal communication, not exposed to external services.

```protobuf
service WorkerAgent {
  // Lifecycle
  rpc LoadProgram (LoadProgramRequest) returns (LoadProgramResponse);
  rpc UnloadProgram (UnloadProgramRequest) returns (UnloadProgramResponse);

  // Unit control
  rpc StartUnit (StartUnitRequest) returns (StartUnitResponse);
  rpc EndUnit (EndUnitRequest) returns (EndUnitResponse);

  // Test execution
  rpc ExecuteMethod (ExecuteMethodRequest) returns (ExecuteMethodResponse);

  // Status and control
  rpc GetStatus (GetStatusRequest) returns (WorkerStatus);
  rpc CancelExecution (CancelRequest) returns (CancelResponse);
  rpc GetAvailableStations (Empty) returns (StationList);
}

message LoadProgramRequest {
  string program_id = 1;
  bytes program_binary = 2;       // compiled OTPL binary
  string otpl_version = 3;
}

message StartUnitRequest {
  string unit_id = 1;
  string program_id = 2;
  PinConfig pin_config = 3;       // voltages, clocks, etc.
  map<string, string> parameters = 4;
}

message ExecuteMethodRequest {
  string execution_id = 1;
  string method_id = 2;
  string workflow_id = 3;
  bytes method_binary = 4;        // compiled method
  map<string, string> parameters = 5;
}

message ExecuteMethodResponse {
  string execution_id = 1;
  bool passed = 2;
  map<string, double> measurements = 3;  // measured values per pin/channel
  string error_message = 4;
  bytes result_data = 5;                 // raw measurement data
}

message WorkerStatus {
  enum State { IDLE = 0; LOADED = 1; RUNNING = 2; ERROR = 3; }
  State state = 1;
  string current_program_id = 2;
  string current_unit_id = 3;
  string current_method_id = 4;
  double uptime_seconds = 5;
}
```

---

## External Service Integration

The Core Engine gRPC server enables programmatic access from external systems. This section describes how different types of external services interact with the test system.

### Integration Use Cases

| External Service | Integration Pattern | gRPC Methods Used |
|---|---|---|
| Factory MES (Manufacturing Execution System) | Submits test jobs when units reach the test station | `SubmitTestJob`, `GetJobStatus`, `GetTestResults` |
| CI/CD Pipeline | Triggers regression tests on new OTPL releases | `SubmitTestJob`, `WatchJobStatus` |
| Centralized Reporting Dashboard | Queries historical results across all stations | `GetTestResults`, `ListStations` |
| Automated Alerting System | Monitors for failures and pages engineers | `WatchJobStatus`, `StreamJobLogs` |
| Scheduling / Orchestrator | Queues jobs across stations based on priority and availability | `ListStations`, `SubmitTestJob`, `CancelJob` |

### Flow: External Service Submits a Test Job

```
External Service              Core Engine (gRPC Server)         Worker Agent          RabbitMQ
      │                                  │                          │                    │
      │  gRPC: ListStations()            │                          │                    │
      ├─────────────────────────────────►│                          │                    │
      │  ◄─── { stations } ─────────────│                          │                    │
      │                                  │                          │                    │
      │  gRPC: SubmitTestJob()           │                          │                    │
      ├─────────────────────────────────►│                          │                    │
      │  ◄─── { jobId, SUBMITTED } ─────│                          │                    │
      │                                  │                          │                    │
      │                                  │  gRPC: LoadProgram()     │                    │
      │                                  ├─────────────────────────►│                    │
      │                                  │  pub test.event.loaded ────────────────►      │
      │                                  │                          │                    │
      │                                  │  gRPC: StartUnit()       │                    │
      │                                  ├─────────────────────────►│                    │
      │                                  │  pub test.event.started ───────────────►       │
      │                                  │                          │                    │
      │  gRPC: WatchJobStatus()          │                          │                    │
      ├─────────────────────────────────►│                          │                    │
      │  ◄── stream: RUNNING ───────────│                          │                    │
      │  ◄── stream: EXECUTING ────────│                          │                    │
      │                                  │  [For each method]       │                    │
      │                                  │  gRPC: ExecuteMethod()   │                    │
      │                                  ├─────────────────────────►│                    │
      │  (logs stream separately)        │  ◄─── result ───────────│                    │
      │                                  │                          │                    │
      │  ◄── stream: COMPLETED ─────────│                          │                    │
      │                                  │                          │                    │
      │  gRPC: GetTestResults()          │                          │                    │
      ├─────────────────────────────────►│                          │                    │
      │  ◄─── { results, report } ──────│                          │                    │
```

### Authentication and Authorization

The gRPC server supports pluggable authentication. The installer configures one of:

| Method | Description |
|---|---|
| Certificate-based | Each external service gets a client certificate signed by the workstation CA |
| Windows Authentication | Uses the engineer workstation domain credentials (integrated security) |
| API Token | Simple token passed in gRPC metadata headers |
| None (insecure) | For development environments only, disabled in production |

### gRPC Metadata Headers

External services pass identification in gRPC metadata:

```
Authorization: Bearer <token>
x-service-name: <name>
x-service-version: <version>
x-request-id: <uuid>
```

### Error Handling for External Calls

| gRPC Status Code | When Returned |
|---|---|
| `OK` | Call succeeded |
| `INVALID_ARGUMENT` | Missing or invalid parameters |
| `UNAUTHENTICATED` | Missing or invalid credentials |
| `NOT_FOUND` | Job ID or station ID does not exist |
| `FAILED_PRECONDITION` | Station is offline or busy |
| `UNAVAILABLE` | Core Engine is not running |
| `INTERNAL` | Unexpected error during execution |

### Job Queueing

When the Core Engine receives a `SubmitTestJob` request for a busy station:

1. The job is added to an in-memory priority queue
2. The response returns `{ status: "QUEUED" }` with the job ID
3. When the station becomes idle, the engine dequeues the highest priority job
4. The external service receives a `WatchJobStatus` stream update: `QUEUED → SUBMITTED → RUNNING`
5. If a higher priority job arrives, it preempts the queued position (priority inversion handled via aging)

---

## RabbitMQ Event Topology

RabbitMQ runs on the engineer workstation or a shared network-accessible machine. Events are published by the Core Engine and Worker Agents and consumed by the desktop UI, logging service, and any external services subscribed to the exchange.

### Exchanges

| Exchange | Type | Purpose |
|---|---|---|
| `test.exchange` | topic | Test lifecycle events |
| `log.exchange` | topic | Structured log entries from engine and workers |
| `worker.exchange` | direct | Worker status updates |

### Test Exchange Events

| Routing Key | Publisher | Consumer | Payload |
|---|---|---|---|
| `test.event.program_loaded` | Core Engine | Desktop UI | `{ programId, timestamp }` |
| `test.event.unit_started` | Core Engine | Desktop UI | `{ unitId, pinConfig }` |
| `test.event.test_completed` | Core Engine | Desktop UI | `{ methodId, passed, measurements }` |
| `test.event.unit_completed` | Core Engine | Desktop UI | `{ unitId, summary }` |
| `test.event.program_unloaded` | Core Engine | Desktop UI | `{ programId }` |
| `test.event.error` | Core Engine / Worker | Desktop UI | `{ phase, message, stackTrace }` |
| `test.event.progress` | Core Engine | Desktop UI | `{ completed, total, currentMethod }` |

### Log Exchange Events

| Routing Key | Publisher | Consumer | Payload |
|---|---|---|---|
| `log.event.info` | Core Engine, Worker | Desktop UI, Log File | `{ level, message, source, timestamp }` |
| `log.event.warning` | Core Engine, Worker | Desktop UI, Log File | `{ level, message, source, timestamp }` |
| `log.event.error` | Core Engine, Worker | Desktop UI, Log File | `{ level, message, source, timestamp }` |
| `log.event.debug` | Core Engine, Worker | Desktop UI, Log File | `{ level, message, source, timestamp }` |

### Worker Exchange Events

| Routing Key | Publisher | Consumer | Payload |
|---|---|---|---|
| `worker.event.status_change` | Worker Agent | Core Engine | `{ stationId, newState, currentMethod }` |
| `worker.event.method_progress` | Worker Agent | Core Engine | `{ methodId, progressPercent }` |

---

## Communication Flow: End to End

### Engineer Loads and Runs a Test

```
Engineer                    Desktop App (Engine)            Worker Agent            RabbitMQ
   │                              │                            │                      │
   │  Select program path         │                            │                      │
   ├─────────────────────────────►│                            │                      │
   │                              │                            │                      │
   │  Select DUT station          │                            │                      │
   ├─────────────────────────────►│                            │                      │
   │                              │                            │                      │
   │  Click START                 │                            │                      │
   ├─────────────────────────────►│                            │                      │
   │                              │                            │                      │
   │                              │  gRPC: LoadProgram()       │                      │
   │                              ├──────────────────────────► │                      │
   │                              │  ◄─── OK ───────────────── │                      │
   │                              │                            │                      │
   │                              │  pub test.event.program_loaded ───────────────►  │
   │                              │                            │                      │
   │                              │  gRPC: StartUnit()         │                      │
   │                              ├──────────────────────────► │                      │
   │                              │  ◄─── OK ───────────────── │                      │
   │                              │                            │                      │
   │                              │  pub test.event.unit_started ────────────────►   │
   │                              │                            │                      │
   │                              │  [For each test method]    │                      │
   │                              │  gRPC: ExecuteMethod()     │                      │
   │                              ├──────────────────────────► │                      │
   │                              │  pub log.event.info ────────────────────────►     │
   │                              │  ◄─── result ──────────── │                      │
   │                              │                            │                      │
   │  Update UI                   │                            │                      │
   │◄─────────────────────────────│                            │                      │
   │                              │                            │                      │
   │                              │  gRPC: EndUnit()           │                      │
   │                              ├──────────────────────────► │                      │
   │                              │  ◄─── OK ───────────────── │                      │
   │                              │                            │                      │
   │                              │  gRPC: UnloadProgram()     │                      │
   │                              ├──────────────────────────► │                      │
   │                              │  ◄─── OK ───────────────── │                      │
   │                              │                            │                      │
   │                              │  pub test.event.program_unloaded ──────────────►  │
   │                              │                            │                      │
   │  Show final report           │                            │                      │
   │◄─────────────────────────────│                            │                      │
```

---

## Communication Decision Summary

| Path | Protocol | Why |
|---|---|---|
| Core Engine ↔ Worker Agent | gRPC | Synchronous: engine blocks on each method execution, needs result before continuing workflow |
| External Service ↔ Core Engine | gRPC | Synchronous: external systems need immediate response for job submission, status queries, and result retrieval; streaming for real-time log and status updates |
| Core Engine → Desktop UI logs | RabbitMQ | Asynchronous: UI must never block the engine; logs arrive as fast as the UI can display them |
| Worker Agent → Core Engine (progress) | RabbitMQ | Async: workers report progress without waiting; engine aggregates |
| Desktop UI discovery of stations | gRPC | Sync: needs immediate list of available stations before the engineer can start |
| Core Engine → Log File writer | RabbitMQ | Async: log writes must not affect test timing |

---

## OTPL Language and Visual Studio Extension

The VS Extension is a separate tool for authoring and validating test programs. It does not run tests. It compiles OTPL source into the binary format that the desktop application loads.

### Compilation Pipeline

```
OTPL Source (.otpl) ──► OTPL Compiler (VS Extension) ──► Compiled Binary (.otplb)
                              │
                    ┌─────────┴─────────┐
                    │                    │
              Syntax Check        Semantic Analysis
              • Keywords          • Type resolution
              • Grammar           • Method existence validation
              • Structure         • Parameter compatibility
                                  • Flow path validation
                                  • Custom method registry
```

### VS Extension Components

| Component | Purpose |
|---|---|
| OTPL Editor | Code editing with syntax highlighting, code folding, IntelliSense |
| Syntax Validator | Real-time grammar checks, error squiggles |
| Semantic Analyzer | Type checking, method resolution, flow validation |
| Compiler | Produces `.otplb` binary that the desktop app loads |
| Test Explorer | Browse compiled programs, view method tree (read-only) |

---

## Combined Installer

### What the Installer Sets Up

```
Combined Installer (.msi)
│
├── Prerequisites
│   ├── .NET Runtime 6.0+ (if not present)
│   └── RabbitMQ (embedded Erlang + RabbitMQ Server, configures Windows service)
│
├── On the Engineer Workstation
│   ├── Desktop Application (Core Engine)
│   │   • Test lifecycle state machine
│   │   • gRPC server on port 50051 (external services API)
│   │   • gRPC client (connects to Worker Agents)
│   │   • RabbitMQ publisher (events and logs)
│   ├── RabbitMQ (Windows Service, runs locally)
│   ├── Logging Service (Windows Service, writes log files)
│   └── CLI utility (testctl.exe for headless scripting)
│       • Wraps the gRPC external API for shell scripts and CI/CD
│
├── On Each Test Station Machine
│   ├── Worker Agent (Windows Service)
│   │   • Connects back to the engineer workstation's RabbitMQ
│   │   • Listens for gRPC connections on a configurable port
│   │   • Controls DUT hardware through vendor-specific drivers
│   └── DUT Driver Interfaces (vendor DLLs)
│
└── Visual Studio Extension
    └── OTPL Language Support (.vsix)
```

### Network Topology

```
┌─────────────────────┐         ┌─────────────────────┐
│  Engineer WS #1     │         │  Engineer WS #2     │
│  • Desktop App      │         │  • Desktop App      │
│  • RabbitMQ         │         │  • RabbitMQ          │
│  • Logging Service  │         │  • Logging Service   │
│  • VS Extension     │         │  • VS Extension      │
└────────┬────────────┘         └────────┬────────────┘
         │                               │
         │   Network (LAN)               │
         │                               │
         ├───────────────────────────────┤
         │                               │
┌────────┴────────────┐    ┌─────────────┴───────────┐
│  Test Station #1    │    │  Test Station #2         │
│  • Worker Agent     │    │  • Worker Agent          │
│  • DUT #1, #2       │    │  • DUT #3, #4            │
└─────────────────────┘    └──────────────────────────┘
```

Each engineer workstation has its own RabbitMQ instance and connects to shared test station machines over the LAN. Test stations are configured with the engineer workstation address so the Worker Agent knows where to publish logs and status events.
