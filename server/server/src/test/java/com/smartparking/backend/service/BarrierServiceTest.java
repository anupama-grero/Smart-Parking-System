package com.smartparking.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.smartparking.backend.dto.GateStatusResponse;
import com.smartparking.backend.exception.Esp32UnavailableException;
import com.smartparking.backend.exception.InvalidGateException;
import com.smartparking.backend.model.GateAction;
import com.smartparking.backend.model.GateStatus;
import com.smartparking.backend.model.GateType;
import com.smartparking.backend.model.ParkingSlot;
import com.smartparking.backend.repository.GateRepository;
import com.smartparking.backend.repository.ParkingSlotRepository;

class BarrierServiceTest {

    private GateRepository gateRepository;
    private ParkingSlotRepository slotRepository;
    private Esp32SerialService serialService;
    private SensorOccupancyService occupancyService;
    private BarrierService barrierService;

    @BeforeEach
    void setUp() {
        gateRepository = Mockito.mock(GateRepository.class);
        slotRepository = Mockito.mock(ParkingSlotRepository.class);
        serialService = Mockito.mock(Esp32SerialService.class);
        occupancyService = Mockito.mock(SensorOccupancyService.class);
        barrierService = new BarrierService(
                gateRepository, slotRepository, serialService, occupancyService, "test-token", 10);

        when(serialService.isConnected()).thenReturn(true);
        when(serialService.isConfigured()).thenReturn(true);
        when(serialService.isGateStateConfirmed()).thenReturn(false);
        when(serialService.getCommandState()).thenReturn("IDLE");
        when(occupancyService.getLastSuccessfulBatchAt()).thenReturn(Instant.now());
        when(slotRepository.findAllByOrderBySlotNumberAsc()).thenReturn(availableSlots());
    }

    @Test
    void statusDoesNotPretendDisconnectedGateIsClosed() {
        when(serialService.isConnected()).thenReturn(false);

        GateStatusResponse response = barrierService.getGateStatus();

        assertEquals("UNKNOWN", response.getEntryGate());
        assertEquals("UNSUPPORTED", response.getExitGate());
        assertFalse(response.isHardwareConnected());
    }

    @Test
    void requestRemainsPendingUntilSerialConfirmsIt() {
        when(serialService.getCommandState()).thenReturn("PENDING");
        when(serialService.getCommandMessage()).thenReturn("OPEN command sent; waiting for firmware status.");
        barrierService.controlBarrier(GateType.ENTRANCE, GateAction.OPEN);

        verify(serialService).requestCommand(GateType.ENTRANCE, GateAction.OPEN);
        assertEquals("UNKNOWN", barrierService.getGateStatus().getEntryGate());
    }

    @Test
    void entryOpenIsRejectedWhenAllFourSlotsAreOccupied() {
        when(slotRepository.findAllByOrderBySlotNumberAsc()).thenReturn(occupiedSlots());

        assertThrows(IllegalStateException.class,
                () -> barrierService.controlBarrier(GateType.ENTRANCE, GateAction.OPEN));
        verify(serialService, never()).requestCommand(any(), any());
    }

    @Test
    void entryOpenIsRejectedWhenSensorTelemetryIsStale() {
        when(occupancyService.getLastSuccessfulBatchAt()).thenReturn(Instant.now().minusSeconds(30));

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> barrierService.controlBarrier(GateType.ENTRANCE, GateAction.OPEN));
        verify(serialService, never()).requestCommand(any(), any());
    }

    @Test
    void separateExitCommandIsNotInventedForSingleGateFirmware() {
        assertThrows(InvalidGateException.class,
                () -> barrierService.controlBarrier(GateType.EXIT, GateAction.OPEN));
        verify(serialService, never()).requestCommand(any(), any());
    }

    @Test
    void disconnectedSerialCommandFailsExplicitly() {
        when(serialService.isConnected()).thenReturn(false);
        doThrow(new Esp32UnavailableException("serial disconnected"))
                .when(serialService).requestCommand(GateType.ENTRANCE, GateAction.CLOSE);

        assertThrows(Esp32UnavailableException.class,
                () -> barrierService.controlBarrier(GateType.ENTRANCE, GateAction.CLOSE));
    }

    @Test
    void sharedSecretMustBeConfiguredAndMatch() {
        assertFalse(barrierService.isGateControlTokenValid(null));
        assertFalse(barrierService.isGateControlTokenValid("wrong"));
        org.junit.jupiter.api.Assertions.assertTrue(barrierService.isGateControlTokenValid("test-token"));
    }

    private List<ParkingSlot> availableSlots() {
        return List.of(
                slot(1, false), slot(2, false), slot(3, false), slot(4, false));
    }

    private List<ParkingSlot> occupiedSlots() {
        return List.of(
                slot(1, true), slot(2, true), slot(3, true), slot(4, true));
    }

    private ParkingSlot slot(int number, boolean occupied) {
        ParkingSlot slot = new ParkingSlot(number, occupied);
        slot.setLastSensorUpdate(LocalDateTime.now());
        slot.setLastDistanceCm(occupied ? 4.0 : 100.0);
        return slot;
    }
}
