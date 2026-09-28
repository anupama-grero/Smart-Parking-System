package com.smartparking.backend.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartparking.backend.model.ParkingSlot;
import com.smartparking.backend.service.ParkingSlotService;

@RestController
@RequestMapping("/api/parking")
public class ParkingSlotController {

    private final ParkingSlotService parkingSlotService;

    public ParkingSlotController(ParkingSlotService parkingSlotService) {
        this.parkingSlotService = parkingSlotService;
    }

    /**
     * Real-time parking status API.
     *
     * Used by the Driver Dashboard and polling script.
     *
     * GET /api/parking/status
     */
    @GetMapping("/status")
    public List<Map<String, Object>> getParkingStatus() {

        return parkingSlotService.getAllSlots()
                .stream()
                .map(slot -> Map.<String, Object>of(
                        "slotId", slot.getSlotNumber(),
                        "isOccupied", slot.isOccupied(),
                        "category", slot.getCategory()
                ))
                .collect(Collectors.toList());
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
            @RequestBody ParkingSlot slot) {

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
            @RequestBody ParkingSlot slot) {

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