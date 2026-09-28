# 🚗 Smart Parking System — IoT & Automated Gate Management System

An IoT-powered smart campus parking management and automated barrier control system built using **ESP32 microcontrollers**, **HC-SR04 ultrasonic sensors**, and a **Java Spring Boot (MVC)** web backend.

---

## 1. Project Overview

The **Smart Parking System** provides automated campus access control, real-time parking slot occupancy monitoring, and driver parking session tracking. The system seamlessly integrates embedded IoT sensors with a robust Spring Boot backend and dynamic Thymeleaf web dashboards for both drivers and security personnel.

### Key Objectives
* **Automated & Remote Access Control:** Automated entrance and exit barrier gate operations triggered via ESP32 microcontrollers or overridden remotely by security guards.
* **Real-Time Occupancy Telemetry:** Continuous ultrasonic distance measurements from 8 campus parking slots mapped dynamically to database state.
* **Driver Session Management:** Tracking driver entries, parking slot allocations, active sessions, and departures.
* **Live Dashboards:** Public Driver Dashboard showing real-time slot availability and Security Guard Dashboard for override gate controls.

---

## 2. Main Features

The system implements 8 core integrated modules (Issues #1–#8):

- **Driver Dashboard View (Issue #1):** Public Thymeleaf dashboard rendering an 8-slot campus parking grid categorized by user role (Undergraduates, Lecturers, Visitors, etc.).
- **Real-Time Polling Script (Issue #2):** Asynchronous JavaScript poller querying backend slot statuses every 5 seconds to update UI badges dynamically without page refreshes.
- **Security Guard Barrier Control (Issue #3):** Live security dashboard equipped with interactive barrier visualizers, status lights, vehicle animation loops, and light/dark theme toggling.
- **Custom CSS Enhancements (Issue #4):** Custom theme styling system ([dashboard.css](file:///d:/Year%203%20-%20Semester%201/Enterprise%20Application%20Development/Smart-Parking-System/server/src/main/resources/static/css/dashboard.css)) supporting dark/light mode tokens, smooth transitions, status pills, and responsive grid layouts.
- **Parking & Slot Management API (Issue #5):** Full RESTful CRUD endpoints for parking slot configuration, category assignment, and manual occupancy updates backed by H2/MySQL database persistence.
- **Real-Time Occupancy & ESP32 Sensor Integration (Issue #6):** Ingestion of telemetry from HC-SR04 ultrasonic distance sensors with distance-threshold processing (50 cm threshold) and active session state protection.
- **Security Guard Gate & Barrier API (Issue #7):** RESTful APIs for remote barrier gate control (`/api/gates/...`) with ESP32 device health checks, timeout resilience, and mock-mode simulation support.
- **Driver & Parking Session Management (Issue #8):** Complete driver registration, entry session creation, active session tracking, and exit session completion with slot release logic.

---

## 3. Technology Stack

### Backend & Core
* **Language:** Java 17
* **Framework:** Spring Boot 3.x (Spring MVC, Spring Data JPA)
* **Database:** H2 In-Memory Database / MySQL
* **Build System:** Apache Maven

### Frontend
* **Templating:** Thymeleaf, HTML5
* **Styling:** Vanilla CSS3 (Custom CSS Tokens, Dark/Light Mode), Bootstrap 5 CDN
* **Logic:** Modern JavaScript (ES6+ `fetch` API, DOM manipulation)

### Embedded / IoT Hardware
* **Microcontroller:** ESP32 DevKit V1
* **Sensors:** HC-SR04 Ultrasonic Distance Sensors (Range: 2–400 cm)
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
   +------------------------------------------------------+
   |             ESP32 Microcontroller Layer              |
   |                                                      |
   |  +-----------------------+   +--------------------+  |
   |  |  HC-SR04 Ultrasonic   |   |   Servo Barrier    |  |
   |  |  Distance Sensors     |   |   Actuators        |  |
   |  +-----------------------+   +--------------------+  |
   +------------------------------------------------------+
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
Manages parking slot metadata across 8 campus categories (Undergraduates, Lecturers, Visitors, etc.), allowing slot queries, creation, updates, and occupancy state manipulation.

### 2. Driver & Parking Session Management (`ParkingSessionController`, `ParkingSessionService`)
Handles vehicle arrival and departure lifecycle. Validates driver credentials, assigns available slots upon entry, creates `ACTIVE` sessions, and releases slots upon session exit completion.

### 3. Real-Time Telemetry Processing (`SensorOccupancyController`, `SensorOccupancyService`)
Processes distance telemetry sent from ESP32 HC-SR04 ultrasonic sensors. Calculates physical slot occupancy against a 50 cm threshold while preventing active sessions from being accidentally cleared by sensor noise.

### 4. Gate & Barrier Override Control (`BarrierController`, `BarrierService`)
Controls automated entrance and exit barrier gates. Features real-time gate state querying, manual override commands (`OPEN`/`CLOSE`), ESP32 health checks, and mock-mode simulation.

---

## 7. Frontend Views

### Driver Dashboard (`GET /driver`)
- Public, responsive 8-slot parking grid layout.
- Summary counter cards displaying available vs occupied slot counts.
- Embedded JavaScript poller fetching live updates every 5 seconds from `GET /api/parking/status`.

### Security Guard Dashboard (`GET /security-guard`)
- Guard override control station featuring single-gate visualizers, state badges, signal lights, and vehicle animation loop.
- Live status poller fetching barrier states from `GET /api/gates/status` every 3 seconds.
- Manual trigger buttons to open/close Entrance and Exit barriers asynchronously using [security-gate.js](file:///d:/Year%203%20-%20Semester%201/Enterprise%20Application%20Development/Smart-Parking-System/server/src/main/resources/static/js/security-gate.js).

---

## 8. ESP32 & Sensor Integration

### Hardware Configuration
* **Ultrasonic Distance Measurement:** HC-SR04 sensors measure vehicle distance in centimeters (2–400 cm).
* **Occupancy Decision Rule:**
  - `distance <= 50.0 cm` ──> **OCCUPIED**
  - `distance > 50.0 cm` ──> **AVAILABLE** (unless protected by an `ACTIVE` parking session).
* **Communication Protocol:** HTTP POST REST payloads (`application/json`) sent to `/api/sensors/occupancy`.

### ESP32 Gate Control Protocol
* **Control Endpoint:** `POST {esp32.base-url}/api/barrier/control?gate={gate}&action={action}`
* **Resilience Settings:** Connect timeout (3000ms), read timeout (3000ms), and configurable mock mode (`esp32.mock-mode-enabled=true`).

---

## 9. API Documentation

### Parking Slot APIs (`ParkingSlotController`)
| Method | Endpoint | Description | Status Code |
|---|---|---|---|
| `GET` | `/api/parking/status` | Real-time parking slot statuses for poller | `200 OK` |
| `GET` | `/api/parking/slots` | Retrieves list of all parking slots | `200 OK` |
| `GET` | `/api/parking/slots/{id}` | Retrieves a single parking slot by ID | `200 OK` / `404 Not Found` |
| `POST` | `/api/parking/slots` | Creates a new parking slot | `201 Created` |
| `PUT` | `/api/parking/slots/{id}` | Updates parking slot details | `200 OK` / `404 Not Found` |
| `DELETE` | `/api/parking/slots/{id}` | Deletes a parking slot | `204 No Content` |
| `PUT` | `/api/parking/slots/{id}/occupancy` | Updates slot occupancy state | `200 OK` |

### Sensor Telemetry API (`SensorOccupancyController`)
| Method | Endpoint | Request Body | Status Code |
|---|---|---|---|
| `POST` | `/api/sensors/occupancy` | `{"slotId": 1, "distance": 12.5}` | `200 OK` / `400 Bad Request` |

### Barrier Gate Control APIs (`BarrierController`)
| Method | Endpoint | Description | Status Code |
|---|---|---|---|
| `GET` | `/api/gates/status` | Fetches Entrance & Exit barrier states and ESP32 health | `200 OK` |
| `POST` | `/api/gates/entry/open` | Opens the Entrance barrier gate | `200 OK` / `502 Bad Gateway` |
| `POST` | `/api/gates/entry/close` | Closes the Entrance barrier gate | `200 OK` / `502 Bad Gateway` |
| `POST` | `/api/gates/exit/open` | Opens the Exit barrier gate | `200 OK` / `502 Bad Gateway` |
| `POST` | `/api/gates/exit/close` | Closes the Exit barrier gate | `200 OK` / `502 Bad Gateway` |
| `POST` | `/api/gates/control` | Control endpoint with JSON `{"gate":"ENTRANCE","action":"OPEN"}` | `200 OK` / `400 Bad Request` |

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

* **Startup Seeding:** [DatabaseInitializer.java](file:///d:/Year%203%20-%20Semester%201/Enterprise%20Application%20Development/Smart-Parking-System/server/src/main/java/com/smartparking/backend/config/DatabaseInitializer.java) automatically seeds Slots 1–8 at startup if the database is empty.

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
   cd server
   ```

---

## 12. Configuration (`application.properties`)

Configuration settings located in `server/src/main/resources/application.properties`:

```properties
# Server Configuration
server.port=8080

# Database Configuration (H2 In-Memory)
spring.datasource.url=jdbc:h2:mem:smartparking;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true
spring.jpa.hibernate.ddl-auto=update

# ESP32 Communication & Mock Settings
esp32.base-url=http://192.168.1.100
esp32.connect-timeout-ms=3000
esp32.read-timeout-ms=3000
esp32.mock-mode-enabled=true

# Parking Sensor Telemetry Settings
parking.sensor.occupancy-threshold-cm=50.0
parking.sensor.max-distance-cm=400.0
parking.sensor.min-slot-number=1
parking.sensor.max-slot-number=8
```

---

## 13. Running the Application

### Build & Run Commands
```bash
# Build project and run tests
mvn clean test

# Run Spring Boot Application
mvn spring-boot:run
```

Once started, access the dashboards in your browser:
* **Driver Dashboard:** `http://localhost:8080/driver`
* **Security Guard Dashboard:** `http://localhost:8080/security-guard`
* **H2 Database Console:** `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:smartparking`)

---

## 14. Testing

### Automated Test Suite
The project maintains 50 comprehensive unit and integration tests across controller, service, and repository layers:

```bash
mvn test
```

**Test Execution Summary:**
- `BackendApplicationTests`: Spring application context loading
- `BarrierControllerTest`: Barrier REST controller mappings and 502/400 exception mappings
- `BarrierServiceTest`: Barrier state transitions, repeated requests, gate independence, and mock mode
- `ParkingSessionIntegrationTest`: Driver entry/exit workflows, slot reservation, and session history
- `SensorOccupancyIntegrationTest`, `SensorOccupancyControllerTest`, `SensorOccupancyServiceTest`: Telemetry ingestion, threshold calculation, 1:1 sensor-slot mapping, and active session overrides

**Verification Results:** `Tests run: 50, Failures: 0, Errors: 0, Skipped: 0` (100% Pass Rate).

---

## 15. Git Workflow

Development follows a structured branch workflow:
- `main`: Production release branch.
- `develop`: Primary integration branch for feature pull requests.
- `feature/*`: Dedicated feature development branches (e.g., `feature/security-guard`, `feature/backend/Parking-&-slot-management-API`).

---

## 16. Completed Issues Summary

- [x] **Issue #1 — Driver Dashboard View:** HTML/Thymeleaf 8-slot display.
- [x] **Issue #2 — Real-Time Polling Script:** 5-second polling mechanism updating slot cards.
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