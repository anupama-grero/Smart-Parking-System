package com.smartparking.backend.exception;

public class Esp32UnavailableException extends RuntimeException {
    public Esp32UnavailableException(String message) {
        super(message);
    }

    public Esp32UnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
