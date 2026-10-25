package com.smartparking.backend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * Batch occupancy payload posted by the ESP32 firmware to
 * {@code POST /api/sensors/occupancy/batch}.
 *
 * The active four-slot firmware reports every parking sensor in one request:
 *
 * <pre>{@code
 * {
 *   "availableSlots": 3,
 *   "occupiedSlots": 1,
 *   "slots": [
 *     {"slotNumber": 1, "occupied": true,  "distanceCm": 4.2},
 *     {"slotNumber": 2, "occupied": false, "distanceCm": 110.0},
 *     {"slotNumber": 3, "occupied": false, "distanceCm": -1},
 *     {"slotNumber": 4, "occupied": false, "distanceCm": 95.0}
 *   ]
 * }
 * }</pre>
 *
 * A slot whose sensor returned no echo reports {@code distanceCm = -1}; that
 * slot's previous state is retained so one faulty sensor cannot clear valid
 * occupancy data.
 *
 * {@code availableSlots} / {@code occupiedSlots} are informational only —
 * the backend recomputes occupancy from the per-slot distances and its configured
 * occupancy threshold.
 */
public class BatchOccupancyRequest {

    @JsonAlias("available_slots")
    private Integer availableSlots;

    @JsonAlias("occupied_slots")
    private Integer occupiedSlots;

    @JsonAlias("readings")
    private List<SlotReading> slots;

    public BatchOccupancyRequest() {
    }

    public BatchOccupancyRequest(Integer availableSlots, Integer occupiedSlots, List<SlotReading> slots) {
        this.availableSlots = availableSlots;
        this.occupiedSlots = occupiedSlots;
        this.slots = slots;
    }

    public Integer getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(Integer availableSlots) {
        this.availableSlots = availableSlots;
    }

    public Integer getOccupiedSlots() {
        return occupiedSlots;
    }

    public void setOccupiedSlots(Integer occupiedSlots) {
        this.occupiedSlots = occupiedSlots;
    }

    public List<SlotReading> getSlots() {
        return slots;
    }

    public void setSlots(List<SlotReading> slots) {
        this.slots = slots;
    }

    /**
     * One ultrasonic slot reading from the firmware.
     */
    public static class SlotReading {

        /**
         * 1-based campus slot number.
         */
        @JsonAlias({"slotNumber", "slot_number"})
        private Integer slot;

        /**
         * Firmware's own occupancy guess; informational only.
         */
        private Boolean occupied;

        /**
         * Distance in centimetres, or -1 when no echo was received.
         */
        @JsonAlias({"distanceCm", "distance_cm"})
        private Double distance;

        public SlotReading() {
        }

        public SlotReading(Integer slot, Boolean occupied, Double distance) {
            this.slot = slot;
            this.occupied = occupied;
            this.distance = distance;
        }

        public Integer getSlot() {
            return slot;
        }

        public void setSlot(Integer slot) {
            this.slot = slot;
        }

        /**
         * Alias retained for callers using the earlier {@code slot} field.
         */
        public void setSlotNumber(Integer slotNumber) {
            this.slot = slotNumber;
        }

        public Boolean getOccupied() {
            return occupied;
        }

        public void setOccupied(Boolean occupied) {
            this.occupied = occupied;
        }

        public Double getDistance() {
            return distance;
        }

        public void setDistance(Double distance) {
            this.distance = distance;
        }

        /**
         * Alias retained for callers using the earlier {@code distance} field.
         */
        public void setDistanceCm(Double distanceCm) {
            this.distance = distanceCm;
        }
    }
}
