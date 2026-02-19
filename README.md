# SMPPulse

**SMPP Server Load Testing Application**

A standalone desktop application for load testing SMPP servers, built with Java 21 and JavaFX. SMPPulse generates high-throughput SMPP traffic using virtual threads, provides real-time monitoring dashboards, and supports configurable DLR handling with a dynamic parameter template system.

---

## Features

- **Dual Operation Modes** - Connect to an existing SMSC (ESME mode) or run a built-in SMSC simulator that accepts ESME connections
- **Massive Concurrency** - Java 21 virtual threads power the load generation engine for high TPS with minimal resource usage
- **Real-Time Dashboard** - Live TPS line chart, success/failure pie chart, latency percentiles (P50/P95/P99), and counter tiles
- **Dynamic Templates** - Parameterize source/destination addresses, message text, and TLV values with placeholders like `${random.digits(10)}`, `${sequence}`, `${uuid}`, and more
- **DLR Support** - Request and track delivery receipts in ESME mode; generate configurable DLRs in SMSC simulator mode
- **Profile Management** - Save and load complete test configurations as YAML profiles
- **Report Export** - Export results as CSV, JSON, or self-contained HTML reports with embedded charts
- **Dark Theme UI** - Purpose-built dark interface designed for extended testing sessions
- **PDU Message Log** - Real-time filterable table of all SMPP PDU traffic with direction, type, and status

## Requirements

- **Java 21** or later (with preview features)
- **Maven 3.8+** (for building)

## Quick Start

### Build

```bash
# Clone the repository
git clone <repository-url>
cd SMPPulse

# Build the fat JAR
mvn clean package
```

### Run

```bash
# Run with Maven (development)
mvn javafx:run

# Run the fat JAR (standalone)
java --enable-preview -jar target/smppulse-fat.jar
```

## Usage Guide

### 1. Connection Setup

Navigate to the **Connection** tab on the left panel.

| Field | Description | Default |
|---|---|---|
| Host | SMPP server hostname or IP | `localhost` |
| Port | SMPP server port | `2775` |
| System ID | ESME identifier for binding | `smppclient` |
| Password | Bind password | `password` |
| Bind Type | `TRANSMITTER`, `RECEIVER`, or `TRANSCEIVER` | `TRANSCEIVER` |
| Mode | `ESME` (connect to SMSC) or `SMSC` (accept connections) | `ESME` |
| Session Count | Number of concurrent SMPP sessions | `1` |
| Window Size | Max unacknowledged PDUs per session | `10` |
| TLS | Enable TLS encryption | Off |

**Auto-Reconnect** is enabled by default with exponential backoff (1s initial delay, 60s max, 2x multiplier).

Click **Connect** to establish the SMPP session. The status indicator in the bottom bar turns green when connected.

### 2. Configure Load Test

Navigate to the **Load Test** tab.

| Field | Description | Default |
|---|---|---|
| Target TPS | Messages per second to sustain | `100` |
| Ramp Up | Seconds to linearly ramp from 0 to target TPS | `0` |
| Ramp Down | Seconds to linearly ramp from target TPS to 0 | `0` |
| Duration | Total test duration in seconds (0 = unlimited) | `60` |
| Total Messages | Stop after N messages (0 = use duration) | `0` |
| Operation Type | `SUBMIT_SM` or `DATA_SM` | `SUBMIT_SM` |
| Max In-Flight | Maximum concurrent pending messages | `1000` |

### 3. Configure Message Template

Navigate to the **Template** tab.

Define message content using static text and dynamic placeholders:

```
Source:  ${random.digits(10)}
Dest:    ${random.digits(10)}
Message: Test message ${sequence} at ${timestamp}
```

#### Available Placeholders

| Placeholder | Description | Example Output |
|---|---|---|
| `${random.digits(n)}` | Random digits of length n | `8472951036` |
| `${random.alpha(n)}` | Random letters of length n | `kZpQmWxBnY` |
| `${random.alphanumeric(n)}` | Random alphanumeric of length n | `a3Bx9kM2pQ` |
| `${sequence}` | Auto-incrementing number | `1`, `2`, `3`... |
| `${uuid}` | Random UUID | `550e8400-e29b-41d4-...` |
| `${timestamp}` | Current epoch milliseconds | `1708300800000` |
| `${timestamp:format}` | Formatted timestamp | `${timestamp:yyyyMMddHHmmss}` |
| `${range(min,max)}` | Random integer in range | `${range(1,100)}` -> `47` |
| `${file:path:random}` | Random line from a text file | `${file:numbers.txt:random}` |
| `${csv:path:column}` | Value from a CSV column | `${csv:data.csv:phone}` |

