# 🚗 IoT Smart Parking & Automated Gate Management System

An IoT-powered smart campus parking management and automated barrier control system built using a single **ESP32**, **HC-SR04 ultrasonic sensors**, and a **Java Spring Boot (MVC)** web dashboard. This system provides real-time parking slot monitoring, automated vehicle detection, and a web-based interface for drivers and security staff.

**Institution:** University of Colombo School of Computing  
**Course:** Enterprise Application Development (Year 3 - Semester 1)

---

## 📌 Project Overview

The IoT Smart Parking & Automated Gate Management System demonstrates IoT integration with web technologies, combining:

- **Automated Entry/Exit Control:** Ultrasonic sensors detect approaching vehicles and trigger servo-operated barrier gates
- **Real-Time Occupancy Monitoring:** Tracks parking availability across 8 designated campus slots
- **Web Dashboard:** Public driver view with live slot statuses and categories
- **Security Override:** Security guard dashboard for remote barrier control (planned)
- **Safety Assistance:** Reverse parking safety features using distance sensors, buzzers, and LEDs (Demo on Slot 4)
- **Live Display:** I2C display at the gate entrance showing real-time availability

---

## 🎯 Objectives

1. **System Integration:** Demonstrate seamless integration between embedded systems (ESP32) and enterprise backend (Spring Boot)
2. **IoT Communication:** Implement Wi-Fi based HTTP REST API for sensor-to-server communication
3. **Real-Time Data Management:** Handle concurrent requests and live updates from multiple sensors
4. **Web Application Development:** Build a responsive, accessible web interface using Thymeleaf and Bootstrap
5. **Database Design:** Model parking state and access control using relational database concepts
6. **Hardware-Software Interface:** Validate sensor readings and actuate barriers reliably

---

## ✨ Key Features

| Feature | Status | Notes |
|---------|--------|-------|
| **Driver Dashboard** | ✅ In Development | `/driver` route, Thymeleaf template with 8-slot grid |
| **Real-Time Occupancy Display** | ✅ Planned | Backend APIs to support polling/updates |
| **Automated Entry Barrier** | ✅ Planned | HC-SR04 sensor + SG90 servo control |
| **Automated Exit Barrier** | ✅ Planned | HC-SR04 sensor + SG90 servo control |
| **Shared-Trigger Sensor Configuration** | ✅ Planned | All ultrasonic sensors share trigger pin (GPIO 5) |
| **Reverse Safety Assist (Demo)** | ✅ Planned | Slot 4: buzzer + Red/Green LED feedback |
| **Entrance Display** | ✅ Planned | I2C OLED/LCD showing available & occupied slots |
| **Security Guard Dashboard** | ⏳ Planned | Remote barrier control via REST API |
| **Database Models** | ⏳ Planned | JPA entities for slots, barriers, occupancy |
| **REST API Endpoints** | ⏳ Planned | Controllers for slot status, barrier control |

---

## 🏗️ System Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                        Hardware Layer                        │
├──────────────────────────────────────────────────────────────┤
│
│  Sensors (HC-SR04 × 10)           Actuators (SG90 × 2)
│  ├─ 8 Parking Slots                ├─ Entry Barrier Servo
│  ├─ 1 Entry Gate                   └─ Exit Barrier Servo
│  └─ 1 Exit Gate
│
│  Displays                          Indicators
│  ├─ I2C OLED SSD1306              ├─ Active Buzzer (GPIO 4)
│  └─ OR 16x2 LCD                    ├─ Red LED (GPIO 2)
│                                    └─ Green LED (GPIO 15)
│
└──────────────────────────────────────────────────────────────┘
                              ▲
                              │ UART/I2C/GPIO
                              │
                    ┌─────────▼──────────┐
                    │   ESP32 DevKit     │
                    │  (Microcontroller) │
                    └─────────┬──────────┘
                              │
                         Wi-Fi │ (TCP/IP)
                              │
                    ┌─────────▼──────────────────┐
                    │   Network (Router/WiFi)    │
                    └─────────┬──────────────────┘
                              │
            ┌─────────────────┼─────────────────┐
            │                 │                 │
    ┌───────▼────────┐ ┌──────▼─────┐ ┌────────▼──────┐
    │ Driver Browser │ │   Security │ │ Administrator │
    │  (/driver)     │ │  Dashboard │ │    (planned)   │
    └────────┬───────┘ └──────┬─────┘ └────────┬───────┘
             │                │                │
             └────────────────┼────────────────┘
                              │
                    ┌─────────▼──────────────────┐
                    │  Spring Boot Backend       │
                    │  (Java 17+, Tomcat 11)     │
                    ├────────────────────────────┤
                    │  Controllers:              │
                    │  - DriverDashboardController
                    │  - REST API (planned)      │
                    │                            │
                    │  Models & Repositories:    │
                    │  - ParkingSlot (planned)   │
                    │  - GateStatus (planned)    │
                    │                            │
                    │  Services:                 │
                    │  - ParkingService (planned)│
                    │  - SensorPolling (planned) │
                    └─────────┬──────────────────┘
                              │
                ┌─────────────┼─────────────┐
                │             │             │
        ┌───────▼─────┐  ┌────▼────┐  ┌────▼────┐
        │  Thymeleaf  │  │  MySQL  │  │ H2 In   │
        │   Templates │  │         │  │ Memory  │
        ├─────────────┤  └─────────┘  └─────────┘
        │ Templates:  │
        │ - driver-   │
        │   dashboard │
        │ - security- │
        │   gate      │
        │   (planned) │
        └─────────────┘
