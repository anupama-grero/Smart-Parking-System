package com.smartparking.backend.controller;

import java.util.List;
import java.time.Instant;
import java.util.Comparator;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import com.smartparking.backend.dto.BatchOccupancyRequest;
import com.smartparking.backend.dto.ParkingStatusResponse;
import com.smartparking.backend.dto.ReverseWarningRequest;
import com.smartparking.backend.dto.SensorOccupancyResponse;
import com.smartparking.backend.model.ParkingSlot;
import com.smartparking.backend.service.ParkingSlotService;
import com.smartparking.backend.service.SensorOccupancyService;

@RestController
@RequestMapping("/api/parking")
public class ParkingSlotController {

    private final ParkingSlotService parkingSlotService;
    private final SensorOccupancyService sensorOccupancyService;
    private final int staleAfterSeconds;

    public ParkingSlotController(ParkingSlotService parkingSlotService,
                                 SensorOccupancyService sensorOccupancyService,
                                 @Value("${parking.sensor.stale-after-seconds:10}") int staleAfterSeconds) {
        this.parkingSlotService = parkingSlotService;
        this.sensorOccupancyService = sensorOccupancyService;
        this.staleAfterSeconds = staleAfterSeconds;
    }

    /**
     * Batch occupancy payload posted by the ESP32 firmware
     * ({@code sendParkingStatusToBackend()} in the active four-slot sketch)
     * every second.
     *
     * POST /api/sensors/occupancy/batch
     *
    * <p>Each valid reading is applied through the configured occupancy threshold,
     * active-session protection and slot-2 reverse-warning rules as a single
     * reading posted to {@code /api/sensors/occupancy}.
     *
     * <p>A no-echo reading ({@code distance = -1} or {@code null}) preserves
     * that slot's last valid state. Missing, duplicate, and out-of-range slots
     * are rejected rather than silently omitted.
     *
     * <p>Body: {"availableSlots":3,"occupiedSlots":1,"slots":[{"slotNumber":1,"occupied":true,"distanceCm":4.2}, ...]}
     *
     * @return one entry per processed reading, in payload order.
     */
    @PostMapping("/occupancy")
    public ResponseEntity<List<SensorOccupancyResponse>> receiveParkingOccupancy(
            @RequestBody BatchOccupancyRequest request) {
        return ResponseEntity.ok(sensorOccupancyService.processBatchReading(request));
    }

    /**
     * Real-time parking status API.
     *
     * Used by the Driver Dashboard and polling script.
     *
     * GET /api/parking/status
     */
    @GetMapping("/status")
    public ParkingStatusResponse getParkingStatus() {
        Instant lastBatch = sensorOccupancyService.getLastSuccessfulBatchAt();
        List<ParkingStatusResponse.SlotStatus> slots = parkingSlotService.getAllSlots().stream()
                .filter(slot -> slot.getSlotNumber() >= 1 && slot.getSlotNumber() <= 4)
                .map(slot -> {
                    Instant updatedAt = slot.getLastSensorUpdate() == null
                            ? null
                            : slot.getLastSensorUpdate().atZone(java.time.ZoneId.systemDefault()).toInstant();
                    boolean fresh = updatedAt != null
                            && java.time.Duration.between(updatedAt, Instant.now()).getSeconds() <= staleAfterSeconds;
                    return new ParkingStatusResponse.SlotStatus(
                            slot.getSlotNumber(),
                            slot.isOccupied(),
                            slot.getLastSensorUpdate() != null || slot.isOccupied(),
                            slot.getLastDistanceCm(),
                            updatedAt,
                            fresh,
                            slot.isReverseWarning());
                })
                .sorted(Comparator.comparingInt(ParkingStatusResponse.SlotStatus::slotNumber))
                .toList();
        int occupied = (int) slots.stream().filter(ParkingStatusResponse.SlotStatus::occupied).count();
        int available = (int) slots.stream()
                .filter(ParkingStatusResponse.SlotStatus::known)
                .filter(slot -> !slot.occupied())
                .count();
        int unknown = (int) slots.stream().filter(slot -> !slot.known()).count();
        boolean stale = !sensorOccupancyService.isTelemetryConnected(staleAfterSeconds);
        Instant latestUpdate = lastBatch != null
                ? lastBatch
                : slots.stream()
                        .map(ParkingStatusResponse.SlotStatus::lastSensorUpdate)
                        .filter(java.util.Objects::nonNull)
                        .max(Comparator.naturalOrder())
                        .orElse(null);
        return new ParkingStatusResponse(
                slots,
                occupied,
                available,
                unknown,
                slots.size() == 4 && occupied == 4 && unknown == 0,
                !stale,
                latestUpdate,
                stale);
    }

