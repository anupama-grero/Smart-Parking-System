#include <WiFi.h>
#include <HTTPClient.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include <ESP32Servo.h>

// =====================================================
// WIFI CONFIGURATION
// =====================================================

const char* WIFI_SSID = "YOUR_WIFI_NAME";
const char* WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";

// Spring Boot backend
const char* SERVER_URL = "http://192.168.1.100:8080";

// =====================================================
// ULTRASONIC SENSOR PINS
// =====================================================

// Shared trigger for all ultrasonic sensors
#define ULTRASONIC_TRIGGER 5

// Parking slot echo pins (GPIO 12 is reserved for vehicle sensor 2)
const int parkingEchoPins[8] = {
    13,  // Slot 1
    23,  // Slot 2
    14,  // Slot 3
    27,  // Slot 4
    26,  // Slot 5
    25,  // Slot 6
    33,  // Slot 7
    32   // Slot 8
};

// Entry / Exit ultrasonic sensors (backend-aware gates)
#define ENTRY_ECHO 35
#define EXIT_ECHO  34

// Digital vehicle detection sensors: either pin opens the gate
// GPIO 2  (D2)  = vehicle detection sensor 1
// GPIO 12 (D12) = vehicle detection sensor 2
// Typical IR / photoelectric modules output LOW when a vehicle is
// detected. Set VEHICLE_DETECTED_LEVEL to HIGH if yours is active HIGH.
#define VEHICLE_SENSOR_1 2
#define VEHICLE_SENSOR_2 12
const int VEHICLE_DETECTED_LEVEL = LOW;

// =====================================================
// SERVO PINS
// =====================================================

#define ENTRY_SERVO_PIN 18
#define EXIT_SERVO_PIN  19

Servo entryGateServo;
Servo exitGateServo;

// =====================================================
// DISPLAY
// =====================================================

#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64

#define OLED_RESET -1
#define OLED_ADDRESS 0x3C

Adafruit_SSD1306 display(
    SCREEN_WIDTH,
    SCREEN_HEIGHT,
    &Wire,
    OLED_RESET
);

// =====================================================
// SLOT 4 SAFETY
// =====================================================

#define BUZZER_PIN 4
#define RED_LED_PIN 16
#define GREEN_LED_PIN 15

// =====================================================
// DISTANCE THRESHOLDS
// =====================================================

// Shared detection range for every ultrasonic sensor.
// Object detected only when a valid reading is <= 5 cm.
const float ULTRASONIC_DETECTION_THRESHOLD_CM = 5.0;

// =====================================================
// SERVO ANGLES
// =====================================================

const int GATE_CLOSED_ANGLE = 0;
const int GATE_OPEN_ANGLE = 90;
const unsigned long GATE_OPEN_HOLD_MS = 1000;

// =====================================================
// PARKING DATA
// =====================================================

bool parkingOccupied[8] = {
    false, false, false, false,
    false, false, false, false
};

float parkingDistance[8] = {
    0, 0, 0, 0,
    0, 0, 0, 0
};

int availableSlots = 8;

// =====================================================
// GATE STATE
// =====================================================

bool entryGateOpen = false;
bool exitGateOpen = false;
unsigned long entryGateOpenedAt = 0;
unsigned long exitGateOpenedAt = 0;
bool vehicleDetectionLatched = false;

// =====================================================
// TIMERS
// =====================================================

unsigned long lastParkingRead = 0;
unsigned long lastBackendUpdate = 0;
unsigned long lastDisplayUpdate = 0;

const unsigned long PARKING_INTERVAL = 1000;
const unsigned long BACKEND_INTERVAL = 3000;
const unsigned long DISPLAY_INTERVAL = 1000;


// =====================================================
// SETUP
// =====================================================

void setup() {

    Serial.begin(115200);

    Serial.println();
    Serial.println("==================================");
    Serial.println(" SMART PARKING ESP32 CONTROLLER");
    Serial.println("==================================");

    // -------------------------------------------------
    // Ultrasonic trigger
    // -------------------------------------------------

    pinMode(ULTRASONIC_TRIGGER, OUTPUT);
    digitalWrite(ULTRASONIC_TRIGGER, LOW);

    // Parking echo pins
    for (int i = 0; i < 8; i++) {
        pinMode(parkingEchoPins[i], INPUT);
    }

    // Entry / exit echo
    pinMode(ENTRY_ECHO, INPUT);
    pinMode(EXIT_ECHO, INPUT);

    // -------------------------------------------------
    // Safety components
    // -------------------------------------------------

    pinMode(BUZZER_PIN, OUTPUT);
    pinMode(RED_LED_PIN, OUTPUT);
    pinMode(GREEN_LED_PIN, OUTPUT);

    digitalWrite(BUZZER_PIN, LOW);
    digitalWrite(RED_LED_PIN, LOW);
    digitalWrite(GREEN_LED_PIN, LOW);

    // -------------------------------------------------
    // Gate + vehicle sensors
    // -------------------------------------------------

    initializeGate();

    // -------------------------------------------------
    // I2C display
    // -------------------------------------------------

    Wire.begin(21, 22);

    if (!display.begin(
            SSD1306_SWITCHCAPVCC,
            OLED_ADDRESS)) {

        Serial.println("OLED initialization failed!");

    } else {

        display.clearDisplay();
        display.setTextSize(1);
        display.setTextColor(SSD1306_WHITE);

        display.setCursor(0, 0);
        display.println("SMART PARKING");

        display.setCursor(0, 20);
        display.println("Initializing...");

        display.display();
    }

    // -------------------------------------------------
    // Wi-Fi
    // -------------------------------------------------

    connectWiFi();

    Serial.println("System initialized.");
}