```

**Communication Flow:**

1. **Sensor → ESP32:** HC-SR04 ultrasonic sensors send distance measurements via GPIO pins
2. **ESP32 → Backend:** Sends HTTP POST requests with JSON payloads to Spring Boot server via Wi-Fi
3. **Backend → Database:** Stores sensor readings and gate status in MySQL/H2
4. **Backend → Dashboard:** Serves Thymeleaf-rendered HTML pages to drivers and security staff
5. **Dashboard → Backend:** Drivers/security staff trigger updates via form submissions or fetch API calls

---

## 🅿️ Parking Slot Categories

The system manages 8 dedicated campus parking slots, each reserved for specific user categories:

| Slot # | Category | Purpose | Safety Feature (Slot 4) |
|--------|----------|---------|------------------------|
| 1 | Undergraduates | Student parking | — |
| 2 | Short Courses Students | Short course participant parking | — |
| 3 | Visiting Lecturers | Guest lecturer parking | — |
| 4 | Visitors | Campus visitor parking | ✅ Reverse Safety Assist (Distance buzzer + LED feedback) |
| 5 | Lecturers | Faculty parking | — |
| 6 | Short Course Teachers | Instructor parking | — |
| 7 | Academic Staff | Administrative staff parking | — |
| 8 | Non-Academic Staff | Support staff parking | — |

---

## 🛠️ Hardware Requirements

### Microcontroller
- **1× ESP32 DevKit V1** (30-pin or 38-pin variant, 240 MHz, dual-core, 520 KB SRAM)

### Sensors
- **10× HC-SR04 Ultrasonic Distance Sensors**
  - 8× for parking slots (Slots 1–8)
  - 1× for entry barrier detection
  - 1× for exit barrier detection
  - All sensors share a common trigger pin (GPIO 5)

### Actuators
- **2× SG90 or MG90S Micro Servo Motors** (for entry and exit barriers)
  - Operating voltage: 4.8–6.0V
  - Torque: ~1.5–1.8 kg·cm
  - Control: PWM signals from GPIO 18 (entry) and GPIO 19 (exit)

### Display
- **1× 0.96-inch I2C OLED Display (SSD1306)** OR **16×2 I2C LCD**
  - Communication: I2C (GPIO 21 = SDA, GPIO 22 = SCL)
  - Shows real-time available and occupied slot counts

### Indicators & Alerts
- **1× 5V Active Buzzer** (GPIO 4, digital output)
- **1× 5mm Red LED** (GPIO 2, with 220Ω current-limiting resistor)
- **1× 5mm Green LED** (GPIO 15, with 220Ω current-limiting resistor)

### Power & Connectivity
- **1× 5V / 3A DC External Power Supply** (regulated, for ESP32, servos, and sensors)
- **2× 830-point Solderless Breadboards** (MB-102 or equivalent)

### Wiring
- **80× Female-to-Male Jumper Wires** (signal connections)
- **40× Male-to-Male Jumper Wires** (power rails and logic connections)

---

## 📌 ESP32 GPIO Pin Allocation

The ESP32 uses the following GPIO pins for sensors, actuators, and communication:

| Component | Function | GPIO Pin | Notes |
|-----------|----------|----------|-------|
| **Ultrasonic Sensors** | Shared Trigger | GPIO 5 | All 10 sensors share this trigger pin |
| **Slot 1 Sensor** | Echo Input | GPIO 13 | |
| **Slot 2 Sensor** | Echo Input | GPIO 12 | |
| **Slot 3 Sensor** | Echo Input | GPIO 14 | |
| **Slot 4 Sensor** | Echo Input | GPIO 27 | |
| **Slot 5 Sensor** | Echo Input | GPIO 26 | |
| **Slot 6 Sensor** | Echo Input | GPIO 25 | |
| **Slot 7 Sensor** | Echo Input | GPIO 33 | |
| **Slot 8 Sensor** | Echo Input | GPIO 32 | |
| **Entry Barrier Sensor** | Echo Input | GPIO 35 | ⚠️ Input-only pin (no output capability) |
| **Exit Barrier Sensor** | Echo Input | GPIO 34 | ⚠️ Input-only pin (no output capability) |
| **Entry Barrier Servo** | PWM Control | GPIO 18 | Servo pulse width modulation |
| **Exit Barrier Servo** | PWM Control | GPIO 19 | Servo pulse width modulation |
| **I2C Display** | SDA (Data) | GPIO 21 | I2C clock stretching supported |
| **I2C Display** | SCL (Clock) | GPIO 22 | I2C clock stretching supported |
| **Safety Buzzer** | Digital Output | GPIO 4 | Active HIGH (5V signal via current limit) |
| **Red LED** | Digital Output | GPIO 2 | 220Ω resistor recommended |
| **Green LED** | Digital Output | GPIO 15 | 220Ω resistor recommended |

**Important Notes:**
- GPIO 34 and GPIO 35 are **input-only** pins on the ESP32 and cannot be used for PWM output or digital writes
- GPIO 5 is used as the shared trigger for all HC-SR04 sensors to minimize pin usage
- The I2C bus (GPIO 21 and 22) uses pull-up resistors (typically 4.7kΩ) to VCC

---

## 💻 Technology Stack

### Firmware (Embedded)
- **Language:** C++ with Arduino IDE
- **Platform:** ESP32 (Espressif IDF through Arduino)
- **Key Libraries:**
  - `ESP32Servo` — PWM servo control
  - `Adafruit_SSD1306` — OLED display driver (I2C)
  - `ArduinoJson` — JSON serialization for HTTP payloads
  - `WiFi` (built-in) — Wi-Fi connectivity
  - `HTTPClient` (built-in) — HTTP requests to backend

### Backend (Server-Side)
- **Language:** Java 17+ (OpenJDK)
- **Framework:** Spring Boot 4.1.1
- **Key Modules:**
  - `spring-boot-starter-web` — HTTP server & REST endpoints
  - `spring-boot-starter-thymeleaf` — Server-side HTML templating
  - `spring-boot-starter-data-jpa` — Object-relational mapping
  - `spring-boot-starter-test` — Unit and integration testing
- **Database Support:**
  - `mysql-connector-j` — MySQL JDBC driver
  - `com.h2database:h2` — H2 in-memory database (for development/testing)
- **Build Tool:** Maven 3.9.16

### Frontend (Client-Side)
- **Markup:** HTML5
- **Styling:** CSS3 + **Bootstrap 5.3.3** (from CDN)
- **Templating:** Apache Thymeleaf (server-side rendering)
- **Scripting:** Vanilla JavaScript (ES6)
  - Fetch API for asynchronous HTTP requests
  - DOM manipulation for dynamic updates

### Communication Protocol
- **Protocol:** HTTP/HTTPS (RESTful architecture)
- **Data Format:** JSON (RFC 7159)
- **Wi-Fi:** IEEE 802.11 b/g/n (2.4 GHz)

### Development & Testing
- **IDE:** Visual Studio Code, Arduino IDE, IntelliJ IDEA
- **Version Control:** Git
- **Build & Compilation:** Maven (JVM), Arduino CLI (firmware)

---

## 📂 Repository Structure

```
smart-parking-system/
├── README.md                          # Project documentation (this file)
├── .gitignore                         # Git ignore rules
│
├── firmware/                          # ESP32 Firmware
│   └── smart_parking_esp32/
│       ├── smart_parking_esp32.ino    # Main Arduino sketch
│       ├── config.h                   # Configuration macros (WiFi SSID, server URL, etc.)
│       ├── sensors.h / sensors.cpp    # Ultrasonic sensor abstraction
│       ├── gates.h / gates.cpp        # Servo barrier control
│       ├── display.h / display.cpp    # I2C OLED/LCD display driver
│       ├── safety.h / safety.cpp      # Buzzer & LED safety feedback
│       └── communication.h / .cpp     # HTTP REST client
│
├── server/                            # Java Spring Boot Backend
│   ├── pom.xml                        # Maven project configuration
│   ├── .mvn/wrapper/                  # Maven wrapper (bundled Maven)
│   ├── mvnw / mvnw.cmd                # Maven wrapper scripts
│   │
│   └── src/
│       ├── main/
│       │   ├── java/com/smartparking/backend/
│       │   │   ├── BackendApplication.java          # Spring Boot entry point
│       │   │   ├── DriverDashboardController.java   # Dashboard routing
│       │   │   │
│       │   │   ├── controller/                      # (Planned)
│       │   │   │   ├── WebViewController.java
│       │   │   │   └── ParkingApiController.java
│       │   │   │
│       │   │   ├── model/                           # (Planned)
│       │   │   │   ├── ParkingSlot.java
│       │   │   │   ├── GateStatus.java
│       │   │   │   └── SensorReading.java
│       │   │   │
│       │   │   ├── repository/                      # (Planned)
│       │   │   │   ├── ParkingSlotRepository.java
│       │   │   │   └── GateStatusRepository.java
│       │   │   │
│       │   │   ├── service/                         # (Planned)
│       │   │   │   ├── ParkingService.java
│       │   │   │   ├── SensorPollingService.java
│       │   │   │   └── GateControlService.java
│       │   │   │
│       │   │   ├── dto/                             # (Planned)
│       │   │   │   └── SensorDataDto.java
│       │   │   │
│       │   │   └── config/                          # (Planned)
│       │   │       └── DatabaseConfig.java
│       │   │
│       │   └── resources/
│       │       ├── application.properties           # Spring Boot configuration
│       │       │
│       │       ├── templates/
│       │       │   ├── driver-dashboard.html        # Driver view (8-slot grid)
│       │       │   ├── security-gate.html           # Security guard dashboard (planned)
│       │       │   └── index.html                   # Home page (planned)
│       │       │
│       │       └── static/
│       │           ├── css/
│       │           │   └── dashboard.css            # Custom styling (planned)
│       │           │
│       │           └── js/
│       │               ├── driver-poller.js         # Real-time polling (planned)
│       │               └── security-gate.js         # Gate control script (planned)
│       │
│       └── test/
│           └── java/com/smartparking/backend/
│               └── BackendApplicationTests.java    # Integration tests
│
├── database/                          # Database setup
│   ├── schema.sql                     # MySQL table definitions
│   └── seed.sql                       # Initial data for testing
│
└── docs/                              # Documentation
    ├── README.md                      # Additional documentation
    ├── api/
    │   └── api-documentation.md       # REST API endpoint specifications
    ├── database/
    │   └── database-design.md         # ER diagram and normalization notes
    └── schematics/
        ├── circuit-diagram.png        # Breadboard wiring diagram
        ├── esp32-pin-mapping.md       # GPIO pin assignment reference
        └── wiring-diagram.md          # Detailed connection guide
