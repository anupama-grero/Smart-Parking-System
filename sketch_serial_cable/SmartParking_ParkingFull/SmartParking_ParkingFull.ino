#include <WiFi.h>
#include <HTTPClient.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include <ESP32Servo.h>

// Replace these with your own Wi-Fi and Spring Boot server details.
const char *WIFI_SSID = "Anupama Grero";
const char *WIFI_PASSWORD = "Anupama1234";
const char *SERVER_URL = "http://10.198.169.138:8080";

// OLED
#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64
#define OLED_RESET -1
#define OLED_ADDRESS 0x3C
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);

// Four parking sensors (kept from your uploaded code)
const int parkingTrigPins[4] = {5, 16, 15, 19};
const int parkingEchoPins[4] = {13, 33, 34, 32};

// Gate sensors (kept from your uploaded code)
#define GATE_SENSOR_1_TRIG 2
#define GATE_SENSOR_1_ECHO 23
#define GATE_SENSOR_2_TRIG 27
#define GATE_SENSOR_2_ECHO 39

// Servo
#define SERVO_PIN 18
#define GATE_CLOSED_ANGLE 0
#define GATE_OPEN_ANGLE 90

// Slot 2 warning outputs
#define BUZZER_PIN 4
#define RED_LED_PIN 25
#define GREEN_LED_PIN 26

// Timing and detection settings
#define GATE_DETECTION_DISTANCE_CM 5.0f
#define GATE_OPEN_DURATION_MS 3000UL
#define PARKING_UPDATE_INTERVAL_MS 250UL
#define GATE_CHECK_INTERVAL_MS 100UL
#define PARKING_OCCUPIED_DISTANCE_CM 5.0f
#define SLOT2_WARNING_DISTANCE_CM 3.0f
#define BACKEND_OCCUPANCY_INTERVAL_MS 1000UL
#define WIFI_RECONNECT_INTERVAL_MS 10000UL

Servo gateServo;

bool slotOccupied[4] = {false, false, false, false};
float parkingDistances[4] = {-1, -1, -1, -1};
float gateDistance1 = -1;
float gateDistance2 = -1;
float slot2Distance = -1;

bool gateIsOpen = false;
bool slot2WarningActive = false;
bool beepOutputOn = false;

// When the security guard manually opens via the dashboard button,
// manualOverride suppresses the auto-close timer until they press Close.
bool manualOverride = false;

unsigned long lastBeepToggle = 0;
unsigned long gateOpenedAt = 0;
unsigned long lastParkingUpdate = 0;
unsigned long lastGateCheck = 0;
unsigned long lastDisplayUpdate = 0;
unsigned long lastOccupancySend = 0;
unsigned long lastWiFiReconnect = 0;

float readUltrasonicDistance(int trigPin, int echoPin, float timeoutMs);
unsigned long readRawEchoUs(int trigPin, int echoPin);
bool isGateDetected(float distanceCm);
bool detectVehicle();
void updateParkingOccupancy();
int getOccupiedSlots();
int getAvailableSlots();
bool isParkingFull();
void updateReverseSafety();
void updateWarnings();
void updateOLED();
void updateGate();
void openGate();
void closeGate();
void initializeGate();
void connectWiFi();
void sendParkingStatusToBackend();
void handleSerialCommands();
void testParkingSensors();
void printSensorReading(int trigPin, int echoPin);
void sendGateStatus(const char *gate, bool isOpen);

void setup()
{
  Serial.begin(115200);
  Serial.setTimeout(50);

  for (int i = 0; i < 4; i++) {
    pinMode(parkingTrigPins[i], OUTPUT);
    digitalWrite(parkingTrigPins[i], LOW);
    pinMode(parkingEchoPins[i], INPUT);
  }

  pinMode(GATE_SENSOR_1_TRIG, OUTPUT);
  pinMode(GATE_SENSOR_1_ECHO, INPUT);
  pinMode(GATE_SENSOR_2_TRIG, OUTPUT);
  pinMode(GATE_SENSOR_2_ECHO, INPUT);
  digitalWrite(GATE_SENSOR_1_TRIG, LOW);
  digitalWrite(GATE_SENSOR_2_TRIG, LOW);

  pinMode(BUZZER_PIN, OUTPUT);
  pinMode(RED_LED_PIN, OUTPUT);
  pinMode(GREEN_LED_PIN, OUTPUT);
  digitalWrite(BUZZER_PIN, LOW);
  digitalWrite(RED_LED_PIN, LOW);
  digitalWrite(GREEN_LED_PIN, HIGH);

  Wire.begin(21, 22);
  if (!display.begin(SSD1306_SWITCHCAPVCC, OLED_ADDRESS)) {
    Serial.println("OLED initialization failed!");
  } else {
    display.clearDisplay();
    display.setTextSize(1);
    display.setTextColor(SSD1306_WHITE);
    display.setCursor(0, 0);
    display.println("Smart Parking");
    display.println("Initializing...");
    display.display();
  }

  initializeGate();
  connectWiFi();

  Serial.println();
  Serial.println("============================");
  Serial.println("SMART PARKING SYSTEM READY");
  Serial.println("Entry sensor: TRIG 2 / ECHO 23");
  Serial.println("Exit sensor: TRIG 27 / ECHO 39");
  Serial.println("Parking TRIG: 5, 16, 15, 19");
  Serial.println("Parking ECHO: 13, 33, 34, 32");
  Serial.println("Servo: GPIO 18");
  Serial.println("Commands: OPEN, CLOSE, STATUS, TEST");
  Serial.println("============================");
}

