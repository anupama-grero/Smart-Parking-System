# 🚗 Smart Parking System — IoT & Automated Gate Management System

An IoT-powered smart parking system with automated gate control, parking slot monitoring, and integrated hardware components. Built using **ESP32 microcontrollers**, **HC-SR04 ultrasonic sensors**, and a **Java Spring Boot (MVC)** web backend.

---

## 1. Project Overview

The **Smart Parking System** provides automated gate access control, real-time parking slot occupancy monitoring, and driver parking session tracking. The system integrates embedded IoT sensors with a robust Spring Boot backend and dynamic Thymeleaf web dashboards for both drivers and security personnel.

### Hardware Configuration
* **4 Parking Slots:** One ultrasonic sensor per slot
* **2 Ultrasonic Gate Sensors:** Entry and exit detection operate the shared gate
* **Total Ultrasonic Sensors:** 6 (4 parking + 2 gate)
* **1 Servo Motor:** Controls the barrier gate mechanism
* **Active Buzzer & Lights:** Audio/visual alerts for events
* **OLED Display:** Shows all four slot states and PARKING FULL

### Key Objectives
* **Automated & Remote Access Control:** Automated barrier gate operations controlled via ESP32 microcontrollers or remotely by security guards.
* **Real-Time Occupancy Telemetry:** Continuous monitoring of the four physical parking slots.
* **Driver Session Management:** Tracking driver entries, parking slot allocations, active sessions, and departures.
* **Live Dashboards:** Public Driver Dashboard showing real-time slot availability and Security Guard Dashboard for gate controls.

---

## 2. Main Features