```

**Legend:** ✅ = Implemented, ⏳ = Planned

---

## 🎨 Software Architecture

### Backend Layers

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  (Thymeleaf Templates + HTML/CSS)       │
│  - driver-dashboard.html                │
│  - security-gate.html (planned)         │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│      Controller Layer (Spring MVC)      │
│  - DriverDashboardController            │
│  - ParkingApiController (planned)       │
│  - GateControlController (planned)      │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│        Service Layer (Business Logic)   │
│  - ParkingService                       │
│  - SensorPollingService                 │
│  - GateControlService                   │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│  Data Access Layer (Spring Data JPA)    │
│  - ParkingSlotRepository                │
│  - GateStatusRepository                 │
│  - SensorReadingRepository              │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│         Database Layer                  │
│  - MySQL (Production)                   │
│  - H2 In-Memory (Development/Testing)   │
└─────────────────────────────────────────┘
```

### Database Schema (Planned)

```sql
-- Parking Slots Table
CREATE TABLE parking_slots (
    slot_id INT PRIMARY KEY AUTO_INCREMENT,
    slot_number INT UNIQUE NOT NULL (1-8),
    category VARCHAR(50) NOT NULL,
    is_occupied BOOLEAN DEFAULT FALSE,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    distance_cm INT,
    sensor_gpio INT
);

-- Gate Status Table
CREATE TABLE gate_status (
    gate_id INT PRIMARY KEY AUTO_INCREMENT,
    gate_type VARCHAR(20) NOT NULL (ENTRY/EXIT),
    is_open BOOLEAN DEFAULT FALSE,
    servo_angle INT,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sensor_distance INT
);

-- Sensor Readings (for historical data)
CREATE TABLE sensor_readings (
    reading_id INT PRIMARY KEY AUTO_INCREMENT,
    slot_id INT,
    distance_cm INT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (slot_id) REFERENCES parking_slots(slot_id)
);
```

