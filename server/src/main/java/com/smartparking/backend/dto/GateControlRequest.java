package com.smartparking.backend.dto;

public class GateControlRequest {
    private String gate;
    private String action;

    public GateControlRequest() {
    }

    public GateControlRequest(String gate, String action) {
        this.gate = gate;
        this.action = action;
    }

    public String getGate() {
        return gate;
    }

    public void setGate(String gate) {
        this.gate = gate;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