void loop()
{
  unsigned long now = millis();

  if (WiFi.status() != WL_CONNECTED &&
      now - lastWiFiReconnect >= WIFI_RECONNECT_INTERVAL_MS) {
    connectWiFi();
  }

  bool wifiConnected = (WiFi.status() == WL_CONNECTED);
  if (wifiConnected && !wasWiFiConnected) {
    Serial.println("Wi-Fi connected");
    Serial.print("ESP32 IP: ");
    Serial.println(WiFi.localIP());
  } else if (!wifiConnected && wasWiFiConnected) {
    Serial.println("Wi-Fi disconnected");
  }
  wasWiFiConnected = wifiConnected;

  if (now - lastParkingUpdate >= PARKING_UPDATE_INTERVAL_MS) {
    lastParkingUpdate = now;
    updateParkingOccupancy();
    updateReverseSafety();

    if (now - lastOccupancySend >= BACKEND_OCCUPANCY_INTERVAL_MS) {
      lastOccupancySend = now;
      sendParkingStatusToBackend();
    }
  }

  updateGate();
  handleSerialCommands();
  updateWarnings();

  if (now - lastDisplayUpdate >= 500) {
    lastDisplayUpdate = now;
    updateOLED();
  }
}

// Reads one ultrasonic sensor. Returns -1 when no echo is received.
float readUltrasonicDistance(int trigPin, int echoPin, float timeoutMs)
{
  digitalWrite(trigPin, LOW);
  delayMicroseconds(3);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);

  unsigned long timeoutUs = (unsigned long)(timeoutMs * 1000.0f);
  unsigned long duration = pulseIn(echoPin, HIGH, timeoutUs);
  if (duration == 0) return -1.0f;

  return (duration * 0.0343f) / 2.0f;
}

unsigned long readRawEchoUs(int trigPin, int echoPin)
{
  digitalWrite(trigPin, LOW);
  delayMicroseconds(3);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);
  return pulseIn(echoPin, HIGH, 25000UL);
}

bool isGateDetected(float distanceCm)
{
  return distanceCm >= 0.0f &&
         distanceCm <= GATE_DETECTION_DISTANCE_CM;
}

bool detectVehicle()
{
  gateDistance1 = readUltrasonicDistance(
      GATE_SENSOR_1_TRIG, GATE_SENSOR_1_ECHO, 25.0f);

  delay(20);

  gateDistance2 = readUltrasonicDistance(
      GATE_SENSOR_2_TRIG, GATE_SENSOR_2_ECHO, 25.0f);

  Serial.print("Entry Sensor (ECHO GPIO ");
  Serial.print(GATE_SENSOR_1_ECHO);
  Serial.print("): ");
  if (gateDistance1 < 0) Serial.print("NO ECHO");
  else Serial.print(gateDistance1, 2);
  Serial.print(" cm | Exit Sensor (ECHO GPIO ");
  Serial.print(GATE_SENSOR_2_ECHO);
  Serial.print("): ");
  if (gateDistance2 < 0) Serial.print("NO ECHO");
  else Serial.print(gateDistance2, 2);
  Serial.println(" cm");

  return isGateDetected(gateDistance1) || isGateDetected(gateDistance2);
}

void updateParkingOccupancy()
{
  for (int i = 0; i < 4; i++) {
    float distance = readUltrasonicDistance(
        parkingTrigPins[i], parkingEchoPins[i], 25.0f);

    parkingDistances[i] = distance;

    // Keep the last known slot state if the sensor gives no valid echo.
    if (distance >= 0.0f) {
      slotOccupied[i] = (distance <= PARKING_OCCUPIED_DISTANCE_CM);
    }

    delay(30);
  }

  slot2Distance = parkingDistances[1];
}