---

## 🌐 Frontend Architecture

### Driver Dashboard View

**Route:** `/driver`  
**Technology:** Thymeleaf + Bootstrap 5 + Vanilla JavaScript

**Features (Planned/In Development):**
1. **Parking Summary Card**
   - Available slots count
   - Occupied slots count
   - Real-time status indicator

2. **Parking Grid Display**
   - 8-slot responsive grid (4 columns on desktop, 2 on tablet, 1 on mobile)
   - Color-coded slot status: Green (Available) / Red (Occupied)
   - Slot category and number displayed
   - Distance sensor reading (debugging mode)

3. **Real-Time Polling** (Planned)
   - JavaScript `setInterval()` or `fetch()` every 2–5 seconds
   - AJAX updates without full page reload
   - Visual feedback for status changes

4. **Responsive Design**
   - Mobile-first Bootstrap breakpoints
   - Touch-friendly buttons and controls
   - Accessibility (WCAG 2.1 AA compliance)

### Security Dashboard View (Planned)

**Route:** `/security`  
**Technology:** Thymeleaf + Bootstrap 5 + Vanilla JavaScript

**Features:**
1. **Gate Control Panel**
   - Manual entry barrier control button
   - Manual exit barrier control button
   - Servo angle slider
   - Confirmation dialog before opening barriers

