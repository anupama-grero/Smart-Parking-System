package com.smartparking.backend.service;

import java.time.Instant;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.smartparking.backend.dto.GateStatusResponse;
import com.smartparking.backend.exception.InvalidGateException;
import com.smartparking.backend.model.Gate;
import com.smartparking.backend.model.GateAction;
import com.smartparking.backend.model.GateStatus;
import com.smartparking.backend.model.GateType;
import com.smartparking.backend.repository.GateRepository;
import com.smartparking.backend.repository.ParkingSlotRepository;

@Service
public class BarrierService {

    private final GateRepository gateRepository;
    private final ParkingSlotRepository slotRepository;
    private final Esp32SerialService serialService;
    private final SensorOccupancyService occupancyService;
    private final String gateControlToken;
    private final int staleAfterSeconds;

    public BarrierService(
            GateRepository gateRepository,
            ParkingSlotRepository slotRepository,
            Esp32SerialService serialService,
            SensorOccupancyService occupancyService,
            @Value("${esp32.gate-control-token:}") String gateControlToken,
            @Value("${parking.sensor.stale-after-seconds:10}") int staleAfterSeconds) {
        this.gateRepository = gateRepository;
        this.slotRepository = slotRepository;
        this.serialService = serialService;
        this.occupancyService = occupancyService;
        this.gateControlToken = gateControlToken;
        this.staleAfterSeconds = staleAfterSeconds;
    }

    public GateStatusResponse getGateStatus() {
        syncReportedGateState();
        String entryStatus = serialService.getGateState() == null
                ? "UNKNOWN"
                : serialService.getGateState().name();
        String espStatus = serialService.isConnected()
                ? "Connected"
                : serialService.isConfigured() ? "Disconnected" : "Not configured";

        GateStatusResponse response = new GateStatusResponse(
                espStatus,
                entryStatus,
                "UNSUPPORTED",
                "Firmware exposes one shared gate; exit movement remains sensor-controlled.");
        response.setHardwareConnected(serialService.isConnected());
        response.setGateStateConfirmed(serialService.isGateStateConfirmed());
        response.setCommandState(serialService.getCommandState());
        response.setRequestedGateState(serialService.getRequestedState() == null
                ? null
                : serialService.getRequestedState().name());
        response.setCommandMessage(serialService.getCommandMessage());
        Instant lastUpdate = serialService.getLastCommunicationAt();
        response.setLastHardwareUpdate(lastUpdate == null ? null : lastUpdate.toString());
        response.setGateControlEnabled(serialService.isConnected() && !gateControlToken.isBlank());
        response.setEntryOpenAllowed(
                response.isGateControlEnabled() && !isParkingFull() && isParkingDataTrusted());
        return response;
    }

    public String checkEsp32Health() {
        if (serialService.isConnected()) {
            return "Connected";
        }
        return serialService.isConfigured() ? "Disconnected" : "Not configured";
    }

    /**
     * Legacy polling route is retained for older firmware, but only reports
     * confirmed state and fails closed while full or unknown.
     */
    public String getGateCommand(GateType gateType) {
        if (gateType != GateType.ENTRANCE || !serialService.isGateStateConfirmed()) {
            return "CLOSED";
        }
        if (isParkingFull()) {
            return "CLOSED";
        }
        return serialService.getGateState() == GateStatus.OPEN ? "OPEN" : "CLOSED";
    }

    public String getEntryGateCommand() {
        return getGateCommand(GateType.ENTRANCE);
    }

    public String getExitGateCommand() {
        return "CLOSED";
    }

