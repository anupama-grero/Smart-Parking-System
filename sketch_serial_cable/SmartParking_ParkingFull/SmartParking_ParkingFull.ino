#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include <ESP32Servo.h>
#include <HTTPClient.h>
#include <WebServer.h>
#include <WiFi.h>
#include <Wire.h>

// Replace these with your own Wi-Fi and Spring Boot server details.
const char *WIFI_SSID = "Anupama Grero";
const char *WIFI_PASSWORD = "Anupama1234";
const char *SERVER_URL = "http://10.198.169.138:8080";
const char *GATE_CONTROL_TOKEN = "guard123";
const char *GATE_CONTROL_HEADER = "X-Gate-Control-Token";
const char *REQUEST_HEADERS[] = {GATE_CONTROL_HEADER};

// Create a web server on port 80
WebServer server(80);

// OLED
#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64
#define OLED_RESET -1
#define OLED_ADDRESS 0x3C
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);

// Four parking sensors
const int parkingTrigPins[4] = {5, 16, 15, 19};
const int parkingEchoPins[4] = {13, 33, 34, 32};

// Gate sensors
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
bool wasWiFiConnected = false;

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
void handleWebRoutes();
bool isGateRequestAuthorized();
void sendGateStatus(const char *gate, bool isOpen);

void setup() {
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

  // Setup Web Server Routes
  handleWebRoutes();
  server.collectHeaders(REQUEST_HEADERS, 1);
  server.begin();
  Serial.println("HTTP server started");
}

void loop() {
  unsigned long now = millis();

  // Handle Web Server Clients
  server.handleClient();

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
  updateWarnings();

  if (now - lastDisplayUpdate >= 500) {
    lastDisplayUpdate = now;
    updateOLED();
  }
}

// Sets up webpages to view data and control the system over Wi-Fi
void handleWebRoutes() {
  server.on("/", HTTP_GET, []() {
    String html = "<html lang='en'><head><meta charset='UTF-8'><meta "
                  "http-equiv='refresh' content='2'>";
    html += "<meta name='viewport' content='width=device-width, "
            "initial-scale=1.0'>";
    html += "<title>Smart Parking Dashboard</title>";
    html += "<style>body{font-family:Arial,sans-serif;text-align:center;"
            "background:#f4f4f9;color:#333;margin:0;padding:20px;}";
    html += ".card{background:white;padding:20px;margin:10px "
            "auto;border-radius:8px;max-width:400px;box-shadow:0 2px 4px "
            "rgba(0,0,0,0.1);}";
    html += ".btn{background:#007BFF;color:white;border:none;padding:10px "
            "20px;margin:5px;border-radius:5px;cursor:pointer;font-size:16px;}";
    html += ".btn-danger{background:#DC3545;}.occupied{color:red;font-weight:"
            "bold;}.available{color:green;font-weight:bold;}</style>";
    html += "</head><body>";
    html += "<h1>Smart Parking System</h1>";

    html += "<div class='card'>";
    html += "<h2>Gate Status</h2>";
    html +=
        "<p><b>State:</b> " + String(gateIsOpen ? "OPEN" : "CLOSED") + "</p>";
    html += "<p><a href='/open'><button class='btn'>Open Gate</button></a>";
    html += "<a href='/close'><button class='btn btn-danger'>Close "
            "Gate</button></a></p>";
    html += "</div>";

    html += "<div class='card'>";
    html += "<h2>Parking Status</h2>";
    html +=
        "<p><b>Available Slots:</b> " + String(getAvailableSlots()) + "/4</p>";
    for (int i = 0; i < 4; i++) {
      html += "<p>Slot " + String(i + 1) + ": ";
      if (slotOccupied[i]) {
        html += "<span class='occupied'>OCCUPIED</span>";
      } else {
        html += "<span class='available'>AVAILABLE</span>";
      }
      html +=
          " (" +
          (parkingDistances[i] < 0 ? "No Echo"
                                   : String(parkingDistances[i], 1) + " cm") +
          ")</p>";
    }
    html += "</div>";

    html += "</body></html>";
    server.send(200, "text/html", html);
  });

  server.on("/open", HTTP_GET, []() {
    if (!isParkingFull()) {
      manualOverride = true;
      openGate();
    }
    server.sendHeader("Location", "/");
    server.send(303);
  });

  server.on("/close", HTTP_GET, []() {
    manualOverride = false;
    closeGate();
    server.sendHeader("Location", "/");
    server.send(303);
  });

  server.on("/api/gate/status", HTTP_GET, []() {
    server.send(200, "text/plain", gateIsOpen ? "OPEN" : "CLOSED");
  });

  server.on("/api/gate/open", HTTP_POST, []() {
    if (!isGateRequestAuthorized()) {
      server.send(403, "text/plain", "Invalid gate-control token.");
      return;
    }
    if (isParkingFull()) {
      server.send(
          409, "text/plain",
          "ESP32 rejected OPEN because all four parking slots are occupied.");
      return;
    }
    manualOverride = true;
    openGate();
    server.send(200, "text/plain", "OPEN command accepted.");
  });

  server.on("/api/gate/close", HTTP_POST, []() {
    if (!isGateRequestAuthorized()) {
      server.send(403, "text/plain", "Invalid gate-control token.");
      return;
    }
    manualOverride = false;
    closeGate();
    server.send(200, "text/plain", "CLOSE command accepted.");
  });
}

