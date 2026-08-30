# 🚗 IoT Smart Parking & Automated Gate Management System

An IoT-powered smart campus parking management and automated barrier control system built using a single **ESP32**, **HC-SR04 ultrasonic sensors**, and a **Java Spring Boot (MVC)** web dashboard.

---

## 📌 Project Overview
This system provides real-time parking slot monitoring and automated access control:
* **Automated Entry & Exit Barriers:** Ultrasonic sensors detect approaching vehicles to trigger servo-operated barrier gates automatically.
* **Security Web Override:** A security guard dashboard with a single-click button to remotely lift the entry barrier via REST API polling.
* **Real-Time 8-Slot Occupancy Grid:** Tracks parking availability across 8 designated campus slots using a shared-trigger ultrasonic sensor setup.
* **Reverse Safety Assist (Demo Slot 4):** Distance-based safety alerts using an active buzzer and Red/Green LEDs to guide drivers during parking.
* **Entrance Live Display:** An I2C display (OLED SSD1306 / 16x2 LCD) at the gate showing real-time available and occupied slot counts.
* **Driver Web Dashboard:** A public, auto-refreshing MVC web view showing live slot statuses.

---

## 🏗️ System Architecture

                ┌─────────────────────────┐
                │      ESP32 DevKit       │
                └───────────┬─────────────┘
                            │
   ┌────────────────────────┼────────────────────────┐
   │                        │                        │
   ▼                        ▼                        ▼
[ Entry & Exit Gates ]    [ 8x Parking Slots ]    [ Status & Safety ]
• 2x HC-SR04 Sensors      • 8x HC-SR04 Sensors    • I2C Gate Display
• 2x Servo Barriers       (Shared Trig Pin 5)     • Red/Green LEDs
• Active Buzzer
│
▼ WiFi (HTTP REST / JSON)
┌─────────────────────────────────────┐
│      Spring Boot Backend (MVC)      │
├─────────────────────────────────────┤
│ • Model: JPA / Hibernate & MySQL    │
│ • View: Thymeleaf + Bootstrap 5     │
│ • Controller: Web & REST API Layers │
└─────────────────────────────────────┘


---

## 🅿️ Parking Categorization (8 Prototype Slots)
* **Slot 1:** Undergraduates
* **Slot 2:** Short Courses Students
* **Slot 3:** Visiting Lecturers
* **Slot 4:** Visitors (Reverse Safety Assist with Buzzer & LEDs)
* **Slot 5:** Lecturers
* **Slot 6:** Short Course Teachers
* **Slot 7:** Academic Staff
* **Slot 8:** Non-Academic Staff

---

## 🛠️ Hardware Requirements
* **Microcontroller:** 1x ESP32 DevKit V1 (30-pin or 38-pin)
* **Sensors:** 10x HC-SR04 Ultrasonic Distance Sensors (8 Slots + Entry + Exit)
* **Actuators:** 2x SG90 / MG90S Micro Servos (Gate Barriers)
* **Display:** 1x 0.96" I2C OLED (SSD1306) or 16x2 I2C LCD
* **Indicators & Alerts:**
  * 1x 5V Active Buzzer
  * 1x 5mm Red LED & 1x 5mm Green LED
  * 2x 220Ω Resistors
* **Power & Wiring:**
  * 2x 830-Point Solderless Breadboards (MB-102)
  * 1x 5V / 3A DC External Power Supply
  * 80x Female-to-Male (F-M) Jumper Wires
  * 40x Male-to-Male (M-M) Jumper Wires

---

## 📌 ESP32 GPIO Pin Allocation

| Component / Function | Pin Name | ESP32 GPIO |
| :--- | :--- | :--- |
| **All 10 Ultrasonic Sensors** | Shared `Trig` | **GPIO 5** |
| **Slot 1–8 Sensors** | `Echo 1` – `Echo 8` | **GPIO 13, 12, 14, 27, 26, 25, 33, 32** |
| **Entry Gate Sensor** | `Echo Entry` | **GPIO 35** *(Input Only)* |
| **Exit Gate Sensor** | `Echo Exit` | **GPIO 34** *(Input Only)* |
| **Barrier Servos** | Entry / Exit Signal | **GPIO 18 / GPIO 19** |
| **I2C Gate Display** | SDA / SCL | **GPIO 21 / GPIO 22** |
| **Safety Buzzer** | Buzzer (+) | **GPIO 4** |
| **Safety LEDs** | Red / Green | **GPIO 2 / GPIO 15** |

---

## 👥 Team & Contributions
Group Leader: System Architecture, Hardware Integration & Task Delegation

Embedded Sub-Team: Breadboard assembly, shared-trigger calibration & ESP32 firmware

Software Sub-Team: Spring Boot MVC development, MySQL setup & Thymeleaf UI design

---

