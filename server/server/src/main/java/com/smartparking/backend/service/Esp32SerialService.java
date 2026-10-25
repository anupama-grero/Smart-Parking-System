package com.smartparking.backend.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.smartparking.backend.exception.Esp32UnavailableException;
import com.smartparking.backend.exception.InvalidGateException;
import com.smartparking.backend.model.GateAction;
import com.smartparking.backend.model.GateStatus;
import com.smartparking.backend.model.GateType;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class Esp32SerialService {

    private static final Logger log = LoggerFactory.getLogger(Esp32SerialService.class);

    private final String portName;
    private final int baudRate;
    private final long commandTimeoutMs;
    private final long communicationTimeoutMs;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "esp32-serial-command-timeouts");
        thread.setDaemon(true);
        return thread;
    });
    private final StringBuilder inputBuffer = new StringBuilder();

    private volatile SerialPort port;
    private volatile GateStatus gateState;
    private volatile boolean gateStateConfirmed;
    private volatile Instant lastCommunicationAt;
    private volatile String commandState = "IDLE";
    private volatile String commandMessage = "No gate command has been sent.";
    private volatile GateStatus requestedState;

    public Esp32SerialService(
            @Value("${esp32.serial.port:}") String portName,
            @Value("${esp32.serial.baud-rate:115200}") int baudRate,
            @Value("${esp32.serial.command-timeout-ms:3000}") long commandTimeoutMs,
            @Value("${esp32.serial.connection-timeout-ms:10000}") long communicationTimeoutMs) {
        this.portName = portName.trim();
        this.baudRate = baudRate;
        this.commandTimeoutMs = commandTimeoutMs;
        this.communicationTimeoutMs = communicationTimeoutMs;
    }

    @PostConstruct
    void start() {
        reconnect();
    }

    @Scheduled(fixedDelay = 5000)
    public synchronized void reconnect() {
        if (port != null && port.isOpen()) {
            Instant last = lastCommunicationAt;
            if (last != null
                    && Duration.between(last, Instant.now()).toMillis() <= communicationTimeoutMs) {
                return;
            }
            log.warn("ESP32 serial port {} stopped responding; reconnecting", portName);
            handleDisconnect();
        }
        if (portName.isBlank()) {
            return;
        }

        SerialPort candidate = SerialPort.getCommPort(portName);
        candidate.setComPortParameters(baudRate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        candidate.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
        if (!candidate.openPort()) {
            log.warn("ESP32 serial port {} is unavailable; will retry", portName);
            return;
        }

        candidate.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE
                        | SerialPort.LISTENING_EVENT_PORT_DISCONNECTED;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() == SerialPort.LISTENING_EVENT_PORT_DISCONNECTED) {
                    handleDisconnect();
                } else if (event.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                    readAvailable(candidate);
                }
            }
        });
        port = candidate;
        log.info("ESP32 serial port {} connected at {} baud", portName, baudRate);
        scheduler.schedule(() -> writeStatusQuery(candidate), 750, TimeUnit.MILLISECONDS);
    }

    public boolean isConnected() {
        SerialPort currentPort = port;
        Instant last = lastCommunicationAt;
        return currentPort != null
                && currentPort.isOpen()
                && last != null
                && Duration.between(last, Instant.now()).toMillis() <= communicationTimeoutMs;
    }

    public boolean isConfigured() {
        return !portName.isBlank();
    }

    public GateStatus getGateState() {
        return gateState;
    }

    public boolean isGateStateConfirmed() {
        return gateStateConfirmed;
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
        if (!isConnected()) {
            throw new Esp32UnavailableException("ESP32 USB serial port is disconnected or not configured.");
        }
        if ("PENDING".equals(commandState)) {
            throw new IllegalStateException("A gate command is already pending confirmation.");
        }

        requestedState = action == GateAction.OPEN ? GateStatus.OPEN : GateStatus.CLOSED;
        commandState = "PENDING";
        commandMessage = action + " command sent; waiting for firmware status.";

        SerialPort currentPort = port;
        byte[] command = (action.name() + "\n").getBytes(StandardCharsets.US_ASCII);
        if (currentPort.writeBytes(command, command.length) != command.length) {
            commandState = "FAILED";
            commandMessage = "The serial command could not be written to the ESP32.";
            throw new Esp32UnavailableException(commandMessage);
        }

        scheduler.schedule(() -> writeStatusQuery(currentPort), 200, TimeUnit.MILLISECONDS);
        scheduler.schedule(() -> timeoutPendingCommand(requestedState), commandTimeoutMs, TimeUnit.MILLISECONDS);
    }

    private void writeStatusQuery(SerialPort targetPort) {
        if (port != targetPort || !targetPort.isOpen()) {
            return;
        }
        byte[] status = "STATUS\n".getBytes(StandardCharsets.US_ASCII);
        if (targetPort.writeBytes(status, status.length) != status.length) {
            log.warn("Could not query ESP32 status over serial port {}", portName);
        }
    }

    private void readAvailable(SerialPort source) {
        byte[] bytes = new byte[Math.max(1, source.bytesAvailable())];
        int count = source.readBytes(bytes, bytes.length);
        if (count <= 0) {
            return;
        }

        synchronized (inputBuffer) {
            for (int i = 0; i < count; i++) {
                char character = (char) (bytes[i] & 0xff);
                if (character == '\n') {
                    String line = inputBuffer.toString().trim();
                    inputBuffer.setLength(0);
                    if (!line.isEmpty()) {
                        processLine(line);
                    }
                } else if (character != '\r') {
                    inputBuffer.append(character);
                }
            }
        }
    }

    private void processLine(String line) {
        lastCommunicationAt = Instant.now();
        log.debug("ESP32 serial: {}", line);
        String normalized = line.toUpperCase(Locale.ROOT);

        if (normalized.contains("PARKING FULL - MANUAL OPEN BLOCKED")) {
            commandState = "REJECTED";
            commandMessage = "ESP32 rejected OPEN because all four parking slots are occupied.";
            return;
        }

        GateStatus observed = null;
        if (normalized.contains("GATE_OPENED")
                || normalized.contains("\"STATE\":\"OPEN\"")
                || normalized.contains("GATE: OPEN")) {
            observed = GateStatus.OPEN;
        } else if (normalized.contains("GATE_CLOSED")
                || normalized.contains("\"STATE\":\"CLOSED\"")
                || normalized.contains("GATE: CLOSED")) {
            observed = GateStatus.CLOSED;
        }

        if (observed == null) {
            return;
        }

        gateState = observed;
        gateStateConfirmed = true;
        if ("PENDING".equals(commandState)) {
            if (observed == requestedState) {
                commandState = "CONFIRMED";
                commandMessage = "ESP32 reported gate state " + observed + ".";
            } else {
                commandState = "FAILED";
                commandMessage = "ESP32 reported " + observed + " instead of the requested " + requestedState + ".";
            }
        }
    }

    private void timeoutPendingCommand(GateStatus expectedState) {
        if ("PENDING".equals(commandState) && requestedState == expectedState) {
            commandState = "FAILED";
            commandMessage = "Timed out waiting for serial confirmation from the ESP32.";
            log.warn(commandMessage);
        }
    }

    private synchronized void handleDisconnect() {
        log.warn("ESP32 serial port {} disconnected", portName);
        SerialPort disconnectedPort = port;
        port = null;
        lastCommunicationAt = null;
        gateStateConfirmed = false;
        if ("PENDING".equals(commandState)) {
            commandState = "FAILED";
            commandMessage = "ESP32 disconnected before confirming the gate command.";
        }
        if (disconnectedPort != null && disconnectedPort.isOpen()) {
            disconnectedPort.closePort();
        }
    }

    @PreDestroy
    void stop() {
        scheduler.shutdownNow();
        SerialPort currentPort = port;
        port = null;
        if (currentPort != null && currentPort.isOpen()) {
            currentPort.removeDataListener();
            currentPort.closePort();
        }
    }
}