bool isGateRequestAuthorized() {
  return server.hasHeader(GATE_CONTROL_HEADER) &&
         server.header(GATE_CONTROL_HEADER) == GATE_CONTROL_TOKEN;
}

// Reads one ultrasonic sensor. Returns -1 when no echo is received.
float readUltrasonicDistance(int trigPin, int echoPin, float timeoutMs) {
  digitalWrite(trigPin, LOW);
  delayMicroseconds(3);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);

  unsigned long timeoutUs = (unsigned long)(timeoutMs * 1000.0f);
  unsigned long duration = pulseIn(echoPin, HIGH, timeoutUs);
  if (duration == 0)
    return -1.0f;

  return (duration * 0.0343f) / 2.0f;
}

unsigned long readRawEchoUs(int trigPin, int echoPin) {
  digitalWrite(trigPin, LOW);
  delayMicroseconds(3);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);
  return pulseIn(echoPin, HIGH, 25000UL);
}

bool isGateDetected(float distanceCm) {
  return distanceCm >= 0.0f && distanceCm <= GATE_DETECTION_DISTANCE_CM;
}

bool detectVehicle() {
  gateDistance1 =
      readUltrasonicDistance(GATE_SENSOR_1_TRIG, GATE_SENSOR_1_ECHO, 25.0f);
  delay(20);
  gateDistance2 =
      readUltrasonicDistance(GATE_SENSOR_2_TRIG, GATE_SENSOR_2_ECHO, 25.0f);
  return isGateDetected(gateDistance1) || isGateDetected(gateDistance2);
}

void updateParkingOccupancy() {
  for (int i = 0; i < 4; i++) {
    float distance =
        readUltrasonicDistance(parkingTrigPins[i], parkingEchoPins[i], 25.0f);
    parkingDistances[i] = distance;

    if (distance >= 0.0f) {
      slotOccupied[i] = (distance <= PARKING_OCCUPIED_DISTANCE_CM);
    }
    delay(30);
  }
  slot2Distance = parkingDistances[1];
}

int getOccupiedSlots() {
  int occupied = 0;
  for (int i = 0; i < 4; i++) {
    if (slotOccupied[i])
      occupied++;
  }
  return occupied;
}

int getAvailableSlots() { return 4 - getOccupiedSlots(); }

bool isParkingFull() { return getOccupiedSlots() == 4; }

void updateReverseSafety() {
  slot2WarningActive =
      slot2Distance >= 0.0f && slot2Distance <= SLOT2_WARNING_DISTANCE_CM;
}

void updateWarnings() {
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

void updateOLED() {
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
    if (i < 3)
      display.print(" ");
  }

  display.setCursor(0, 29);
  if (isParkingFull()) {
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

void updateGate() {
  unsigned long now = millis();
  if (now - lastGateCheck < GATE_CHECK_INTERVAL_MS)
    return;
  lastGateCheck = now;

  detectVehicle();

  bool entryDetected = isGateDetected(gateDistance1);
  bool exitDetected = isGateDetected(gateDistance2);

  if (exitDetected) {
    openGate();
  } else if (entryDetected) {
    if (isParkingFull()) {
      closeGate();
    } else {
      openGate();
    }
  } else if (gateIsOpen && !manualOverride &&
             now - gateOpenedAt >= GATE_OPEN_DURATION_MS) {
    closeGate();
  }
}

void openGate() {
  if (gateIsOpen)
    return;
  gateServo.write(GATE_OPEN_ANGLE);
  gateIsOpen = true;
  gateOpenedAt = millis();
  sendGateStatus("entry", true);
}

void closeGate() {
  if (!gateIsOpen)
    return;
  gateServo.write(GATE_CLOSED_ANGLE);
  gateIsOpen = false;
  sendGateStatus("entry", false);
}

void initializeGate() {
  gateServo.setPeriodHertz(50);
  gateServo.attach(SERVO_PIN, 500, 2400);
  gateServo.write(GATE_CLOSED_ANGLE);
  gateIsOpen = false;
}

void connectWiFi() {
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  lastWiFiReconnect = millis();
  Serial.println("Connecting to Wi-Fi...");
}

void sendParkingStatusToBackend() {
  if (WiFi.status() != WL_CONNECTED)
    return;

  HTTPClient http;
  http.setConnectTimeout(1000);
  http.setTimeout(1000);

  String slotsJSON = "";
  for (int i = 0; i < 4; i++) {
    if (i > 0)
      slotsJSON += ",";
    slotsJSON += "{\"slotNumber\":" + String(i + 1);
    slotsJSON += ",\"occupied\":";
    slotsJSON += (slotOccupied[i] ? "true" : "false");
    slotsJSON += ",\"distanceCm\":";
    slotsJSON += (parkingDistances[i] < 0.0f ? String("-1")
                                             : String(parkingDistances[i], 2));
    slotsJSON += "}";
  }

  String payload = "{\"availableSlots\":" + String(getAvailableSlots());
  payload += ",\"occupiedSlots\":" + String(getOccupiedSlots());
  payload += ",\"slots\":[" + slotsJSON + "]}";

  String url = String(SERVER_URL) + "/api/sensors/occupancy/batch";
  http.begin(url);
  http.addHeader("Content-Type", "application/json");

  int responseCode = http.POST(payload);
  http.end();
}

void sendGateStatus(const char *gate, bool isOpen) {
  // Status changes are sent to the backend automatically via Wi-Fi batch
  // updates or can be viewed in browser.
}