    /**
     * Reverse parking safety status.
     *
     * Reports whether the reverse-manoeuvre warning (slot 2) is active.
     * GET /api/parking/reverse-warning
     */
    @GetMapping("/reverse-warning")
    public java.util.Map<String, Object> getReverseWarningStatus() {
        ParkingSlot slot2 = parkingSlotService.getSlotByNumber(2);
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("slotNumber", 2);
        result.put("reverseWarning", slot2.isReverseWarning());
        result.put("active", slot2.isReverseWarning());
        result.put("distanceCm", slot2.getLastDistanceCm());
        result.put("message", slot2.isReverseWarning()
                ? "Reverse warning active - object detected behind slot 2"
                : "Reverse warning inactive");
        return result;
    }

    /**
     * Reverse parking warning event pushed by the serial bridge
     * (serial_bridge.py) when the firmware reports an object behind the
     * reverse-parking slot.
     *
     * POST /api/parking/reverse-warning
     * Body: {"slotNumber":2,"active":true} or {"slotNumber":2,"reverseWarning":true}
     */
    @PostMapping("/reverse-warning")
    public ResponseEntity<java.util.Map<String, Object>> setReverseWarning(
            @RequestBody ReverseWarningRequest request) {
        if (request == null || request.getSlotNumber() == null) {
            throw new IllegalArgumentException("slotNumber is required");
        }

        boolean active = request.isActive();
        parkingSlotService.setReverseWarning(request.getSlotNumber(), active);

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("slotNumber", request.getSlotNumber());
        result.put("reverseWarning", active);
        result.put("active", active);
        result.put("message", active
                ? "Reverse warning active - object detected behind slot " + request.getSlotNumber()
                : "Reverse warning inactive");
        return ResponseEntity.ok(result);
    }

    // Get all parking slots.
     
    @GetMapping("/slots")
    public List<ParkingSlot> getAllSlots() {
        return parkingSlotService.getAllSlots();
    }

    // Get a parking slot by ID.
    
     
    @GetMapping("/slots/{id}")
    public ParkingSlot getSlotById(@PathVariable Long id) {
        return parkingSlotService.getSlotById(id);
    }

    
    //  Create a parking slot.
    
    @PostMapping("/slots")
    public ResponseEntity<ParkingSlot> createSlot(
            @Valid @RequestBody ParkingSlot slot) {

        ParkingSlot createdSlot =
                parkingSlotService.createSlot(slot);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdSlot);
    }

    
    //   Update a parking slot.
     
    @PutMapping("/slots/{id}")
    public ParkingSlot updateSlot(
            @PathVariable Long id,
            @Valid @RequestBody ParkingSlot slot) {

        return parkingSlotService.updateSlot(id, slot);
    }

    
    //  Delete a parking slot.
     
    @DeleteMapping("/slots/{id}")
    public ResponseEntity<Void> deleteSlot(
            @PathVariable Long id) {

        parkingSlotService.deleteSlot(id);

        return ResponseEntity.noContent().build();
    }

    
     //Update occupancy status.
     
    @PutMapping("/slots/{id}/occupancy")
    public ParkingSlot updateOccupancy(
            @PathVariable Long id,
            @RequestBody boolean occupied) {

        return parkingSlotService.updateOccupancy(id, occupied);
    }
}