2. **System Status Monitor**
   - Real-time gate positions
   - Last sensor readings
   - ESP32 connectivity status
   - Error logs

3. **Audit Trail** (Planned)
   - Record of all gate operations
   - Timestamp and operator information
   - Export logs as CSV/PDF

---

## 📡 REST Communication Overview

### ESP32 → Backend Communication

**Request Type:** `POST`  
**Endpoint:** `/api/parking/update`  
**Content-Type:** `application/json`

**Example Payload:**
```json
{
  "timestamp": 1693097400000,
  "slots": [
    { "slot_id": 1, "occupied": false, "distance_cm": 50 },
    { "slot_id": 2, "occupied": true, "distance_cm": 8 },
    { "slot_id": 3, "occupied": false, "distance_cm": 60 },
    { "slot_id": 4, "occupied": true, "distance_cm": 15 }
  ],
  "entry_distance_cm": 200,
  "exit_distance_cm": 150
}
```

**Response:** `200 OK`
```json
{
  "status": "success",
  "message": "Slot data updated successfully",
  "entries_open": false
}
```

### Backend → ESP32 Communication (Gate Control)

**Request Type:** `GET`  
**Endpoint (on ESP32):** `http://<ESP32_IP>:8080/gate/control`  
**Query Parameters:**
- `gate`: `ENTRY` or `EXIT`
- `action`: `OPEN` or `CLOSE`
- `angle`: servo angle (0–180)

**Example:** `http://192.168.1.100:8080/gate/control?gate=ENTRY&action=OPEN&angle=90`

**Response:** `200 OK`
```json
{
  "gate": "ENTRY",
  "status": "opened",
  "angle": 90
}
```

---

## 🗄️ Database Overview

### Database Choice

- **Production:** MySQL 8.0+ (persistent storage, multi-user support)
- **Development/Testing:** H2 in-memory database (fast, no external setup)

### Key Entities

1. **ParkingSlot**
   - Unique identifier (1–8)
   - Category (undergraduate, lecturer, etc.)
   - Occupancy status
   - Last sensor reading (distance in cm)
   - Last update timestamp

2. **GateStatus**
   - Type (entry/exit)
   - Open/closed boolean
   - Current servo angle
   - Last sensor reading
   - Operation timestamp

3. **SensorReading** (Optional, for analytics)
   - Slot reference
   - Distance measurement
   - Timestamp for historical analysis