// =====================================================
// MAIN LOOP
// =====================================================

void loop() {

    unsigned long currentMillis = millis();

    // -------------------------------------------------
    // Read parking sensors
    // -------------------------------------------------

    if (currentMillis - lastParkingRead >= PARKING_INTERVAL) {

        lastParkingRead = currentMillis;

        readParkingSensors();

        calculateAvailableSlots();

        slot4SafetySystem();

        Serial.println();
        printParkingStatus();
    }

    // -------------------------------------------------
    // Display
    // -------------------------------------------------

    if (currentMillis - lastDisplayUpdate >= DISPLAY_INTERVAL) {

        lastDisplayUpdate = currentMillis;

        updateDisplay();
    }

    // -------------------------------------------------
    // Vehicle detection + automatic / backend gates
    // -------------------------------------------------

    updateGate();

    handleExitGate();

    handleEntryGate();

    // -------------------------------------------------
    // Backend communication
    // -------------------------------------------------

    if (currentMillis - lastBackendUpdate >= BACKEND_INTERVAL) {

        lastBackendUpdate = currentMillis;

        sendParkingStatusToBackend();

        checkEntryGateCommand();

        checkExitGateCommand();
    }
}


// =====================================================
// WIFI
// =====================================================

void connectWiFi() {

    Serial.print("Connecting to Wi-Fi");

    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

    int attempts = 0;

    while (
        WiFi.status() != WL_CONNECTED &&
        attempts < 30
    ) {

        delay(500);

        Serial.print(".");

        attempts++;
    }

    Serial.println();

    if (WiFi.status() == WL_CONNECTED) {

        Serial.println("Wi-Fi connected.");

        Serial.print("ESP32 IP: ");
        Serial.println(WiFi.localIP());

    } else {

        Serial.println("Wi-Fi connection failed.");
    }
}


// =====================================================
// ULTRASONIC DISTANCE
// =====================================================

float readDistance(int echoPin) {

    // Trigger pulse

    digitalWrite(
        ULTRASONIC_TRIGGER,
        LOW
    );

    delayMicroseconds(2);

    digitalWrite(
        ULTRASONIC_TRIGGER,
        HIGH
    );

    delayMicroseconds(10);

    digitalWrite(
        ULTRASONIC_TRIGGER,
        LOW
    );

    // Measure echo

    unsigned long duration =
        pulseIn(
            echoPin,
            HIGH,
            30000
        );

    if (duration == 0) {

        return -1;
    }

    // Speed of sound:
    // distance = duration * 0.0343 / 2

    float distance =
        duration * 0.0343 / 2.0;

    return distance;
}

bool isValidUltrasonicDistance(float distanceCm) {
    return distanceCm >= 0.0;
}

bool isUltrasonicObjectDetected(float distanceCm) {
    return isValidUltrasonicDistance(distanceCm) &&
           distanceCm <= ULTRASONIC_DETECTION_THRESHOLD_CM;
}


// =====================================================
// PARKING SENSOR READING
// =====================================================

void readParkingSensors() {

    for (int i = 0; i < 8; i++) {

        float distance =
            readDistance(
                parkingEchoPins[i]
            );

        parkingDistance[i] =
            distance;

        if (!isValidUltrasonicDistance(distance)) {
            continue;
        }

        parkingOccupied[i] =
            isUltrasonicObjectDetected(distance);

        // Small delay to reduce ultrasonic interference
        delay(50);
    }
}


// =====================================================
// AVAILABLE SLOT CALCULATION
// =====================================================

void calculateAvailableSlots() {

    availableSlots = 0;

    for (int i = 0; i < 8; i++) {

        if (!parkingOccupied[i]) {

            availableSlots++;
        }
    }
}


// =====================================================
// PRINT PARKING STATUS
// =====================================================

