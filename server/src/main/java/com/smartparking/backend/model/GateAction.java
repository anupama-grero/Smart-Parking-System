package com.smartparking.backend.model;

public enum GateAction {
    OPEN,
    CLOSE;

    public static GateAction fromString(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim().toUpperCase();
        if ("OPEN".equals(normalized)) {
            return OPEN;
        }
        if ("CLOSE".equals(normalized) || "CLOSED".equals(normalized)) {
            return CLOSE;
        }
        throw new IllegalArgumentException("Unknown gate action: " + text);
    }
}
