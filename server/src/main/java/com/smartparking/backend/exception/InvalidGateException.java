package com.smartparking.backend.exception;

public class InvalidGateException extends RuntimeException {
    public InvalidGateException(String message) {
        super(message);
    }
}