Click **Resolve Preview** to see sample resolved values before running the test.

#### Optional TLV Parameters

Add custom TLV (Tag-Length-Value) parameters by entering a hex tag and value. TLV values also support placeholders.

### 4. Configure DLR (Optional)

Navigate to the **DLR** tab.

**ESME Mode (requesting DLRs):**

| Field | Description | Default |
|---|---|---|
| Request DLR | Include registered_delivery in submit_sm | Off |
| Registered Delivery | Bitmask value for delivery receipt request | `1` |
| DLR Timeout | Seconds to wait before marking a DLR as timed out | `60` |

**SMSC Simulator Mode (generating DLRs):**

| Field | Description | Default |
|---|---|---|
| Generate DLR | Send delivery receipts for received messages | On |
| DLR Delay | Milliseconds to wait before sending the DLR | `1000` |
| Success Rate | Percentage of DLRs with DELIVRD status (0.0 - 1.0) | `0.95` |
| DLR Format | Format pattern for the DLR text body | Standard SMPP format |

### 5. Run the Test

- Press **F5** or go to **Test > Start** to begin the load test
- Press **F6** or **Test > Pause** to pause/resume
- Press **F7** or **Test > Stop** to stop the test

### 6. Monitor Results

Switch to the **Dashboard** tab on the right panel to see real-time metrics:

- **Counter Tiles** - Submitted, Success, Failed, Throttled message counts
- **TPS Metrics** - Current, Average, and Peak TPS with success rate
- **TPS Chart** - Live line chart of throughput over time (auto-scrolling 60s window)
- **Latency Percentiles** - P50, P95, P99, Average, Min, and Max response times
- **Result Distribution** - Pie chart of success vs failure ratio

The **Message Log** tab shows a real-time table of individual PDU messages with:
- Timestamp, Direction (IN/OUT/ERR), PDU Type, Message ID, Source, Destination, Status
- Text filter and direction filter (All/OUT/IN/ERR)
- Auto-scroll toggle and message count

The **Status Bar** at the bottom shows connection state, current TPS, progress, and elapsed time.

### 7. Export Reports

Export test results from the **Dashboard** toolbar buttons or from the **File** menu:

| Format | Description |
|---|---|
| **CSV** | Sectioned spreadsheet with summary metrics and TPS time-series data |
| **JSON** | Structured data with counters, latency, TPS history, and DLR stats |
| **HTML** | Self-contained dark-themed report with embedded SVG TPS chart, CSS pie chart, latency bar chart, and full metrics table. Opens in any browser with no external dependencies. |

### 8. Save/Load Profiles

Save your complete test configuration (connection, load test, DLR, template) as a reusable YAML profile:

- **Ctrl+S** or **File > Save Profile** - Save current config to a `.yaml` file
- **Ctrl+O** or **File > Load Profile** - Load a previously saved profile

## Keyboard Shortcuts

| Shortcut | Action |
|---|---|
| `F5` | Start load test |
| `F6` | Pause / Resume |
| `F7` | Stop load test |
| `Ctrl+O` | Load profile |
| `Ctrl+S` | Save profile |
| `Alt+F4` | Exit |

## Project Structure