The system implements 8 core integrated modules (Issues #1–#8):

- **Driver Dashboard View (Issue #1):** Public Thymeleaf dashboard rendering a 4-slot parking grid showing all available parking slots.
- **Real-Time Polling Script (Issue #2):** Asynchronous JavaScript poller querying backend slot status every 3 seconds.
- **Security Guard Barrier Control (Issue #3):** Live security dashboard equipped with barrier visualizers, status lights, vehicle animation loops, and light/dark theme toggling.
- **Custom CSS Enhancements (Issue #4):** Custom theme styling system ([dashboard.css](./server/server/src/main/resources/static/css/dashboard.css)) supporting dark/light mode tokens, smooth transitions, status pills, and responsive grid layouts.
- **Parking & Slot Management API (Issue #5):** Full RESTful CRUD endpoints for parking slot configuration, manual occupancy updates, and status monitoring backed by H2/MySQL database persistence.
- **Real-Time Occupancy & ESP32 Sensor Integration (Issue #6):** Ingestion of telemetry from HC-SR04 ultrasonic distance sensors with firmware-aligned occupancy (5 cm) and reverse-warning (3 cm) thresholds plus active session state protection.
- **Security Guard Gate & Barrier API (Issue #7):** Token-protected gate requests sent over the firmware-supported USB serial commands, with explicit pending/confirmed/failed results.
- **Driver & Parking Session Management (Issue #8):** Complete driver registration, entry session creation, active session tracking, and exit session completion with slot release logic.

---

## 3. Technology Stack

### Backend & Core
* **Language:** Java 17
* **Framework:** Spring Boot 4.1.1 (Spring MVC, Spring Data JPA)
* **Database:** H2 In-Memory Database / MySQL
* **Build System:** Apache Maven

### Frontend
* **Templating:** Thymeleaf, HTML5
* **Styling:** Vanilla CSS3 (Custom CSS Tokens, Dark/Light Mode), Bootstrap 5 CDN
* **Logic:** Modern JavaScript (ES6+ `fetch` API, DOM manipulation)

### Embedded / IoT Hardware
* **Microcontroller:** ESP32 DevKit V1
* **Sensors:** HC-SR04 Ultrasonic Distance Sensors (Range: 2–400 cm)
* **Total Sensors:** 6 (4 parking slots + 2 gate sensors)
* **Actuators:** SG90 / MG90S Micro Servos (Gate Barrier Arms)
* **Communication:** HTTP REST API (JSON payloads over WiFi)

---

## 4. System Architecture

```text
                                 +-------------------------+
                                 |   Security Guard &      |
                                 |   Driver Dashboards     |
                                 +------------+------------+
                                              |
                                              | HTTP REST / Web Polling
                                              v
+-----------------------------------------------------------------------------------+
|                            SPRING BOOT MVC BACKEND                                |
|                                                                                   |
|  +---------------------------+  +------------------------+  +------------------+  |
|  |        Controllers        |  |        Services        |  |   Repositories   |  |
|  | - BarrierController       |  | - BarrierService       |  | - SlotRepository |  |
|  | - DriverDashboardController| | - SensorOccupancySvc   |  | - DriverRepository| |
|  | - SecurityGuardController |  | - ParkingSessionSvc    |  | - SessionRepo    |  |
|  | - SensorOccupancyCtrl     |  | - ParkingSlotSvc       |  +--------+---------+  |
|  | - ParkingSessionCtrl      |  +-----------+------------+           |            |
|  | - ParkingSlotCtrl         |              |                        | JPA        |
|  +-------------+-------------+              |                        v            |
+----------------|----------------------------|-----------------+-------------+-----+
                 |                            |                 | Database    |
                 |                            |                 | (H2/MySQL)  |
                 |                            |                 +-------------+
                 v                            v
    +-----------------------------------------------------+
    |             ESP32 Microcontroller Layer             |
    |                                                     |
    |  +---------------------+-----------------------+     |
    |  | 4x HC-SR04 Parking  |  2x HC-SR04 Gate      |     |
    |  |    Slot Sensors    |  |    Sensors          |     |
    |  +---------------------+  +-----------+---------+     |
    |  |    Servo Motor      |             |               |
    |  |   (Gate Control)    |             v               |
    |  +-----------+---------+  +--------+--------+        |
    |            |             |  Active Buzzer   |        |
    |            |             |  & LED Lights    |        |
    |            +-------------+-----------------+        |
    |  |    OLED Display      |                        |
    |  +---------------------+-----------------------+  |
    +-----------------------------------------------------+
    ```


---

## 5. Project Structure

```text
Smart-Parking-System/
├── README.md
├── .gitignore
└── server/
    ├── pom.xml
    ├── mvnw
    ├── mvnw.cmd
    └── src/
        ├── main/
        │   ├── java/com/smartparking/backend/
        │   │   ├── BackendApplication.java
        │   │   │
        │   │   ├── config/
        │   │   │   └── DatabaseInitializer.java
        │   │   │
        │   │   ├── controller/
        │   │   │   ├── BarrierController.java
        │   │   │   ├── DriverDashboardController.java
        │   │   │   ├── ParkingSessionController.java
        │   │   │   ├── ParkingSlotController.java
        │   │   │   ├── SecurityGuardController.java
        │   │   │   └── SensorOccupancyController.java
        │   │   │
        │   │   ├── dto/
        │   │   │   ├── ApiResponse.java
        │   │   │   ├── CreateSessionRequest.java
        │   │   │   ├── GateControlRequest.java
        │   │   │   ├── GateStatusResponse.java
        │   │   │   ├── SensorOccupancyRequest.java
        │   │   │   └── SensorOccupancyResponse.java
        │   │   │
        │   │   ├── exception/
        │   │   │   ├── Esp32CommunicationException.java
        │   │   │   ├── Esp32UnavailableException.java
        │   │   │   ├── GlobalExceptionHandler.java
        │   │   │   ├── InvalidGateException.java
        │   │   │   └── ResourceNotFoundException.java
        │   │   │
        │   │   ├── model/
        │   │   │   ├── Driver.java
        │   │   │   ├── GateAction.java
        │   │   │   ├── GateStatus.java
        │   │   │   ├── GateType.java
        │   │   │   ├── ParkingSession.java
        │   │   │   ├── ParkingSlot.java
        │   │   │   └── SessionStatus.java
        │   │   │
        │   │   ├── repository/
        │   │   │   ├── DriverRepository.java
        │   │   │   ├── ParkingSessionRepository.java
        │   │   │   └── ParkingSlotRepository.java
        │   │   │
        │   │   └── service/
        │   │       ├── BarrierService.java
        │   │       ├── ParkingSessionService.java
        │   │       ├── ParkingSlotService.java
        │   │       ├── SensorOccupancyService.java
        │   │       └── SensorSlotMapper.java
        │   │
        │   └── resources/
        │       ├── application.properties
        │       ├── static/
        │       │   ├── css/
        │       │   │   └── dashboard.css
        │       │   └── js/
        │       │       └── security-gate.js
        │       └── templates/
        │           ├── driver-dashboard.html
        │           └── security-guard.html
        │
        └── test/java/com/smartparking/backend/
            ├── BackendApplicationTests.java
            ├── ParkingSessionIntegrationTest.java
            ├── SensorOccupancyIntegrationTest.java
            ├── controller/
            │   ├── BarrierControllerTest.java
            │   └── SensorOccupancyControllerTest.java
            └── service/
                ├── BarrierServiceTest.java
                └── SensorOccupancyServiceTest.java
```

---

## 6. Backend Modules

### 1. Parking Slot Management (`ParkingSlotController`, `ParkingSlotService`)
Manages metadata for the four physical parking slots. Obsolete rows, if present in an existing database, are preserved but excluded from the live status API and dashboards.

### 2. Driver & Parking Session Management (`ParkingSessionController`, `ParkingSessionService`)
Handles vehicle arrival and departure lifecycle. Validates driver credentials, assigns available slots upon entry, creates `ACTIVE` sessions, and releases slots upon session exit completion.

### 3. Real-Time Telemetry Processing (`SensorOccupancyController`, `SensorOccupancyService`)
Processes distance telemetry sent from ESP32 HC-SR04 ultrasonic sensors. Calculates physical slot occupancy against a 5 cm threshold and activates the Slot 2 reverse warning at 3 cm, while preventing active sessions from being accidentally cleared by sensor noise.

### 4. Gate & Barrier Override Control (`BarrierController`, `BarrierService`)
Queues existing `OPEN`/`CLOSE` serial commands and confirms them only when the firmware reports a matching gate state. No simulated success is returned when hardware is unavailable.

---

## 7. Frontend Views

### Driver Dashboard (`GET /driver`)
- Public, responsive 4-slot parking grid layout showing all available parking slots.
- Summary counter cards displaying available vs occupied slot counts.
- Embedded JavaScript poller fetching live updates every 3 seconds from `GET /api/parking/status`.

### Security Guard Dashboard (`GET /security-guard`)
- Guard override control station featuring single-gate visualizers, state badges, signal lights, and vehicle animation loop.
- Live status poller fetching barrier states from `GET /api/gates/status` every 3 seconds.
- Manual trigger buttons to open/close barriers asynchronously using [security-gate.js](./server/server/src/main/resources/static/js/security-gate.js).

---

## 8. ESP32 & Sensor Integration

### Hardware Configuration
* **4 Parking Slots:** Standard parking slot monitoring via ultrasonic sensors (1 sensor per slot)
* **2 Ultrasonic Sensors:** One for entrance gate detection, one for exit gate detection
* **Total Ultrasonic Sensors:** 6 (4 for parking + 2 for gate)
* **1 Servo Motor:** Controls the barrier gate mechanism
* **Active Buzzer & Lights:** Audio/visual alerts for events
* **OLED Display:** Shows four slot states, capacity, and PARKING FULL
* **Occupancy Decision Rule:**
  - `distance <= 5.0 cm` ──> **OCCUPIED**
  - `distance > 5.0 cm` ──> **AVAILABLE** (unless protected by an `ACTIVE` parking session).
  - `distance = -1` or `null` ──> **UNKNOWN / retain last valid state**.
* **Communication Protocol:** HTTP POST JSON batches to `/api/sensors/occupancy/batch`.

### ESP32 Gate Control Protocol
The active four-slot firmware does not poll HTTP for gate commands. The backend sends only its supported `OPEN`, `CLOSE`, and `STATUS` serial commands at 115200 baud. Gate status remains UNKNOWN until the firmware reports it; a command starts as PENDING and is never represented as a physical success merely because HTTP succeeded.

### USB Serial Cable Sketch Gate Sync
Run `start-system.bat` from the repository root. The selected unchanged firmware has a fixed `SERVER_URL` of `http://192.168.29.59:8080`; this workstation must be reachable at that address (for example, using a DHCP reservation/static IP or an existing network route). This environment currently does not own that IP, so ESP32 Wi-Fi telemetry cannot reach the backend until the network is arranged accordingly. Do not edit the firmware. Its four-slot occupancy batches are sent every second once the fixed address is reachable.

Connect the ESP32 to the backend computer using its USB serial adapter, then set `ESP32_SERIAL_PORT` (for example, `COM3`) and restart the backend. The baud rate is `115200`. Set `ESP32_GATE_CONTROL_TOKEN` in the backend environment and enter that token in the guard dashboard. Gate commands remain disabled if either the serial adapter or token is unavailable.

The firmware controls one shared servo/gate. Its exit sensor can open that gate even when parking is full; the backend does not provide a separate manual exit command because the firmware protocol cannot target an exit gate independently. Entry OPEN commands are blocked by the backend when full or when any slot lacks fresh readings, and the unchanged firmware remains the final full-capacity authority. The server never sends gate commands as a side effect of dashboard polling.

---

## 9. API Documentation

### Parking Slot APIs (`ParkingSlotController`)
| Method | Endpoint | Description | Status Code |
|---|---|---|---|
| `GET` | `/api/parking/status` | Four slot states, counts, fullness, sensor freshness and last update | `200 OK` |
| `GET` | `/api/parking/slots` | Retrieves list of all parking slots | `200 OK` |
| `GET` | `/api/parking/slots/{id}` | Retrieves a single parking slot by ID | `200 OK` / `404 Not Found` |
| `POST` | `/api/parking/slots` | Creates a new parking slot | `201 Created` |
| `PUT` | `/api/parking/slots/{id}` | Updates parking slot details | `200 OK` / `404 Not Found` |
| `DELETE` | `/api/parking/slots/{id}` | Deletes a parking slot | `204 No Content` |
| `PUT` | `/api/parking/slots/{id}/occupancy` | Updates slot occupancy state | `200 OK` |

### Sensor Telemetry API (`SensorOccupancyController`)
| Method | Endpoint | Request Body | Status Code |
|---|---|---|---|
| `POST` | `/api/sensors/occupancy/batch` | Four-slot telemetry payload shown below | `200 OK` / `400 Bad Request` |
| `POST` | `/api/sensors/occupancy` | `{"slotId":1,"distance":12.5}` (single-reading/testing path) | `200 OK` / `400 Bad Request` |

The batch endpoint accepts the unchanged firmware's four-slot request shape:

```json
{
  "availableSlots": 3,
  "occupiedSlots": 1,
  "slots": [
    {"slotNumber": 1, "occupied": true,  "distanceCm": 4.2},
    {"slotNumber": 2, "occupied": false, "distanceCm": 110.0},
    {"slotNumber": 3, "occupied": false, "distanceCm": -1},
    {"slotNumber": 4, "occupied": false, "distanceCm": 95.0}
  ]
}
```

The backend derives occupancy from the firmware-aligned `<= 5 cm` threshold;
the counts and `occupied` flags in the request are informational. `-1` or
`null` readings preserve the last valid state. The parking status response
includes `slots`, `occupiedSlots`, `availableSlots`, `unknownSlots`, `full`,
`hardwareConnected`, `lastSuccessfulUpdate`, and `stale`.

### Barrier Gate Control APIs (`BarrierController`)
| Method | Endpoint | Description | Status Code |
|---|---|---|---|
| `GET` | `/api/gates/status` | Fetches Barrier state and ESP32 health | `200 OK` |
| `POST` | `/api/gates/entry/status?state=OPEN` | Compatibility endpoint for manually reported entrance state; active ESP32 state arrives over USB serial | `200 OK` |
| `POST` | `/api/gates/entry/open` | Requests serial OPEN; requires `X-Gate-Control-Token` | `200 PENDING`, `409` full, `502` disconnected |
| `POST` | `/api/gates/entry/close` | Requests serial CLOSE; requires `X-Gate-Control-Token` | `200 PENDING`, `502` disconnected |
| `POST` | `/api/gates/exit/open` | Not supported as a separate remote action by this firmware | `400 Bad Request` |
| `GET` | `/api/system/health` | Backend, database, Wi-Fi telemetry and USB serial health | `200` / `503` |

Example entry OPEN request:

```http
POST /api/gates/entry/open
X-Gate-Control-Token: <ESP32_GATE_CONTROL_TOKEN>
```

The initial `200` response means the command was queued, not that the barrier
physically opened. Check `commandState` (`PENDING`, `CONFIRMED`, `REJECTED`, or
`FAILED`), `entryGate`, and `gateStateConfirmed` via `GET /api/gates/status`.
The token is required for every gate-control POST; the guard page holds it only
in memory for its current session.

### Driver Parking Session APIs (`ParkingSessionController`)
| Method | Endpoint | Request / Parameters | Status Code |
|---|---|---|---|
| `POST` | `/api/parking-sessions/entry` | `{"licensePlate":"ABC-1234","driverName":"John","phone":"0771234567","slotNumber":1}` | `201 Created` / `400 Bad Request` |
| `PUT` | `/api/parking-sessions/exit/{id}` | Path variable `sessionId` | `200 OK` / `404 Not Found` |
| `GET` | `/api/parking-sessions/{id}` | Path variable `id` | `200 OK` / `404 Not Found` |
| `GET` | `/api/parking-sessions/active` | Retrieves all active parking sessions | `200 OK` |
| `GET` | `/api/parking-sessions/history` | Retrieves full session history | `200 OK` |

---

## 10. Database Schema & Persistence

The application uses **Spring Data JPA** with an **H2 In-Memory Database** (or configurable MySQL database):

```text
+-------------------+        +----------------------+        +--------------------+
|      Driver       |        |    ParkingSession    |        |    ParkingSlot     |
+-------------------+        +----------------------+        +--------------------+
| id (PK)           | 1    * | id (PK)              | *    1 | id (PK)            |
| driverId (Unique) |--------| driver_id (FK)       |--------| slotNumber(Unique) |
| name              |        | parking_slot_id (FK) |        | category           |
| licensePlate      |        | startTime            |        | isOccupied         |
+-------------------+        | endTime              |        | lastDistanceCm     |
                             | status (ACTIVE/DONE) |        | lastSensorUpdate   |
                             +----------------------+        +--------------------+
```

* **Startup Seeding:** [DatabaseInitializer.java](./server/server/src/main/java/com/smartparking/backend/config/DatabaseInitializer.java) automatically seeds Slots 1–4 at startup if the database is empty.

---

## 11. Installation & Setup

### Prerequisites
- **Java Development Kit (JDK):** Version 17 or higher
- **Build Tool:** Apache Maven (or embedded Maven Wrapper `./mvnw`)

### Installation Steps
1. **Clone the repository:**
   ```bash
   git clone https://github.com/anupama-grero/Smart-Parking-System.git
   cd Smart-Parking-System
   ```
2. **Navigate to the server directory:**
   ```bash
   cd server/server
   ```

---

## 12. Configuration (`application.properties`)

Configuration settings are in `server/server/src/main/resources/application.properties`. Override deployment-specific values with environment variables:

```properties
# Server Configuration
server.port=${SERVER_PORT:8080}

# Database Configuration (H2 In-Memory)
spring.datasource.url=${DB_URL:jdbc:h2:mem:smartparkingdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE}
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true
spring.jpa.hibernate.ddl-auto=update

# USB serial gate-control settings are supplied through the backend process environment:
# ESP32_SERIAL_PORT=COM3
# ESP32_SERIAL_BAUD_RATE=115200
# ESP32_SERIAL_COMMAND_TIMEOUT_MS=3000
# ESP32_SERIAL_CONNECTION_TIMEOUT_MS=10000
# ESP32_GATE_CONTROL_TOKEN=<use-a-long-random-secret>

# Parking Sensor Telemetry Settings
parking.sensor.occupancy-threshold-cm=5.0
parking.sensor.reverse-warning-threshold-cm=3.0
parking.sensor.max-distance-cm=400.0
parking.sensor.min-slot-number=1
parking.sensor.max-slot-number=4
parking.sensor.stale-after-seconds=10
```

For MySQL, set `DB_URL`, `DB_DRIVER=com.mysql.cj.jdbc.Driver`, `DB_USERNAME`,
`DB_PASSWORD`, and `JPA_DIALECT=org.hibernate.dialect.MySQLDialect`. Keep database
credentials and the gate-control token outside source control.

---

## 13. Running the Application

### Build & Run Commands
```bash
# Build project and run tests (from server/server)
.\mvnw.cmd clean test

# Run Spring Boot Application
.\mvnw.cmd spring-boot:run
```

Once started, access the dashboards in your browser:
* **Driver Dashboard:** `http://localhost:8080/driver`
* **Security Guard Dashboard:** `http://localhost:8080/security-guard`
* **H2 Database Console:** `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:smartparkingdb`)

---

## 14. Testing

### Automated Test Suite
The backend suite covers controller, service, database, sensor-batch, and disconnected-hardware behavior:

```bash
.\mvnw.cmd test
```

**Covered behavior:**
- `BackendApplicationTests`: Spring application context loading
- `BarrierControllerTest`: Barrier REST controller mappings and 502/400 exception mappings
- `BarrierServiceTest`: Pending serial commands, full/stale-data restrictions, and hardware disconnection
- `ParkingSessionIntegrationTest`: Driver entry/exit workflows, slot reservation, and session history
- `SensorOccupancyIntegrationTest`, `SensorOccupancyControllerTest`, `SensorOccupancyServiceTest`: Four-slot telemetry, 5 cm threshold, no-echo preservation, full-to-available transition, and duplicate rejection

---

## 15. Git Workflow

Development follows a structured branch workflow:
- `main`: Production release branch.
- `develop`: Primary integration branch for feature pull requests.
- `feature/*`: Dedicated feature development branches (e.g., `feature/security-guard`, `feature/backend/Parking-&-slot-management-API`).

---

## 16. Completed Issues Summary

- [x] **Issue #1 — Driver Dashboard View:** HTML/Thymeleaf 4-slot display.
- [x] **Issue #2 — Real-Time Polling Script:** 3-second polling mechanism updating slot cards from live four-slot status.
- [x] **Issue #3 — Security Guard Barrier Control:** Interactive visualizer dashboard.
- [x] **Issue #4 — Custom CSS Enhancements:** Styling tokens, dark/light themes, status pills.
- [x] **Issue #5 — Parking & Slot Management API:** RESTful slot CRUD and database persistence.
- [x] **Issue #6 — Real-Time Occupancy & Sensor Integration:** HC-SR04 telemetry processing.
- [x] **Issue #7 — Security Guard Gate & Barrier API:** Remote gate override REST endpoints.
- [x] **Issue #8 — Driver & Parking Session Management:** Driver arrival, session tracking, departure.

---

## 17. Team & Contributions

* **Group Leader:** System Architecture, Hardware Integration, Task Delegation & Documentation
* **Embedded Sub-Team:** Breadboard assembly, shared-trigger calibration & ESP32 firmware
* **Software Sub-Team:** Spring Boot MVC development, H2/MySQL persistence & Thymeleaf UI design