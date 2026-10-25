package com.smartparking.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

    /**
 * Maps HC-SR04 parking sensors to campus slots.
 * The active firmware maps parking sensors 1–4 directly to slots 1–4.
 * Entry/exit sensors (Gate GPIOs) are not occupancy sensors and are rejected here.
 */
@Component
public class SensorSlotMapper {

    private final int minSlotNumber;
    private final int maxSlotNumber;

    public SensorSlotMapper(
            @Value("${parking.sensor.min-slot-number:1}") int minSlotNumber,
            @Value("${parking.sensor.max-slot-number:4}") int maxSlotNumber) {
        this.minSlotNumber = minSlotNumber;
        this.maxSlotNumber = maxSlotNumber;
    }

    /**
     * Resolves the parking slot number from {@code slotId} and/or {@code sensorId}.
     */
    public int resolveSlotNumber(Integer slotId, Integer sensorId) {
        if (slotId == null && sensorId == null) {
            throw new IllegalArgumentException("slotId or sensorId is required");
        }

        if (sensorId != null && !isParkingSensor(sensorId)) {
            throw new IllegalArgumentException(
                    "Unknown parking sensor: " + sensorId + ". Occupancy sensors are "
                            + minSlotNumber + "–" + maxSlotNumber);
        }

        if (slotId != null && sensorId != null && !slotId.equals(sensorId)) {
            throw new IllegalArgumentException(
                    "sensorId " + sensorId + " does not map to slotId " + slotId);
        }

        return slotId != null ? slotId : sensorId;
    }

    public Integer toSensorId(int slotNumber) {
        return isParkingSensor(slotNumber) ? slotNumber : null;
    }

    public boolean isParkingSensor(int id) {
        return id >= minSlotNumber && id <= maxSlotNumber;
    }
}