    @Transactional
    public GateStatusResponse controlBarrier(GateType gateType, GateAction action) {
        if (gateType == null || action == null) {
            throw new InvalidGateException("Gate type and action must be specified.");
        }
        if (gateType != GateType.ENTRANCE) {
            throw new InvalidGateException(
                    "This firmware has one shared gate and does not support separate remote exit commands.");
        }
        if (action == GateAction.OPEN && isParkingFull()) {
            throw new IllegalStateException("Entry is blocked because all four parking slots are occupied.");
        }
        if (action == GateAction.OPEN && !isParkingDataTrusted()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Entry cannot be opened until fresh readings for all four parking slots are available.");
        }

        serialService.requestCommand(gateType, action);
        GateStatusResponse response = getGateStatus();
        response.setMessage(action + " command accepted; physical status is pending ESP32 confirmation.");
        return response;
    }

    @Transactional
    public GateStatusResponse recordGateStatus(GateType gateType, GateStatus status) {
        if (gateType == null || status == null) {
            throw new InvalidGateException("Gate type and state must be specified.");
        }
        if (gateType != GateType.ENTRANCE) {
            throw new InvalidGateException("The connected firmware reports one shared physical gate.");
        }

        updateGateState(gateType, status);
        return getGateStatus();
    }

    @Transactional
    public GateStatusResponse openEntranceBarrier() {
        return controlBarrier(GateType.ENTRANCE, GateAction.OPEN);
    }

    @Transactional
    public GateStatusResponse closeEntranceBarrier() {
        return controlBarrier(GateType.ENTRANCE, GateAction.CLOSE);
    }

    public GateStatusResponse openExitBarrier() {
        throw new InvalidGateException(
                "The unchanged firmware has no separate remote exit command; the exit sensor operates the shared gate.");
    }

    public GateStatusResponse closeExitBarrier() {
        throw new InvalidGateException(
                "The unchanged firmware has no separate remote exit command; the exit sensor operates the shared gate.");
    }

    public boolean isGateControlConfigured() {
        return !gateControlToken.isBlank();
    }

    public boolean isGateControlTokenValid(String providedToken) {
        if (providedToken == null || gateControlToken.isBlank()) {
            return false;
        }
        return java.security.MessageDigest.isEqual(
                gateControlToken.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                providedToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public boolean isParkingFull() {
        return slotRepository.findAllByOrderBySlotNumberAsc().stream()
                .filter(slot -> slot.getSlotNumber() >= 1 && slot.getSlotNumber() <= 4)
                .filter(slot -> slot.isOccupied())
                .count() == 4;
    }

    private boolean isParkingDataTrusted() {
        Instant lastBatch = occupancyService.getLastSuccessfulBatchAt();
        if (lastBatch == null
                || Duration.between(lastBatch, Instant.now()).getSeconds() > staleAfterSeconds) {
            return false;
        }
        List<com.smartparking.backend.model.ParkingSlot> physicalSlots =
                slotRepository.findAllByOrderBySlotNumberAsc().stream()
                .filter(slot -> slot.getSlotNumber() >= 1 && slot.getSlotNumber() <= 4)
                .toList();
        return physicalSlots.size() == 4
                && physicalSlots.stream().allMatch(slot -> slot.getLastSensorUpdate() != null
                        && Duration.between(
                                slot.getLastSensorUpdate().atZone(java.time.ZoneId.systemDefault()).toInstant(),
                                Instant.now())
                                .getSeconds() <= staleAfterSeconds);
    }

    private void syncReportedGateState() {
        if (serialService.isGateStateConfirmed()) {
            updateGateState(GateType.ENTRANCE, serialService.getGateState());
        }
    }

    private void updateGateState(GateType gateType, GateStatus status) {
        Optional<Gate> existing = gateRepository.findByGateType(gateType);
        Gate gate = existing.orElseGet(() -> new Gate(
                gateType == GateType.ENTRANCE ? "Main Entrance Gate" : "Main Exit Gate",
                gateType,
                status));
        if (gate.getStatus() != status) {
            gate.setStatus(status);
            gate.setUpdatedAt(LocalDateTime.now());
            gateRepository.save(gate);
        }
    }

    public GateStatus getEntranceGateState() {
        return serialService.getGateState();
    }

    public GateStatus getExitGateState() {
        return null;
    }

    public GateRepository getGateRepository() {
        return gateRepository;
    }
}