---

## 👥 Team & Contributions

### Roles & Responsibilities

**Group Leader**
- System architecture design and documentation
- Hardware/software integration planning
- Code review and quality assurance
- Sprint planning and task delegation
- Final project presentation

**Embedded Systems Sub-Team**
- ESP32 firmware development (C++/Arduino)
- Breadboard assembly and soldering
- HC-SR04 sensor calibration
- Shared-trigger ultrasonic configuration
- Servo and safety hardware testing
- Display (OLED/LCD) integration

**Software Development Sub-Team**
- Spring Boot backend development (Java)
- REST API design and implementation
- MySQL database schema and optimization
- Thymeleaf HTML template design
- Frontend JavaScript (polling, real-time updates)
- Bootstrap CSS customization
- Unit and integration testing

### Current Development Tasks

**Frontend Issues Assigned:**
1. **Driver Dashboard View** (`templates/driver-dashboard.html`)
   - 8-slot parking grid with status indicators
   - Slot categories and numbering
   - Bootstrap responsive layout
   - CSS styling and animations

2. **Real-Time Polling Script** (`static/js/driver-poller.js`)
   - Fetch API calls to backend at regular intervals
   - DOM manipulation for live updates
   - Error handling and reconnection logic
   - Visual feedback for status changes

3. **Security Gate Control** (`static/js/security-gate.js`)
   - Manual barrier control buttons
   - Servo angle slider
   - Confirmation dialogs
   - Status feedback and error handling

4. **Custom CSS Enhancements** (`static/css/dashboard.css`)
   - Custom color scheme and typography
   - Responsive breakpoints
   - Animation and hover effects
   - Dark mode support (optional)

---

## 🚀 Running the Backend

### Prerequisites

- **Java 17+** installed (OpenJDK or Oracle JDK)
- **Maven 3.8+** installed (or use bundled Maven wrapper)
- **Git** for version control
- Port `8080` available on localhost

### Step 1: Restore Backend Files (if deleted)

If the `server/pom.xml` or Java source files are missing from your working directory, restore them from Git:

```bash
cd "D:\Year 3 - Semester 1\Enterprise Application Development\Smart-Parking-System"
git restore server/pom.xml server/src/main/java server/src/main/resources/application.properties server/src/test server/.mvn server/mvnw server/mvnw.cmd
```

### Step 2: Navigate to Backend Directory

```bash
cd server
```

### Step 3: Build the Project

```bash
mvn clean install
```

This will:
- Download Maven dependencies (Java libraries)
- Compile the Java source code
- Run unit tests
- Package the application as a JAR file

### Step 4: Run the Application

```bash
mvn spring-boot:run
```

Or, if you have Java and Maven configured system-wide, use:

```bash
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

### Expected Output

You should see log output ending with:

```
Started BackendApplication in X.XXX seconds
Tomcat started on port(s): 8080 (http)
```

### Step 5: Verify the Application

Open your browser and navigate to:

```
http://localhost:8080/driver
```

You should see the **Driver Dashboard** displaying 8 parking slots with their categories and availability status.

### Stopping the Application

Press `Ctrl+C` in the terminal to stop the Spring Boot server.

### Troubleshooting

| Problem | Solution |
|---------|----------|
| "Port 8080 already in use" | Stop any existing Java process using port 8080, or change the port in `application.properties` using `server.port=8081` |
| "No plugin found for prefix 'spring-boot'" | Ensure `pom.xml` exists in the `server/` directory. Restore it with `git restore server/pom.xml` |
| "Cannot find tool mvn" | Install Maven globally or use the bundled wrapper: `./mvnw` (macOS/Linux) or `mvnw.cmd` (Windows) |
| Build failures with dependency errors | Clear Maven cache: `mvn clean` and try again. Check internet connectivity. |
| Thymeleaf template not found | Verify `driver-dashboard.html` exists in `server/src/main/resources/templates/` |

---

## 🔧 Running the ESP32 Firmware

### Prerequisites

- **Arduino IDE** 1.8.19+ or **Arduino CLI**
- **ESP32 Board Support Package** installed in Arduino IDE
- **Required Libraries** installed via Arduino Library Manager:
  - `ESP32Servo` by John K. Bennett
  - `Adafruit SSD1306` by Adafruit Industries
  - `ArduinoJson` by Benoit Blanchon
  - `AsyncTCP` (for advanced Wi-Fi features, optional)

### Step 1: Open the Firmware Sketch

1. Open **Arduino IDE**
2. Navigate to: `File` → `Open`
3. Select: `firmware/smart_parking_esp32/smart_parking_esp32.ino`

### Step 2: Configure Wi-Fi & Backend Settings

Edit `config.h` in the same directory:

```cpp
#define WIFI_SSID "YOUR_WIFI_SSID"
#define WIFI_PASSWORD "YOUR_WIFI_PASSWORD"
#define BACKEND_SERVER "192.168.1.X"  // IP of your Spring Boot server
#define BACKEND_PORT 8080
#define POLLING_INTERVAL 5000  // 5 seconds
```

**⚠️ Security Note:** Do not commit real credentials to Git. Use environment variables or `.gitignore` entries for sensitive data.

### Step 3: Select Board & Port

1. **Board Selection:**
   - Go to: `Tools` → `Board` → `ESP32 Arduino` → **ESP32 Dev Module**

2. **Port Selection:**
   - Go to: `Tools` → `Port`
   - Select the COM port to which your ESP32 is connected (e.g., `COM3`, `/dev/ttyUSB0`)

3. **Other Settings:**
   - Upload Speed: `921600` or `460800`
   - Flash Frequency: `80 MHz`
   - Flash Mode: `DIO`
   - Partition Scheme: `Default 4MB with spiffs`

### Step 4: Verify & Upload

1. **Verify (Compile):** `Sketch` → `Verify/Compile` (or press `Ctrl+R`)
2. **Upload:** `Sketch` → `Upload` (or press `Ctrl+U`)

You should see:

```
Connecting....
Writing at 0x00000000... (5 %)
Connecting....
Written 0x00000000 + 0x00000000 (0 bytes)...
Hash of data verified.
Leaving...
Hard resetting via RTS pin...
```

### Step 5: Verify Functionality

1. Open **Serial Monitor** (`Tools` → `Serial Monitor`)
2. Set Baud Rate to `115200`
3. You should see debug output like:

```
WiFi connecting...
WiFi connected! IP: 192.168.1.100
Backend server: 192.168.1.X:8080
Initializing sensors...
Starting sensor polling...
Slot 1: 45cm (Available)
Slot 2: 8cm (Occupied)
...
```

### Troubleshooting

| Problem | Solution |
|---------|----------|
| Board not detected | Check USB drivers for CH340 or CP2102 (chip depends on ESP32 variant). Reinstall from manufacturer's website. |
| Upload timeout | Reduce upload speed to `115200` or `230400`. Check USB cable quality. |
| Wi-Fi connection fails | Verify SSID and password in `config.h`. Check Wi-Fi router is compatible with 2.4 GHz 802.11 b/g/n. |
| Sensors not responding | Verify GPIO pins match `config.h`. Check HC-SR04 wiring and breadboard connections. |
| Display not showing data | Verify I2C SDA/SCL connections (GPIO 21/22). Check I2C address with I2C scanner sketch. |

---

## 🧪 Testing

### Backend Unit Tests

Run the existing test suite:

```bash
cd server
mvn test
```

Expected tests:
- `BackendApplicationTests` — Spring context initialization

### Integration Tests (Planned)

- Parking slot service operations
- REST API endpoint validation
- Database persistence

### Frontend Testing (Planned)

- Manual browser testing
- Responsive design verification (Chrome DevTools)
- Accessibility audit (Axe DevTools, Lighthouse)

### Hardware Integration Tests

1. **Sensor Calibration Test:** Place objects at known distances and verify HC-SR04 accuracy
2. **Servo Movement Test:** Command entry/exit barriers to open/close and verify smooth motion
3. **Wi-Fi Communication Test:** Monitor ESP32 serial output and backend logs during sensor updates
4. **End-to-End Test:** Drive a vehicle (or obstacle) through entry/exit gates and verify slot updates on dashboard

---

## 🐛 Troubleshooting

### Backend Issues

| Symptom | Cause | Solution |
|---------|-------|----------|
| Dashboard displays "Connection Refused" | Backend not running or port mismatch | Ensure `mvn spring-boot:run` is active on port 8080 |
| Slot statuses don't update | Polling script not running or API endpoint missing | Check browser console for JavaScript errors. Verify backend receives POST requests. |
| Template rendering error (Thymeleaf) | Missing or malformed template file | Verify `driver-dashboard.html` exists and has valid syntax |
| Database connection error | MySQL not running or credentials incorrect | Check `application.properties` database URL, username, password |

### Hardware Issues

| Symptom | Cause | Solution |
|---------|-------|----------|
| HC-SR04 returns constant distance | Sensor disconnected or GPIO misconfigured | Verify sensor VCC/GND. Test with Arduino `NewPing` library example. |
| Servo not moving | PWM signal not reaching servo or servo damaged | Verify GPIO 18/19 are PWM-capable. Test servo with known-good code. |
| I2C display blank | Display disconnected, I2C address wrong, or library error | Verify SDA/SCL connections. Scan I2C address with `Wire` example. |
| Buzzer always on or always off | GPIO short circuit or logic inverted | Check GPIO 4 connection and verify active-HIGH configuration. |
| LEDs not lighting | LED polarity reversed or GPIO misconfigured | Ensure long pin (+) to GPIO, short pin (-) to GND via resistor. |

---

## 🚧 Future Improvements

1. **Advanced Features**
   - Real-time WebSocket updates (faster than polling)
   - Mobile app for iOS/Android
   - QR code-based vehicle registration
   - License plate recognition (ML model integration)
   - Parking reservation system

2. **Hardware Enhancements**
   - Temperature & humidity monitoring
   - LiDAR sensors for improved accuracy (optional, higher cost)
   - Dual ESP32s for redundancy
   - 4G/LTE fallback if Wi-Fi fails

3. **Backend Improvements**
   - Multi-site parking management
   - Analytics dashboard (peak hours, occupancy trends)
   - Machine learning occupancy predictions
   - Integration with campus LDAP for user authentication
   - Billing/payment system for visitor parking

4. **Security Enhancements**
   - HTTPS/TLS encryption for all API calls
   - Role-based access control (RBAC)
   - Two-factor authentication for security staff
   - API rate limiting and DDoS protection
   - Audit logging for compliance

5. **Frontend Improvements**
   - Dark mode theme
   - Internationalization (multiple languages)
   - PWA (Progressive Web App) for offline support
   - Export reports (PDF/Excel)

---

## 📊 Project Status

| Component | Status | Owner | Notes |
|-----------|--------|-------|-------|
| **System Architecture** | ✅ Designed | Group Leader | Blueprint finalized |
| **Repository Setup** | ✅ Complete | Group Leader | Git initialized and structure created |
| **Backend Skeleton** | ✅ In Development | Software Team | Basic Spring Boot app running |
| **Driver Dashboard Controller** | ✅ Implemented | Software Team | Route `/driver` serving Thymeleaf template |
| **Driver Dashboard Template** | ✅ In Development | Frontend Team | HTML grid structure with Bootstrap styling |
| **Database Models (JPA)** | ⏳ Planned | Software Team | ParkingSlot, GateStatus entities to be created |
| **REST APIs** | ⏳ Planned | Software Team | Endpoints for slot status, gate control |
| **Security Dashboard** | ⏳ Planned | Frontend Team | Guard override functionality |
| **Real-Time Polling** | ⏳ Planned | Frontend Team | JavaScript fetch/WebSocket updates |
| **ESP32 Firmware** | ⏳ In Development | Embedded Team | Sensor reading and gate control logic |
| **Sensor Calibration** | ⏳ In Development | Embedded Team | Shared-trigger HC-SR04 setup |
| **Display Integration** | ⏳ Planned | Embedded Team | I2C OLED/LCD support |
| **Safety Assist (Slot 4)** | ⏳ Planned | Embedded Team | Buzzer & LED feedback system |
| **MySQL Database** | ⏳ Planned | Database Team | Schema creation and seed data |
| **Testing** | ⏳ Planned | QA Team | Unit, integration, and hardware tests |
| **Documentation** | ✅ In Progress | Group Leader | API docs, schematics, setup guides |
| **Final Integration** | ⏳ Not Started | Group Leader | End-to-end testing and deployment |

**Legend:**  
✅ = Complete or In Development  
⏳ = Planned or Not Started  
🔴 = Blocked

---

## 📝 License

This project is submitted as coursework for **Enterprise Application Development (Year 3 - Semester 1)** at the **University of Colombo School of Computing**. All rights reserved. Unauthorized use or distribution without explicit permission is prohibited.

---

## 📞 Support & Contact

For issues, questions, or contributions, please contact the group leader or create an issue in the GitHub repository.

**Last Updated:** August 24, 2026