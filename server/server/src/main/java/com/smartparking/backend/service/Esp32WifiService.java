package com.smartparking.backend.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.smartparking.backend.exception.Esp32UnavailableException;
import com.smartparking.backend.exception.InvalidGateException;
import com.smartparking.backend.model.GateAction;
import com.smartparking.backend.model.GateStatus;
import com.smartparking.backend.model.GateType;

@Service
public class Esp32WifiService {

    private static final Logger log = LoggerFactory.getLogger(Esp32WifiService.class);
    private static final String TOKEN_HEADER = "X-Gate-Control-Token";

    private final URI baseUri;
    private final String gateControlToken;
    private final long commandTimeoutMs;
    private final long communicationTimeoutMs;
    private final HttpClient httpClient;

    private volatile GateStatus gateState;
    private volatile boolean gateStateConfirmed;
    private volatile Instant lastCommunicationAt;
    private volatile Instant pendingCommandAt;
    private volatile String commandState = "IDLE";
    private volatile String commandMessage = "No gate command has been sent.";
    private volatile GateStatus requestedState;
    private boolean connectionFailureLogged;

    public Esp32WifiService(
            @Value("${esp32.wifi.base-url:}") String baseUrl,
            @Value("${esp32.gate-control-token:}") String gateControlToken,
            @Value("${esp32.wifi.command-timeout-ms:5000}") long commandTimeoutMs,
            @Value("${esp32.wifi.connection-timeout-ms:2000}") long communicationTimeoutMs) {
        this.baseUri = parseBaseUri(baseUrl);
        this.gateControlToken = gateControlToken;
        this.commandTimeoutMs = commandTimeoutMs;
        this.communicationTimeoutMs = communicationTimeoutMs;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(communicationTimeoutMs))
                .build();
    }

    private URI parseBaseUri(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }

        URI uri = URI.create(baseUrl.trim());
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new IllegalArgumentException("ESP32 Wi-Fi base URL must be an HTTP(S) URL with a host.");
        }
        String value = uri.toString();
        return URI.create(value.endsWith("/") ? value : value + "/");
    }

    @Scheduled(fixedDelayString = "${esp32.wifi.poll-interval-ms:1000}")
    public synchronized void pollGateStatus() {
        if (!isConfigured()) {
            return;
        }

        try {
            HttpResponse<String> response = httpClient.send(
                    HttpRequest.newBuilder(baseUri.resolve("api/gate/status"))
                            .timeout(Duration.ofMillis(communicationTimeoutMs))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("ESP32 status endpoint returned HTTP " + response.statusCode() + ".");
            }

            GateStatus observed = parseGateState(response.body());
            if (observed == null) {
                throw new IOException("ESP32 returned an invalid gate status.");
            }
            lastCommunicationAt = Instant.now();
            gateState = observed;
            gateStateConfirmed = true;
            connectionFailureLogged = false;

            if ("PENDING".equals(commandState)) {
                if (observed == requestedState) {
                    commandState = "CONFIRMED";
                    commandMessage = "ESP32 reported gate state " + observed + ".";
                    pendingCommandAt = null;
                } else if (pendingCommandAt != null
                        && Duration.between(pendingCommandAt, Instant.now()).toMillis() >= commandTimeoutMs) {
                    commandState = "FAILED";
                    commandMessage = "Timed out waiting for Wi-Fi confirmation from the ESP32.";
                    pendingCommandAt = null;
                    log.warn(commandMessage);
                }
            }
        } catch (IOException exception) {
            markDisconnected(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            markDisconnected(exception);
        }
    }

    public boolean isConnected() {
        Instant last = lastCommunicationAt;
        return last != null
                && Duration.between(last, Instant.now()).toMillis() <= communicationTimeoutMs * 2;
    }

    public boolean isConfigured() {
        return baseUri != null;
    }

    public boolean isControlConfigured() {
        return isConfigured() && gateControlToken != null && !gateControlToken.isBlank();
    }

    public GateStatus getGateState() {
        return gateState;
    }

    public boolean isGateStateConfirmed() {
        return gateStateConfirmed && isConnected();
    }

    public Instant getLastCommunicationAt() {
        return lastCommunicationAt;
    }

    public String getCommandState() {
        return commandState;
    }

    public String getCommandMessage() {
        return commandMessage;
    }

    public GateStatus getRequestedState() {
        return requestedState;
    }

    public synchronized void requestCommand(GateType gateType, GateAction action) {
        if (gateType != GateType.ENTRANCE) {
            throw new InvalidGateException(
                    "The connected firmware controls one shared gate and cannot target a separate exit gate.");
        }
        if (!isControlConfigured()) {
            throw new Esp32UnavailableException(
                    "ESP32 Wi-Fi address or gate-control token is not configured.");
        }
        if (!isConnected()) {
            throw new Esp32UnavailableException("ESP32 is disconnected from Wi-Fi or not responding.");
        }
        if ("PENDING".equals(commandState)) {
            throw new IllegalStateException("A gate command is already pending confirmation.");
        }

        String endpoint = action == GateAction.OPEN ? "api/gate/open" : "api/gate/close";
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(endpoint))
                .timeout(Duration.ofMillis(communicationTimeoutMs))
                .header(TOKEN_HEADER, gateControlToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 409) {
                commandState = "REJECTED";
                commandMessage = response.body().isBlank()
                        ? "ESP32 rejected the gate command."
                        : response.body().trim();
                throw new IllegalStateException(commandMessage);
            }
            if (response.statusCode() != 200) {
                commandState = "FAILED";
                commandMessage = "ESP32 rejected the Wi-Fi gate command (HTTP " + response.statusCode() + ").";
                throw new Esp32UnavailableException(commandMessage);
            }

            requestedState = action == GateAction.OPEN ? GateStatus.OPEN : GateStatus.CLOSED;
            commandState = "PENDING";
            pendingCommandAt = Instant.now();
            commandMessage = action + " command sent over Wi-Fi; waiting for firmware status.";
        } catch (IOException exception) {
            markDisconnected(exception);
            throw new Esp32UnavailableException("Could not send a gate command over Wi-Fi.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            markDisconnected(exception);
            throw new Esp32UnavailableException("Wi-Fi gate command was interrupted.", exception);
        }
    }

    private GateStatus parseGateState(String response) {
        return switch (response.trim().toUpperCase(Locale.ROOT)) {
            case "OPEN" -> GateStatus.OPEN;
            case "CLOSED" -> GateStatus.CLOSED;
            default -> null;
        };
    }

    private synchronized void markDisconnected(Exception exception) {
        lastCommunicationAt = null;
        gateStateConfirmed = false;
        if ("PENDING".equals(commandState)) {
            commandState = "FAILED";
            commandMessage = "ESP32 disconnected before confirming the Wi-Fi gate command.";
            pendingCommandAt = null;
        }
        if (!connectionFailureLogged) {
            log.warn("ESP32 Wi-Fi communication failed: {}", exception.getMessage());
            connectionFailureLogged = true;
        }
    }
}