void printParkingStatus() {

    Serial.println("Parking Status:");

    for (int i = 0; i < 8; i++) {

        Serial.print("Slot ");
        Serial.print(i + 1);

        Serial.print(": ");

        if (parkingOccupied[i]) {

            Serial.print("OCCUPIED");

        } else {

            Serial.print("AVAILABLE");
        }

        Serial.print(" | Distance: ");

        Serial.print(
            parkingDistance[i]
        );

        Serial.println(" cm");
    }

    Serial.print("Available slots: ");
    Serial.println(availableSlots);
}


// =====================================================
// SLOT 4 REVERSE SAFETY SYSTEM
// =====================================================

void slot4SafetySystem() {

    // Slot 4 = array index 3

    float distance =
        parkingDistance[3];

    if (!isValidUltrasonicDistance(distance)) {

        digitalWrite(
            RED_LED_PIN,
            LOW
        );

        digitalWrite(
            GREEN_LED_PIN,
            LOW
        );

        digitalWrite(
            BUZZER_PIN,
            LOW
        );

        return;
    }

    if (!isUltrasonicObjectDetected(distance)) {

        digitalWrite(
            GREEN_LED_PIN,
            HIGH
        );

        digitalWrite(
            RED_LED_PIN,
            LOW
        );

        digitalWrite(
            BUZZER_PIN,
            LOW
        );
    } else {

        digitalWrite(
            GREEN_LED_PIN,
            LOW
        );

        digitalWrite(
            RED_LED_PIN,
            HIGH
        );

        tone(
            BUZZER_PIN,
            2500,
            300
        );
    }
}


// =====================================================
// AUTOMATIC GATE CONTROL (GPIO 2 / GPIO 12 + SERVO)
// =====================================================

void initializeGate() {
    pinMode(VEHICLE_SENSOR_1, INPUT);
    pinMode(VEHICLE_SENSOR_2, INPUT);

    entryGateServo.attach(ENTRY_SERVO_PIN);
    exitGateServo.attach(EXIT_SERVO_PIN);

    entryGateServo.write(GATE_CLOSED_ANGLE);
    exitGateServo.write(GATE_CLOSED_ANGLE);

    entryGateOpen = false;
    exitGateOpen = false;
    entryGateOpenedAt = 0;
    exitGateOpenedAt = 0;
    vehicleDetectionLatched = false;

    Serial.println("Gate initialized: CLOSED");
}

bool detectVehicle() {
    bool sensor1Active = digitalRead(VEHICLE_SENSOR_1) == VEHICLE_DETECTED_LEVEL;
    bool sensor2Active = digitalRead(VEHICLE_SENSOR_2) == VEHICLE_DETECTED_LEVEL;
    return sensor1Active || sensor2Active;
}

void openGate() {
    if (entryGateOpen) {
        return;
    }

    Serial.println("Opening gate...");
    entryGateServo.write(GATE_OPEN_ANGLE);
    entryGateOpen = true;
    entryGateOpenedAt = millis();
}

void closeGate() {
    if (!entryGateOpen) {
        return;
    }

    Serial.println("Closing gate...");
    entryGateServo.write(GATE_CLOSED_ANGLE);
    entryGateOpen = false;
    entryGateOpenedAt = 0;
}

void updateGate() {
    bool sensor1Active = digitalRead(VEHICLE_SENSOR_1) == VEHICLE_DETECTED_LEVEL;
    bool sensor2Active = digitalRead(VEHICLE_SENSOR_2) == VEHICLE_DETECTED_LEVEL;
    bool vehiclePresent = sensor1Active || sensor2Active;

    if (!entryGateOpen) {
        if (vehiclePresent && !vehicleDetectionLatched) {
            vehicleDetectionLatched = true;

            if (sensor1Active && sensor2Active) {
                Serial.println("Vehicle detected on GPIO 2 and GPIO 12.");
            } else if (sensor1Active) {
                Serial.println("Vehicle detected on GPIO 2.");
            } else {
                Serial.println("Vehicle detected on GPIO 12.");
            }

            openGate();
        }

        if (!vehiclePresent) {
            vehicleDetectionLatched = false;
        }
    } else if (millis() - entryGateOpenedAt >= GATE_OPEN_HOLD_MS) {
        closeGate();
    }

    if (exitGateOpen &&
        millis() - exitGateOpenedAt >= GATE_OPEN_HOLD_MS) {
        closeExitGate();
    }
}


// =====================================================
// ENTRY GATE (ULTRASONIC + BACKEND)
// =====================================================

void handleEntryGate() {

    float distance =
        readDistance(ENTRY_ECHO);

    if (!isValidUltrasonicDistance(distance)) {
        return;
    }

    if (isUltrasonicObjectDetected(distance)) {

        Serial.println(
            "Vehicle detected at ENTRY ultrasonic sensor."
        );

        /*
         * Automatic opening for GPIO 2 / GPIO 12 is handled
         * by updateGate(). Backend OPEN commands still apply
         * for the ultrasonic entry path.
         */
    }
}


