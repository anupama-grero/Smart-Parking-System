package com.smartparking.backend.service;

import java.time.LocalDateTime;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartparking.backend.dto.BatchOccupancyRequest;
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
    private final double reverseWarningThresholdCm;
    private final double maxDistanceCm;
    private final AtomicReference<Instant> lastSuccessfulBatchAt = new AtomicReference<>();

    public SensorOccupancyService(
            ParkingSlotRepository slotRepository,
            ParkingSessionRepository sessionRepository,
            SensorSlotMapper sensorSlotMapper,
            @Value("${parking.sensor.occupancy-threshold-cm:5}") double occupancyThresholdCm,
            @Value("${parking.sensor.reverse-warning-threshold-cm:3}") double reverseWarningThresholdCm,
            @Value("${parking.sensor.max-distance-cm:400}") double maxDistanceCm) {
        this.slotRepository = slotRepository;
        this.sessionRepository = sessionRepository;
        this.sensorSlotMapper = sensorSlotMapper;
        this.occupancyThresholdCm = occupancyThresholdCm;
        this.reverseWarningThresholdCm = reverseWarningThresholdCm;
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

        Integer sensorId = request.getSensorId() != null
                ? request.getSensorId()
                : sensorSlotMapper.toSensorId(slotNumber);

        // run the shared slot-update logic
        SensorOccupancyResponse response = applyReading(slotNumber, request.getDistance());
        response.setSensorId(sensorId);
        return response;
    }

    /**
     * Applies every slot reading inside one batch payload from the active
     * four-slot ESP32 firmware ({@code POST /api/sensors/occupancy/batch}).
     *
    * Each valid reading goes through exactly the same rules as
    * {@link #processReading(SensorOccupancyRequest)} — same configured threshold,
     * same active-session protection, same reverse-warning handling for slot 2.
     *
     * A no-echo reading is skipped so one faulty sensor cannot erase a
     * previously valid state. Structural or out-of-range batch errors reject
     * the whole payload:
     * <ul>
     *   <li>{@code distance == null} or {@code distance == -1} — the firmware
     *       reports -1 when an HC-SR04 returns no echo;</li>
     *   <li>a missing, duplicate, or out-of-range slot number;</li>
     *   <li>a slot number that has no row in the database.</li>
     * </ul>
     *
     * @return one response per processed reading, in payload order.
     */
    @Transactional
    public List<SensorOccupancyResponse> processBatchReading(BatchOccupancyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Batch occupancy payload cannot be null");
        }

        if (request.getSlots() == null || request.getSlots().size() != 4) {
            throw new IllegalArgumentException("Batch must contain readings for all four parking slots");
        }
        if (request.getAvailableSlots() != null || request.getOccupiedSlots() != null) {
            Integer available = request.getAvailableSlots();
            Integer occupied = request.getOccupiedSlots();
            if (available == null || occupied == null
                    || available < 0 || available > 4
                    || occupied < 0 || occupied > 4
                    || available + occupied != 4) {
                throw new IllegalArgumentException("availableSlots and occupiedSlots must be valid counts totalling four");
            }
        }

        Set<Integer> seenSlots = new HashSet<>();
        for (BatchOccupancyRequest.SlotReading reading : request.getSlots()) {
            if (reading == null || reading.getSlot() == null
                    || reading.getSlot() < 1 || reading.getSlot() > 4) {
                throw new IllegalArgumentException("Each reading must identify a parking slot from 1 to 4");
            }
            if (!seenSlots.add(reading.getSlot())) {
                throw new IllegalArgumentException("Duplicate reading for parking slot " + reading.getSlot());
            }
            Double distance = reading.getDistance();
            if (distance != null
                    && (!Double.isFinite(distance) || distance < -1 || distance > maxDistanceCm)) {
                throw new IllegalArgumentException(
                        "Slot " + reading.getSlot() + " distance must be -1 (no echo) or between 0 and "
                                + maxDistanceCm + " cm");
            }
        }

        if (seenSlots.size() != 4) {
            throw new IllegalArgumentException("Batch must contain exactly one reading for each slot from 1 to 4");
        }

        List<SensorOccupancyResponse> results = new ArrayList<>(4);
        for (BatchOccupancyRequest.SlotReading reading : request.getSlots()) {
            Double distanceCm = reading.getDistance();
            if (distanceCm == null || distanceCm == -1) {
                continue;
            }
            results.add(applyReading(reading.getSlot(), distanceCm));
        }

        lastSuccessfulBatchAt.set(Instant.now());
        return results;
    }

    public Instant getLastSuccessfulBatchAt() {
        return lastSuccessfulBatchAt.get();
    }

    public boolean isTelemetryConnected(int staleAfterSeconds) {
        Instant latest = lastSuccessfulBatchAt.get();
        return latest != null && Duration.between(latest, Instant.now()).getSeconds() <= staleAfterSeconds;
    }

    /**
     * Shared slot-update logic used by both the single-reading and batch paths.
     */
    private SensorOccupancyResponse applyReading(Integer slotNumber, Double distanceCm) {
        if (distanceCm == null) {
            throw new IllegalArgumentException("distance is required");
        }
        if (distanceCm < 0) {
            throw new IllegalArgumentException("distance cannot be negative");
        }
        if (distanceCm > maxDistanceCm) {
            throw new IllegalArgumentException(
                    "distance " + distanceCm + " cm exceeds HC-SR04 range of " + maxDistanceCm + " cm");
        }

        ParkingSlot slot = slotRepository.findBySlotNumber(slotNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Parking slot not found: " + slotNumber));

        boolean physicallyOccupied = isPhysicallyOccupied(distanceCm);
        boolean hasActiveSession = sessionRepository
                .findByParkingSlotAndStatus(slot, SessionStatus.ACTIVE)
                .isPresent();

        boolean heldByActiveSession = hasActiveSession && !physicallyOccupied;
        boolean occupied = physicallyOccupied || hasActiveSession;

        // Reverse parking safety: slot 2 is the designated reverse-manoeuvre
        // space. An object detected within the threshold there triggers the
        // audible/visual reverse warning (buzzer + red LED) independently of
        // the slot's occupancy state.
        boolean reverseWarning = slotNumber == 2 && distanceCm <= reverseWarningThresholdCm;

        LocalDateTime updatedAt = LocalDateTime.now();
        slot.setOccupied(occupied);
        slot.setLastDistanceCm(distanceCm);
        slot.setLastSensorUpdate(updatedAt);
        slot.setReverseWarning(reverseWarning);
        slotRepository.save(slot);

        return new SensorOccupancyResponse(
                slot.getSlotNumber(),
                sensorSlotMapper.toSensorId(slotNumber),
                distanceCm,
                occupied,
                occupied ? "OCCUPIED" : "AVAILABLE",
                heldByActiveSession,
                reverseWarning,
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
        if (!Double.isFinite(distance)) {
            throw new IllegalArgumentException("distance must be a finite number");
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
