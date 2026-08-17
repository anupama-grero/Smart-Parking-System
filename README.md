# 🚗 Smart Parking Management & ANPR Gate System

An IoT-based smart parking and automated barrier management system powered by **ESP32**, **ESP32-CAM**, and real-time web monitoring.

---

## 📌 Project Overview
This system automates vehicle access and parking slot tracking across designated campus zones:
* **Automated & Manual Gate Control:** Uses an **ESP32-CAM** at the main entrance for Automatic Number Plate Recognition (ANPR). Regular vehicles enter automatically, while visitor vehicles require security guard approval via a web interface.
* **Smart Exit Gate:** Automated barrier opening using ultrasonic vehicle detection.
* **Live Slot Monitoring:** Tracks real-time occupancy across 6 campus parking zones using IR and Ultrasonic proximity sensors.
* **Park 2 Reversing Assist:** Dedicated ultrasonic safety node with distance-based visual (Red/Green LED) and audio (Buzzer) alerts.
* **Public & Gate Displays:** Real-time web dashboard for availability and an entrance OLED/LCD screen displaying free slots.

---

## 🏗️ System Architecture

[ Entry Camera (ESP32-CAM) ]             [ Exit Ultrasonic Sensor ]
│                                        │
▼                                        ▼
[ Gate Node (ESP32) ] ──────── WiFi ──────── [ Main Gate Servo ]
│
▼
[ Web Server & Database ] ◄──── Realtime Updates ──── [ Slot Nodes (IR / Ultrasonic) ]
• Security Guard Panel
• Live Slot Dashboard
• Driver Availability View

---

## 🅿️ Parking Categorization (6 Zones)
* **Park 1:** Undergraduates & Short Course Students
* **Park 2 (Left):** Visiting Lecturers
* **Park 2 (Right):** Visitors (Demo Slot with ESP32-CAM & Reversing Safety Alert)
* **Park 3:** Lecturers
* **Park 4:** Short Course Teachers
* **Park 5:** Academic Staff
* **Park 6:** Non-Academic Staff

---

## 🛠️ Hardware Requirements
* **Microcontrollers:** 2x ESP32 DevKit V1
* **Vision Modules:** 2x ESP32-CAM (AI-Thinker) + FTDI / MB Shields
* **Sensors:** 
  * 15x IR Proximity Sensors (TCRT5000)
  * 6x Ultrasonic Sensors (HC-SR04)
* **Actuators & Indicators:**
  * 2x Servos (SG90 / MG995) for Gate Barriers
  * 1x 5V Active Buzzer
  * 1x Red LED & 1x Green LED (with 220Ω Resistors)
* **Displays:** 1x 0.96" I2C OLED (SSD1306) or 16x2 I2C LCD
* **Power & Wiring:** 5V/3A DC Power Supply, Breadboards, Dupont Jumper Wires

---

## 💻 Tech Stack
* **Embedded C++ / Arduino IDE:** ESP32 Firmware & Hardware Interfacing
* **Computer Vision / OCR:** Python (OpenCV, Tesseract OCR / FastALPR)
* **Backend & Realtime DB:** Node.js / Python (Flask) with Firebase Realtime Database
* **Frontend:** HTML5, CSS3 (Bootstrap/Tailwind), JavaScript, WebSockets

---

## 📂 Repository Structure
```text
├── firmware/
│   ├── gate_node/           # ESP32 code for entrance/exit servos & display
│   ├── slot_node/           # ESP32 code for IR & ultrasonic sensor grid
│   └── camera_node/         # ESP32-CAM image capture firmware
├── server/
│   ├── ocr_engine/          # Python number plate detection scripts
│   └── backend/             # Server API and database sync
├── web/
│   ├── security_dashboard/  # Guard interface for classification & gate trigger
│   └── public_view/         # Live slot availability display for drivers
└── docs/                    # Schematics, pinouts, and circuit diagrams
👥 Team & Contributions
Group Leader: Project Architecture, Coordination & Integration

Embedded Team: Sensor integration, actuator control & ESP32 firmware

AI / Vision Team: ESP32-CAM streaming & OCR License Plate pipeline

Web & Backend Team: Web dashboard, API routes & database setup


---

## 2. Steps to Upload the `README.md` to GitHub

You can add this file to your repository using either the **GitHub Web Interface** or the **Git Command Line**:

### Method A: Directly via GitHub Website (Easiest)
1. Open your repository on [GitHub](https://github.com).
2. Look for the **"Add a README"** button (or click **"Add file"** $\rightarrow$ **"Create new file"**).
3. In the filename field, type `README.md`.
4. Paste the markdown content provided above into the editor.
5. Scroll down to **Commit changes**, type a commit message (e.g., `docs: add project README`), and click **Commit changes**.

---

### Method B: Using Git CLI (Terminal / Command Prompt)
If you have cloned the repository to your computer:

1. In your local repository folder, create a file named `README.md` and paste the content into it.
2. Open your terminal in that folder and run:
   ```bash
   git add README.md
   git commit -m "docs: add initial project README"
   git push origin main