int getOccupiedSlots()
{
  int occupied = 0;
  for (int i = 0; i < 4; i++) {
    if (slotOccupied[i]) occupied++;
  }
  return occupied;
}

int getAvailableSlots()
{
  return 4 - getOccupiedSlots();
}

bool isParkingFull()
{
  return getOccupiedSlots() == 4;
}

void updateReverseSafety()
{
  slot2WarningActive =
      slot2Distance >= 0.0f &&
      slot2Distance <= SLOT2_WARNING_DISTANCE_CM;

  Serial.print("{\"type\":\"REVERSE_WARNING\",\"slot\":2,\"active\":");
  Serial.print(slot2WarningActive ? "true" : "false");
  Serial.println("}");
}

void updateWarnings()
{
  if (!slot2WarningActive) {
    digitalWrite(BUZZER_PIN, LOW);
    digitalWrite(RED_LED_PIN, LOW);
    digitalWrite(GREEN_LED_PIN, HIGH);
    beepOutputOn = false;
    return;
  }

  digitalWrite(RED_LED_PIN, HIGH);
  digitalWrite(GREEN_LED_PIN, LOW);

  unsigned long interval = beepOutputOn ? 150UL : 850UL;
  if (millis() - lastBeepToggle >= interval) {
    lastBeepToggle = millis();
    beepOutputOn = !beepOutputOn;
    digitalWrite(BUZZER_PIN, beepOutputOn ? HIGH : LOW);
  }
}

void updateOLED()
{
  display.clearDisplay();
  display.setTextSize(1);
  display.setTextColor(SSD1306_WHITE);
  display.setCursor(0, 0);
  display.println("SMART PARKING");

  display.setCursor(0, 14);
  for (int i = 0; i < 4; i++) {
    display.print("S");
    display.print(i + 1);
    display.print(":");
    display.print(slotOccupied[i] ? "X" : "O");
    if (i < 3) display.print(" ");
  }

  display.setCursor(0, 29);
  if (isParkingFull()) {
    display.setTextSize(1);
    display.println("PARKING FULL");
  } else {
    display.print("AVAILABLE: ");
    display.print(getAvailableSlots());
    display.println("/4");
  }

  display.setCursor(0, 46);
  display.print("GATE: ");
  display.println(gateIsOpen ? "OPEN" : "CLOSED");
  display.display();
}

void updateGate()
{
  unsigned long now = millis();
  if (now - lastGateCheck < GATE_CHECK_INTERVAL_MS) return;
  lastGateCheck = now;

  detectVehicle();

  bool entryDetected = isGateDetected(gateDistance1);
  bool exitDetected = isGateDetected(gateDistance2);

  // Exit is allowed even when the parking area is full.
  if (exitDetected) {
    openGate();
  } else if (entryDetected) {
    if (isParkingFull()) {
      Serial.println("PARKING FULL - entry gate remains closed");
      closeGate();
    } else {
      openGate();
    }
  } else if (gateIsOpen && !manualOverride &&
             now - gateOpenedAt >= GATE_OPEN_DURATION_MS) {
    closeGate();
  }
}

void openGate()
{
  if (gateIsOpen) return;

  gateServo.write(GATE_OPEN_ANGLE);
  gateIsOpen = true;
  gateOpenedAt = millis();

  Serial.println("GATE_OPENED");
  sendGateStatus("entry", true);
}

void closeGate()
{
  if (!gateIsOpen) return;

  gateServo.write(GATE_CLOSED_ANGLE);
  gateIsOpen = false;

  Serial.println("GATE_CLOSED");
  sendGateStatus("entry", false);
}

void initializeGate()
{
  gateServo.setPeriodHertz(50);
  gateServo.attach(SERVO_PIN, 500, 2400);
  gateServo.write(GATE_CLOSED_ANGLE);
  gateIsOpen = false;
}

void connectWiFi()
{
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  lastWiFiReconnect = millis();
  Serial.println("Connecting to Wi-Fi...");
}

