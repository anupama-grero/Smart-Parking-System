package com.smartparking.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartparking.backend.exception.ResourceNotFoundException;
import com.smartparking.backend.model.ParkingSlot;
import com.smartparking.backend.repository.ParkingSlotRepository;

@Service
public class ParkingSlotService {

    private final ParkingSlotRepository parkingSlotRepository;

    public ParkingSlotService(ParkingSlotRepository parkingSlotRepository) {
        this.parkingSlotRepository = parkingSlotRepository;
    }

    /**
     * Retrieves all parking slots ordered by slot number.
     */
    @Transactional(readOnly = true)
    public List<ParkingSlot> getAllSlots() {
        return parkingSlotRepository.findAllByOrderBySlotNumberAsc().stream()
                .filter(slot -> slot.getSlotNumber() >= 1 && slot.getSlotNumber() <= 4)
                .toList();
    }

    /**
     * Retrieves the number of available (unoccupied) parking slots.
     */
    @Transactional(readOnly = true)
    public long countAvailableSlots() {
        return getAllSlots().stream().filter(slot -> !slot.isOccupied()).count();
    }

    /**
     * Retrieves the number of occupied parking slots.
     */
    @Transactional(readOnly = true)
    public long countOccupiedSlots() {
        return getAllSlots().stream().filter(ParkingSlot::isOccupied).count();
    }

    /**
     * Retrieves all available (unoccupied) parking slots.
     */
    @Transactional(readOnly = true)
    public List<ParkingSlot> getAvailableSlots() {
        return getAllSlots().stream().filter(slot -> !slot.isOccupied()).toList();
    }

    /**
     * Retrieves a parking slot by its database ID.
     */
    @Transactional(readOnly = true)
    public ParkingSlot getSlotById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Parking slot ID cannot be null");
        }

        return parkingSlotRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Parking slot not found: " + id));
    }

    /**
     * Retrieves a parking slot by its slot number.
     */
    @Transactional(readOnly = true)
    public ParkingSlot getSlotByNumber(Integer slotNumber) {
        if (slotNumber == null) {
            throw new IllegalArgumentException("Parking slot number cannot be null");
        }

        return parkingSlotRepository.findBySlotNumber(slotNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Parking slot not found: " + slotNumber));
    }

    /**
     * Creates a new parking slot.
     */
    @Transactional
    public ParkingSlot createSlot(ParkingSlot slot) {
        if (slot == null) {
            throw new IllegalArgumentException("Parking slot cannot be null");
        }

        if (slot.getSlotNumber() == null) {
            throw new IllegalArgumentException("Slot number is required");
        }

        if (slot.getCategory() == null || slot.getCategory().isBlank()) {
            throw new IllegalArgumentException("Category is required");
        }

        validatePhysicalSlotNumber(slot.getSlotNumber());
        if (parkingSlotRepository.findBySlotNumber(slot.getSlotNumber()).isPresent()) {
            throw new IllegalStateException(
                    "Parking slot already exists: " + slot.getSlotNumber());
        }

        return parkingSlotRepository.save(slot);
    }

    /**
     * Updates the basic information of an existing parking slot.
     */
    @Transactional
    public ParkingSlot updateSlot(Long id, ParkingSlot updatedSlot) {
        if (id == null) {
            throw new IllegalArgumentException("Parking slot ID cannot be null");
        }

        if (updatedSlot == null) {
            throw new IllegalArgumentException("Parking slot data cannot be null");
        }

        ParkingSlot existingSlot = parkingSlotRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Parking slot not found: " + id));

        if (updatedSlot.getSlotNumber() == null) {
            throw new IllegalArgumentException("Slot number is required");
        }
        validatePhysicalSlotNumber(updatedSlot.getSlotNumber());

        if (updatedSlot.getCategory() == null || updatedSlot.getCategory().isBlank()) {
            throw new IllegalArgumentException("Category is required");
        }

        // Check whether another slot already uses the new slot number.
        parkingSlotRepository.findBySlotNumber(updatedSlot.getSlotNumber())
                .ifPresent(slot -> {
                    if (!slot.getId().equals(id)) {
                        throw new IllegalStateException(
                                "Parking slot already exists: "
                                        + updatedSlot.getSlotNumber());
                    }
                });

        existingSlot.setSlotNumber(updatedSlot.getSlotNumber());
        existingSlot.setCategory(updatedSlot.getCategory());

        return parkingSlotRepository.save(existingSlot);
    }

    /**
     * Deletes an existing parking slot.
     */
    @Transactional
    public void deleteSlot(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Parking slot ID cannot be null");
        }

        if (!parkingSlotRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Parking slot not found: " + id);
        }

        parkingSlotRepository.deleteById(id);
    }

    /**
     * Updates the occupancy status of a parking slot.
     */
    @Transactional
    public ParkingSlot updateOccupancy(Long id, boolean occupied) {
        ParkingSlot slot = getSlotById(id);

        slot.setOccupied(occupied);

        return parkingSlotRepository.save(slot);
    }

    /**
     * Sets the reverse-manoeuvre warning flag for a slot.
     *
     * Driven by the serial bridge when the firmware reports an object behind
     * the reverse-parking slot (slot 2).
     */
    @Transactional
    public void setReverseWarning(Integer slotNumber, boolean active) {
        ParkingSlot slot = getSlotByNumber(slotNumber);
        slot.setReverseWarning(active);
        parkingSlotRepository.save(slot);
    }

    private void validatePhysicalSlotNumber(Integer slotNumber) {
        if (slotNumber < 1 || slotNumber > 4) {
            throw new IllegalArgumentException("Physical parking slot number must be between 1 and 4");
        }
    }
}