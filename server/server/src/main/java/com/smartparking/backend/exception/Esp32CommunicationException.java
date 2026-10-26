package com.smartparking.backend.exception;

public class Esp32CommunicationException extends RuntimeException {
    public Esp32CommunicationException(String message) {
        super(message);
    }

    public Esp32CommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
