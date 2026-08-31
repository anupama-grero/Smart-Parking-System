package com.smartparking.backend.model;

public enum GateType {
    ENTRANCE,
    ENTRY,
    EXIT;

    public static GateType fromString(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim().toUpperCase();
        if ("ENTRY".equals(normalized) || "ENTRANCE".equals(normalized)) {
            return ENTRANCE;
        }
        if ("EXIT".equals(normalized)) {
            return EXIT;
        }
        throw new IllegalArgumentException("Unknown gate type: " + text);
    }
}