void sendParkingStatusToBackend()
{
  if (WiFi.status() != WL_CONNECTED) return;

  HTTPClient http;
  http.setConnectTimeout(1000);
  http.setTimeout(1000);

  String slotsJSON = "";
  for (int i = 0; i < 4; i++) {
    if (i > 0) slotsJSON += ",";
    slotsJSON += "{\"slotNumber\":" + String(i + 1);
    slotsJSON += ",\"occupied\":";
    slotsJSON += (slotOccupied[i] ? "true" : "false");
    slotsJSON += ",\"distanceCm\":";
    slotsJSON += (parkingDistances[i] < 0.0f ? String("-1") : String(parkingDistances[i], 2));
    slotsJSON += "}";
  }

  String payload = "{\"availableSlots\":" + String(getAvailableSlots());
  payload += ",\"occupiedSlots\":" + String(getOccupiedSlots());
  payload += ",\"slots\":[" + slotsJSON + "]}";

  String url = String(SERVER_URL) + "/api/sensors/occupancy/batch";
  http.begin(url);
  http.addHeader("Content-Type", "application/json");

  int responseCode = http.POST(payload);
  if (responseCode >= 200 && responseCode < 300) {
    Serial.print("Parking data uploaded (HTTP ");
    Serial.print(responseCode);
    Serial.println(")");
  } else {
    Serial.print("Parking data upload failed: ");
    Serial.println(responseCode);
  }
  http.end();
}

void sendGateStatus(const char *gate, bool isOpen)
{
  Serial.print("{\"type\":\"GATE_STATUS\",\"gate\":\"");
  Serial.print(gate);
  Serial.print("\",\"state\":\"");
  Serial.print(isOpen ? "OPEN" : "CLOSED");
  Serial.println("\"}");
}

void handleSerialCommands()
{
  if (!Serial.available()) return;

  String command = Serial.readStringUntil('\n');
  command.trim();
  String upperCommand = command;
  upperCommand.toUpperCase();

  if (upperCommand == "OPEN" ||
      upperCommand == "OPEN_GATE" ||
      upperCommand == "OPEN_ENTRY") {
    if (isParkingFull()) {
      Serial.println("PARKING FULL - manual OPEN blocked");
    } else {
      manualOverride = true;
      openGate();
    }
  } else if (upperCommand == "CLOSE" ||
             upperCommand == "CLOSE_GATE" ||
             upperCommand == "CLOSE_ENTRY") {
    manualOverride = false;
    closeGate();
  } else if (upperCommand == "STATUS") {
    Serial.println("----- SYSTEM STATUS -----");
    Serial.print("Gate: ");
    Serial.println(gateIsOpen ? "OPEN" : "CLOSED");
    Serial.print("Occupied slots: ");
    Serial.println(getOccupiedSlots());
    Serial.print("Available slots: ");
    Serial.println(getAvailableSlots());
    Serial.print("Parking full: ");
    Serial.println(isParkingFull() ? "YES" : "NO");

    for (int i = 0; i < 4; i++) {
      Serial.print("Slot ");
      Serial.print(i + 1);
      Serial.print(": ");
      Serial.print(slotOccupied[i] ? "OCCUPIED" : "AVAILABLE");
      Serial.print(" | Distance: ");
      if (parkingDistances[i] < 0.0f) Serial.println("NO ECHO");
      else {
        Serial.print(parkingDistances[i], 2);
        Serial.println(" cm");
      }
    }
  } else if (upperCommand == "TEST") {
    testParkingSensors();
    Serial.println("----- GATE SENSOR TEST -----");
    Serial.print("Entry: ");
    printSensorReading(GATE_SENSOR_1_TRIG, GATE_SENSOR_1_ECHO);
    Serial.print("Exit: ");
    printSensorReading(GATE_SENSOR_2_TRIG, GATE_SENSOR_2_ECHO);
  } else {
    Serial.println("Unknown command. Use OPEN, CLOSE, STATUS, or TEST.");
  }
}

void testParkingSensors()
{
  Serial.println();
  Serial.println("========== PARKING SENSOR TEST ==========");

  for (int i = 0; i < 4; i++) {
    Serial.print("Slot ");
    Serial.print(i + 1);
    Serial.print(" | TRIG GPIO ");
    Serial.print(parkingTrigPins[i]);
    Serial.print(" | ECHO GPIO ");
    Serial.println(parkingEchoPins[i]);

    unsigned long rawUs = readRawEchoUs(
        parkingTrigPins[i], parkingEchoPins[i]);

    if (rawUs == 0) {
      Serial.println("NO ECHO - check sensor and wiring");
    } else {
      float distanceCm = (rawUs * 0.0343f) / 2.0f;
      Serial.print("Raw: ");
      Serial.print(rawUs);
      Serial.print(" us | Distance: ");
      Serial.print(distanceCm, 2);
      Serial.println(" cm");
    }
    delay(100);
  }
  Serial.println("==========================================");
}

void printSensorReading(int trigPin, int echoPin)
{
  float distanceCm = readUltrasonicDistance(trigPin, echoPin, 25.0f);
  if (distanceCm < 0.0f) {
    Serial.println("NO ECHO");
  } else {
    Serial.print(distanceCm, 2);
    Serial.println(" cm");
  }
}
