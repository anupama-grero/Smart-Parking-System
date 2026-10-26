package com.smartparking.backend.dto;

import java.time.Instant;

public class GateStatusResponse {
    private String esp32Status;
    private String entryGate;
    private String exitGate;
    private String message;
    private String timestamp;
    private boolean hardwareConnected;
    private boolean gateStateConfirmed;
    private boolean gateControlEnabled;
    private boolean entryOpenAllowed;
    private String commandState;
    private String requestedGateState;
    private String commandMessage;
    private String lastHardwareUpdate;

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

    public boolean isHardwareConnected() {
        return hardwareConnected;
    }

    public void setHardwareConnected(boolean hardwareConnected) {
        this.hardwareConnected = hardwareConnected;
    }

    public boolean isGateStateConfirmed() {
        return gateStateConfirmed;
    }

    public void setGateStateConfirmed(boolean gateStateConfirmed) {
        this.gateStateConfirmed = gateStateConfirmed;
    }

    public boolean isGateControlEnabled() {
        return gateControlEnabled;
    }

    public void setGateControlEnabled(boolean gateControlEnabled) {
        this.gateControlEnabled = gateControlEnabled;
    }

    public boolean isEntryOpenAllowed() {
        return entryOpenAllowed;
    }

    public void setEntryOpenAllowed(boolean entryOpenAllowed) {
        this.entryOpenAllowed = entryOpenAllowed;
    }

    public String getCommandState() {
        return commandState;
    }

    public void setCommandState(String commandState) {
        this.commandState = commandState;
    }

    public String getRequestedGateState() {
        return requestedGateState;
    }

    public void setRequestedGateState(String requestedGateState) {
        this.requestedGateState = requestedGateState;
    }

    public String getCommandMessage() {
        return commandMessage;
    }

    public void setCommandMessage(String commandMessage) {
        this.commandMessage = commandMessage;
    }

    public String getLastHardwareUpdate() {
        return lastHardwareUpdate;
    }

    public void setLastHardwareUpdate(String lastHardwareUpdate) {
        this.lastHardwareUpdate = lastHardwareUpdate;
    }
}
