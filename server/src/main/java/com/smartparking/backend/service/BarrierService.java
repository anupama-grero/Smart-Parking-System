package com.smartparking.backend.service;

import com.smartparking.backend.dto.GateStatusResponse;
import com.smartparking.backend.exception.Esp32CommunicationException;
import com.smartparking.backend.exception.Esp32UnavailableException;
import com.smartparking.backend.exception.InvalidGateException;
import com.smartparking.backend.model.Gate;
import com.smartparking.backend.model.GateAction;
import com.smartparking.backend.model.GateStatus;
import com.smartparking.backend.model.GateType;
import com.smartparking.backend.repository.GateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class BarrierService {

    private final GateRepository gateRepository;
    private final RestTemplate restTemplate;
    private final String esp32BaseUrl;
    private final boolean mockModeEnabled;

    private GateStatus entranceGateState = GateStatus.CLOSED;
    private GateStatus exitGateState = GateStatus.CLOSED;

    @Autowired
    public BarrierService(
            GateRepository gateRepository,
            @Value("${esp32.base-url:http://192.168.1.100}") String esp32BaseUrl,
            @Value("${esp32.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${esp32.read-timeout-ms:3000}") int readTimeoutMs,
            @Value("${esp32.mock-mode-enabled:true}") boolean mockModeEnabled
    ) {
        this.gateRepository = gateRepository;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        this.restTemplate = new RestTemplate(factory);
        this.esp32BaseUrl = esp32BaseUrl;
        this.mockModeEnabled = mockModeEnabled;
    }

    // Convenience constructor for tests without repository
    public BarrierService(
            String esp32BaseUrl,
            int connectTimeoutMs,
            int readTimeoutMs,
            boolean mockModeEnabled
    ) {
        this(null, esp32BaseUrl, connectTimeoutMs, readTimeoutMs, mockModeEnabled);
    }

    public GateStatusResponse getGateStatus() {
        String esp32Health = checkEsp32Health();
        
        String entryStatus = entranceGateState.name();
        String exitStatus = exitGateState.name();

        if (gateRepository != null) {
            Optional<Gate> entranceGate = gateRepository.findByGateType(GateType.ENTRANCE);
            if (entranceGate.isPresent()) {
                entryStatus = entranceGate.get().getStatus().name();
                entranceGateState = entranceGate.get().getStatus();
            }

            Optional<Gate> exitGate = gateRepository.findByGateType(GateType.EXIT);
            if (exitGate.isPresent()) {
                exitStatus = exitGate.get().getStatus().name();
                exitGateState = exitGate.get().getStatus();
            }
        }

        return new GateStatusResponse(
                esp32Health,
                entryStatus,
                exitStatus,
                "Gate status fetched successfully"
        );
    }

    public String checkEsp32Health() {
        if (mockModeEnabled) {
            return "Online (Mock)";
        }
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(esp32BaseUrl + "/api/health", Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return "Online";
            }
            return "Offline";
        } catch (ResourceAccessException e) {
            return "Offline";
        } catch (Exception e) {
            return "Offline";
        }
    }

    @Transactional
    public GateStatusResponse controlBarrier(GateType gateType, GateAction action) {
        if (gateType == null) {
            throw new InvalidGateException("Gate type must be specified (ENTRANCE or EXIT).");
        }
        if (action == null) {
            throw new InvalidGateException("Gate action must be specified (OPEN or CLOSE).");
        }

        GateType canonicalType = (gateType == GateType.ENTRY) ? GateType.ENTRANCE : gateType;
        String targetGateName = (canonicalType == GateType.ENTRANCE) ? "entrance" : "exit";
        String targetActionName = action.name().toLowerCase();

        // 1. ESP32 communication logic (Hardware Control)
        sendControlCommandToEsp32(targetGateName, targetActionName);

        // 2. Update internal in-memory barrier state
        GateStatus targetStatus = (action == GateAction.OPEN) ? GateStatus.OPEN : GateStatus.CLOSED;
        if (canonicalType == GateType.ENTRANCE) {
            entranceGateState = targetStatus;
        } else if (canonicalType == GateType.EXIT) {
            exitGateState = targetStatus;
        }

        // 3. Persist gate status into database if repository is available
        if (gateRepository != null) {
            Gate gate = gateRepository.findByGateType(canonicalType)
                    .orElseGet(() -> new Gate(
                            (canonicalType == GateType.ENTRANCE) ? "Main Entrance Gate" : "Main Exit Gate",
                            canonicalType,
                            targetStatus
                    ));
            gate.setStatus(targetStatus);
            gate.setUpdatedAt(LocalDateTime.now());
            gateRepository.save(gate);
        }

        String actionPastTense = (action == GateAction.OPEN) ? "opened" : "closed";
        String successMsg = String.format("%s barrier %s successfully!",
                targetGateName.substring(0, 1).toUpperCase() + targetGateName.substring(1),
                actionPastTense);

        return new GateStatusResponse(
                checkEsp32Health(),
                entranceGateState.name(),
                exitGateState.name(),
                successMsg
        );
    }

    public GateStatusResponse openEntranceBarrier() {
        return controlBarrier(GateType.ENTRANCE, GateAction.OPEN);
    }

    public GateStatusResponse closeEntranceBarrier() {
        return controlBarrier(GateType.ENTRANCE, GateAction.CLOSE);
    }

    public GateStatusResponse openExitBarrier() {
        return controlBarrier(GateType.EXIT, GateAction.OPEN);
    }

    public GateStatusResponse closeExitBarrier() {
        return controlBarrier(GateType.EXIT, GateAction.CLOSE);
    }

    private void sendControlCommandToEsp32(String gate, String action) {
        if (mockModeEnabled) {
            // Mock ESP32 device simulation always succeeds
            return;
        }

        String url = String.format("%s/api/barrier/control?gate=%s&action=%s", esp32BaseUrl, gate, action);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new Esp32CommunicationException("ESP32 responded with non-success status code: " + response.getStatusCode());
            }
        } catch (ResourceAccessException e) {
            throw new Esp32UnavailableException("ESP32 device is unreachable at " + esp32BaseUrl, e);
        } catch (Exception e) {
            if (e instanceof Esp32UnavailableException || e instanceof Esp32CommunicationException) {
                throw e;
            }
            throw new Esp32CommunicationException("Failed to communicate with ESP32 device: " + e.getMessage(), e);
        }
    }

    // Getters and Setters for state management testing
    public GateStatus getEntranceGateState() {
        return entranceGateState;
    }

    public void setEntranceGateState(GateStatus entranceGateState) {
        this.entranceGateState = entranceGateState;
    }

    public GateStatus getExitGateState() {
        return exitGateState;
    }

    public void setExitGateState(GateStatus exitGateState) {
        this.exitGateState = exitGateState;
    }

    public GateRepository getGateRepository() {
        return gateRepository;
    }
}