```
SMPPulse/
├── pom.xml
└── src/main/
    ├── java/com/smppulse/
    │   ├── app/                  # Launcher, Application, AppContext
    │   ├── config/               # POJOs, ProfileManager, YAML serialization
    │   ├── core/
    │   │   ├── session/          # SMPP session management, reconnect, TLS
    │   │   └── codec/            # PDU recording and logging
    │   ├── dlr/                  # DLR tracking, handling, generation, SMSC simulator
    │   ├── generator/            # Load engine, rate limiter, virtual threads
    │   ├── param/                # Template parser, resolvers, data sources
    │   ├── metrics/              # Counters, histograms, TPS tracking, export
    │   ├── ui/
    │   │   ├── controller/       # JavaFX FXML controllers
    │   │   └── component/        # Custom UI components (gauges, tiles)
    │   └── util/                 # SMPP utils, validation, FX thread helpers
    └── resources/
        ├── view/                 # FXML layout files
        ├── css/dark-theme.css    # Dark theme stylesheet
        ├── default-profile.yaml  # Default configuration
        └── logback.xml           # Logging configuration
```

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                     JavaFX GUI Layer                     │
│  ┌─────────────┐  ┌────────────┐  ┌───────────────────┐ │
│  │ Config Tabs  │  │ Dashboard  │  │   Message Log     │ │
│  └──────┬──────┘  └─────┬──────┘  └─────────┬─────────┘ │
├─────────┼───────────────┼────────────────────┼───────────┤
│         │          AppContext                 │           │
│         ▼               ▼                    ▼           │
│  ┌────────────┐  ┌────────────┐  ┌───────────────────┐  │
│  │  Profile    │  │  Metrics   │  │    PDU Logger     │  │
│  │  Manager    │  │ Collector  │  │   (10K buffer)    │  │
│  └────────────┘  └─────┬──────┘  └───────────────────┘  │
│                        │                                 │
│  ┌────────────────────────────────────────────────────┐  │
│  │               Load Engine                          │  │
│  │  ┌───────────┐  ┌─────────────┐  ┌──────────────┐ │  │
│  │  │   Rate    │  │   Virtual   │  │   Message    │ │  │
│  │  │  Limiter  │  │   Thread    │  │  Submitter   │ │  │
│  │  │  (token   │  │  Executor   │  │   (tasks)    │ │  │
│  │  │  bucket)  │  │             │  │              │ │  │
│  │  └───────────┘  └─────────────┘  └──────┬───────┘ │  │
│  └─────────────────────────────────────────┼─────────┘  │
│                                            │             │
│  ┌──────────────────┐  ┌──────────────────┼───────────┐ │
│  │ Parameter Engine  │  │ Session Manager  │           │ │
│  │ (template         │  │  ┌──────────────▼─────────┐ │ │
│  │  resolution)      │  │  │ SmppSessionWrapper     │ │ │
│  └──────────────────┘  │  │ (jSMPP 3.0.0)          │ │ │
│                         │  └────────────────────────┘ │ │
│  ┌──────────────────┐  └─────────────────────────────┘  │
│  │  DLR Subsystem   │                                   │
│  │  Tracker/Handler ├──────────────────▶ SMPP Server    │
│  │  Generator/SMSC  │                                   │
│  └──────────────────┘                                   │
└──────────────────────────────────────────────────────────┘
```

## Technology Stack

| Component | Technology | Version |
|---|---|---|
| Language | Java (virtual threads, preview features) | 21 |
| GUI Framework | JavaFX + FXML | 21.0.2 |
| SMPP Protocol | jSMPP | 3.0.0 |
| Configuration | SnakeYAML | 2.2 |
| JSON Export | Jackson Databind | 2.16.1 |
| CSV Export | OpenCSV | 5.9 |
| Logging | SLF4J + Logback | 2.0.11 / 1.4.14 |
| Build | Maven + Shade Plugin | 3.8+ |

## Configuration Reference

### Profile YAML Format

```yaml
name: My Test Profile
description: Example load test configuration
connection:
  host: smpp.example.com
  port: 2775
  systemId: myesme
  password: secret
  bindType: TRANSCEIVER        # TRANSMITTER | RECEIVER | TRANSCEIVER
  connectionMode: ESME         # ESME | SMSC
  sessionCount: 4
  windowSize: 50
  useTls: false
  enquireLinkInterval: 30000   # ms
  responseTimeout: 10000       # ms
  retryPolicy:
    autoReconnect: true
    initialDelayMs: 1000
    maxDelayMs: 60000
    backoffMultiplier: 2.0
    maxRetries: -1             # -1 = unlimited
loadTest:
  targetTps: 500
  rampUpSeconds: 10
  rampDownSeconds: 5
  durationSeconds: 300
  totalMessages: 0             # 0 = use duration
  operationType: SUBMIT_SM     # SUBMIT_SM | DATA_SM
  maxInFlight: 5000
messageTemplate:
  sourceAddress: '${random.digits(10)}'
  destinationAddress: '${random.digits(10)}'
  messageText: 'Load test ${sequence} at ${timestamp}'
  tlvValues: {}
dlr:
  requestDlr: true
  registeredDelivery: 1
  dlrTimeoutSeconds: 60
  generateDlr: true
  dlrDelayMs: 1000
  dlrSuccessRate: 0.95
```

## Logging

Logs are written to both console and `logs/smppulse.log` (rolling, 10MB max, 5 files retained). Configure verbosity in `src/main/resources/logback.xml`. The jSMPP library is set to WARN level by default to reduce noise.

## License

This project is provided as-is for internal testing and development purposes.