// =====================================================
// EXIT GATE (ULTRASONIC)
// =====================================================

void handleExitGate() {

    float distance =
        readDistance(EXIT_ECHO);

    if (!isValidUltrasonicDistance(distance)) {
        return;
    }

    if (isUltrasonicObjectDetected(distance)) {

        Serial.println(
            "Vehicle detected at EXIT gate."
        );

        openExitGate();
    }
}


// =====================================================
// OPEN / CLOSE ENTRY GATE (BACKEND COMPATIBILITY)
// =====================================================

void openEntryGate() {
    openGate();
}

void closeEntryGate() {
    closeGate();
}


// =====================================================
// OPEN EXIT GATE
// =====================================================

void openExitGate() {

    if (!exitGateOpen) {

        Serial.println(
            "Opening EXIT gate..."
        );

        exitGateServo.write(
            GATE_OPEN_ANGLE
        );

        exitGateOpen = true;
        exitGateOpenedAt = millis();
    }
}


// =====================================================
// CLOSE EXIT GATE
// =====================================================

void closeExitGate() {

    if (exitGateOpen) {

        Serial.println(
            "Closing EXIT gate..."
        );

        exitGateServo.write(
            GATE_CLOSED_ANGLE
        );

        exitGateOpen = false;
        exitGateOpenedAt = 0;
    }
}


// =====================================================
// OLED DISPLAY
// =====================================================

void updateDisplay() {

    display.clearDisplay();

    display.setTextSize(1);

    display.setCursor(0, 0);

    display.println(
        "SMART PARKING"
    );

    display.setCursor(0, 16);

    display.print(
        "Available: "
    );

    display.println(
        availableSlots
    );

    display.setCursor(0, 28);

    display.print(
        "Occupied: "
    );

    display.println(
        8 - availableSlots
    );

    display.setCursor(0, 40);

    display.print(
        "S1 S2 S3 S4:"
    );

    display.setCursor(0, 52);

    for (int i = 0; i < 4; i++) {

        display.print(
            parkingOccupied[i]
            ? "X "
            : "O "
        );
    }

    display.display();
}


// =====================================================
// SEND PARKING DATA TO BACKEND
// =====================================================

void sendParkingStatusToBackend() {

    if (
        WiFi.status() != WL_CONNECTED
    ) {

        return;
    }

    HTTPClient http;

    String url =
        String(SERVER_URL) +
        "/api/parking/occupancy";

    http.begin(url);

    http.addHeader(
        "Content-Type",
        "application/json"
    );

    String json = "{";

    json += "\"availableSlots\":";
    json += availableSlots;
    json += ",";

    json += "\"occupiedSlots\":";
    json += (8 - availableSlots);
    json += ",";

    json += "\"slots\":[";

    for (int i = 0; i < 8; i++) {

        json += "{";

        json += "\"slot\":";
        json += (i + 1);
        json += ",";

        json += "\"occupied\":";
        json +=
            parkingOccupied[i]
            ? "true"
            : "false";

        json += ",";

        json += "\"distance\":";
        json += parkingDistance[i];

        json += "}";

        if (i < 7) {

            json += ",";
        }
    }

    json += "]";

    json += "}";

    int responseCode =
        http.POST(json);

    Serial.print(
        "Backend response: "
    );

    Serial.println(
        responseCode
    );

    http.end();
}


// =====================================================
// CHECK ENTRY GATE COMMAND
// =====================================================

void checkEntryGateCommand() {

    if (
        WiFi.status() != WL_CONNECTED
    ) {

        return;
    }

    HTTPClient http;

    String url =
        String(SERVER_URL) +
        "/api/gate/entry/command";

    http.begin(url);

    int responseCode =
        http.GET();

    if (responseCode == 200) {

        String response =
            http.getString();

        Serial.print(
            "Entry command: "
        );

        Serial.println(response);

        /*
         * Example backend response:
         *
         * OPEN
         */

        if (response.indexOf("OPEN") >= 0) {

            openEntryGate();
        }
    }

    http.end();
}


// =====================================================
// CHECK EXIT GATE COMMAND
// =====================================================

void checkExitGateCommand() {

    if (
        WiFi.status() != WL_CONNECTED
    ) {

        return;
    }

    HTTPClient http;

    String url =
        String(SERVER_URL) +
        "/api/gate/exit/command";

    http.begin(url);

    int responseCode =
        http.GET();

    if (responseCode == 200) {

        String response =
            http.getString();

        if (response.indexOf("OPEN") >= 0) {

            openExitGate();
        }
    }

    http.end();
}
