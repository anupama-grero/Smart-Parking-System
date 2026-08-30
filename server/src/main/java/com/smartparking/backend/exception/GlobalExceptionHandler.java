package com.smartparking.backend.exception;

import com.smartparking.backend.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Esp32UnavailableException.class)
    public ResponseEntity<ApiResponse<Object>> handleEsp32Unavailable(Esp32UnavailableException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error("ESP32 device unavailable: " + ex.getMessage()));
    }

    @ExceptionHandler(Esp32CommunicationException.class)
    public ResponseEntity<ApiResponse<Object>> handleEsp32Communication(Esp32CommunicationException ex) {
        return ResponseEntity
                .status(HttpStatus.GATEWAY_TIMEOUT)
                .body(ApiResponse.error("ESP32 communication timeout/error: " + ex.getMessage()));
    }

    @ExceptionHandler({InvalidGateException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiResponse<Object>> handleInvalidRequest(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Invalid request: " + ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error: " + ex.getMessage()));
    }
}
