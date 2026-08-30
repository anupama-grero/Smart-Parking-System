package com.smartparking.backend.dto;

import java.time.Instant;

public class GateStatusResponse {
    private String esp32Status;
    private String entryGate;
    private String exitGate;
    private String message;
    private String timestamp;

    public GateStatusResponse() {
        this.timestamp = Instant.now().toString();
    }

    public GateStatusResponse(String esp32Status, String entryGate, String exitGate, String message) {
        this.esp32Status = esp32Status;
        this.entryGate = entryGate;
        this.exitGate = exitGate;
        this.message = message;
        this.timestamp = Instant.now().toString();
    }

    public String getEsp32Status() {
        return esp32Status;
    }

    public void setEsp32Status(String esp32Status) {
        this.esp32Status = esp32Status;
    }

    public String getEntryGate() {
        return entryGate;
    }

    public void setEntryGate(String entryGate) {
        this.entryGate = entryGate;
    }

    public String getExitGate() {
        return exitGate;
    }

    public void setExitGate(String exitGate) {
        this.exitGate = exitGate;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
