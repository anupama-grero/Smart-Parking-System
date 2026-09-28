package com.smartparking.backend.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartparking.backend.dto.SensorOccupancyRequest;
import com.smartparking.backend.dto.SensorOccupancyResponse;
import com.smartparking.backend.exception.ResourceNotFoundException;
import com.smartparking.backend.model.ParkingSlot;
import com.smartparking.backend.model.SessionStatus;
import com.smartparking.backend.repository.ParkingSessionRepository;
import com.smartparking.backend.repository.ParkingSlotRepository;

@Service
public class SensorOccupancyService {

    private final ParkingSlotRepository slotRepository;
    private final ParkingSessionRepository sessionRepository;
    private final SensorSlotMapper sensorSlotMapper;
    private final double occupancyThresholdCm;
    private final double maxDistanceCm;

    public SensorOccupancyService(
            ParkingSlotRepository slotRepository,
            ParkingSessionRepository sessionRepository,
            SensorSlotMapper sensorSlotMapper,
            @Value("${parking.sensor.occupancy-threshold-cm:50}") double occupancyThresholdCm,
            @Value("${parking.sensor.max-distance-cm:400}") double maxDistanceCm) {
        this.slotRepository = slotRepository;
        this.sessionRepository = sessionRepository;
        this.sensorSlotMapper = sensorSlotMapper;
        this.occupancyThresholdCm = occupancyThresholdCm;
        this.maxDistanceCm = maxDistanceCm;
    }

    /**
     * Applies one ultrasonic reading to the existing parking-slot row.
     * Repeated readings update the same slot; they do not create extra records or sessions.
     * An ACTIVE parking session keeps the slot occupied even if the sensor briefly reports empty.
     */
    @Transactional
    public SensorOccupancyResponse processReading(SensorOccupancyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Sensor reading payload cannot be null");
        }

        validateDistance(request.getDistance());

        int slotNumber = sensorSlotMapper.resolveSlotNumber(request.getSlotId(), request.getSensorId());

        ParkingSlot slot = slotRepository.findBySlotNumber(slotNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Parking slot not found: " + slotNumber));

        boolean physicallyOccupied = isPhysicallyOccupied(request.getDistance());
        boolean hasActiveSession = sessionRepository
                .findByParkingSlotAndStatus(slot, SessionStatus.ACTIVE)
                .isPresent();

        boolean heldByActiveSession = hasActiveSession && !physicallyOccupied;
        boolean occupied = physicallyOccupied || hasActiveSession;

        LocalDateTime updatedAt = LocalDateTime.now();
        slot.setOccupied(occupied);
        slot.setLastDistanceCm(request.getDistance());
        slot.setLastSensorUpdate(updatedAt);
        slotRepository.save(slot);

        Integer sensorId = request.getSensorId() != null
                ? request.getSensorId()
                : sensorSlotMapper.toSensorId(slotNumber);

        return new SensorOccupancyResponse(
                slot.getSlotNumber(),
                sensorId,
                request.getDistance(),
                occupied,
                occupied ? "OCCUPIED" : "AVAILABLE",
                heldByActiveSession,
                updatedAt
        );
    }

    public boolean isPhysicallyOccupied(double distanceCm) {
        return distanceCm <= occupancyThresholdCm;
    }

    private void validateDistance(Double distance) {
        if (distance == null) {
            throw new IllegalArgumentException("distance is required");
        }
        if (distance < 0) {
            throw new IllegalArgumentException("distance cannot be negative");
        }
        if (distance > maxDistanceCm) {
            throw new IllegalArgumentException(
                    "distance " + distance + " cm exceeds HC-SR04 range of " + maxDistanceCm + " cm");
        }
    }
}
