package com.smartparking.backend.dto;

import java.time.Instant;
import java.util.List;

public record ParkingStatusResponse(
        List<SlotStatus> slots,
        int occupiedSlots,
        int availableSlots,
        int unknownSlots,
        boolean full,
        boolean hardwareConnected,
        Instant lastSuccessfulUpdate,
        boolean stale) {

    public record SlotStatus(
            int slotNumber,
            boolean occupied,
            boolean known,
            Double distanceCm,
            Instant lastSensorUpdate,
            boolean sensorFresh,
            boolean reverseWarning) {
    }
}