## 💻 Tech Stack
* **Firmware:** C++ / Arduino IDE (`ESP32Servo`, `Adafruit_SSD1306`, `ArduinoJson`)
* **Backend:** Java 17+, Spring Boot (Spring MVC, Spring Data JPA)
* **Database:** MySQL / H2 In-Memory Database
* **Frontend:** Thymeleaf, HTML5, CSS3 (Bootstrap 5), JavaScript (`fetch` API)
* **Communication:** HTTP REST API (JSON payloads)

---

## Completed Frontend Features

The following frontend issues have been implemented and verified against the current Spring Boot backend:

### 1. Driver Dashboard View

**What it does:** Displays an 8-slot campus parking grid with summary counts for available and occupied spaces.

**Route:** `GET /driver`

**Main files:**
- `server/src/main/resources/templates/driver-dashboard.html`
- `server/src/main/java/com/smartparking/backend/DriverDashboardController.java`

**Details:**
- Bootstrap 5 layout with header, summary cards, parking grid, and footer
- Each slot shows its campus category (Undergraduates, Lecturers, Visitors, etc.)
- Initial slot markup is updated on load by the polling script (see Issue #2)

### 2. Real-Time Parking Occupancy Polling

**What it does:** Polls the backend every 5 seconds and updates slot badges plus available/occupied summary counts.

**API:** `GET /api/parking/status`

**Response format:**
```json
[
  { "slotId": 1, "isOccupied": false },
  { "slotId": 2, "isOccupied": true }
]
```

**Main files:**
- Inline polling script in `driver-dashboard.html`
- `DriverDashboardController.getParkingStatus()`

**Details:**
- Uses `fetch()` with JSON accept headers
- Updates DOM elements `#slot-{id}-status`, `#available-count`, and `#occupied-count`
- Handles API failures gracefully without breaking the page
- Supports both `slotId`/`isOccupied` and snake_case field names

### 3. Security Guard Barrier Control

**What it does:** Provides a security guard dashboard to monitor gate status and remotely open entry/exit barriers.

**Route:** `GET /security-guard`

**APIs:**
- `GET /api/gates/status` → `{ "entryGate": "CLOSED", "exitGate": "CLOSED" }`
- `POST /api/gates/entry/open` → `{ "message": "Entry barrier opened successfully!" }`
- `POST /api/gates/exit/open` → `{ "message": "Exit barrier opened successfully!" }`

**Main files:**
- `server/src/main/resources/templates/security-guard.html`
- `server/src/main/java/com/smartparking/backend/GateController.java`
- `DriverDashboardController` (security guard page route)

**Details:**
- Polls gate status every 3 seconds
- Disables action buttons while a request is in progress
- Shows success/error feedback in a message box
- Gate state is currently held in-memory in the backend (stub until ESP32 integration is connected)

### 4. Custom Dashboard CSS Enhancements

**What it does:** Applies shared team styling for parking cards, occupancy badges, and security gate controls.

**Main file:** `server/src/main/resources/static/css/dashboard.css`

**Details:**
- Bootstrap source files are loaded from CDN and are not modified locally
- Available/occupied states use distinct colors and top-border accents on slot cards
- Gate status badges and action buttons follow the same dashboard theme
- Responsive layout refinements for smaller screens
- Reduced-motion support for accessibility

---

## Running and Testing the Application

### Prerequisites
- Java 17 or later
- Maven Wrapper (included in `server/`)

### Start the backend
```bash
cd server
./mvnw spring-boot:run        # Linux/macOS
.\mvnw.cmd spring-boot:run    # Windows
```

The application starts on `http://localhost:8080` by default.

### Test the dashboards
| Page | URL |
|------|-----|
| Driver Dashboard | http://localhost:8080/driver |
| Security Guard Dashboard | http://localhost:8080/security-guard |

### Run tests
```bash
cd server
./mvnw test        # Linux/macOS
.\mvnw.cmd test    # Windows
```

### Configuration
- Default config: `server/src/main/resources/application.properties`
- Current backend uses in-memory H2 and stub REST responses for parking/gate status
- ESP32/firmware integration is planned but not yet present in this repository

---

## 📂 Repository Structure
```text
├── firmware/
│   └── smart_parking_esp32/     # Unified single-ESP32 Arduino sketch
├── server/
│   └── backend/                 # Spring Boot MVC Application
│       ├── src/main/java/com/smartparking/
│       │   ├── controller/      # WebViewController & REST ApiControllers
│       │   ├── model/           # JPA Entities (ParkingSlot, GateStatus)
│       │   ├── repository/      # Spring Data JPA Repositories
│       │   └── service/         # Business Logic & Database Seeders
│       └── src/main/resources/
│           ├── templates/       # Thymeleaf HTML Views (Driver & Security)
│           └── application.properties
└── docs/
    ├── schematics/              # Circuit schematics and pin mappings
    └── images/                  # Prototype design and screenshots