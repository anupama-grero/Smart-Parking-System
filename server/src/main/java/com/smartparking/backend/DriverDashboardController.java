package com.smartparking.backend;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.smartparking.backend.model.ParkingSlot;
import com.smartparking.backend.repository.ParkingSlotRepository;

@Controller
public class DriverDashboardController {

    private final ParkingSlotRepository parkingSlotRepository;

    public DriverDashboardController(ParkingSlotRepository parkingSlotRepository) {
        this.parkingSlotRepository = parkingSlotRepository;
    }

    @GetMapping("/driver")
    public String driverDashboard() {
        return "driver-dashboard";
    }

    @GetMapping("/security-guard")
    public String securityGuard() {
        return "security-guard";
    }

    // Real-time polling API for parking slot statuses (dynamically connected to database)
    @GetMapping("/api/parking/status")
    @ResponseBody
    public List<Map<String, Object>> getParkingStatus() {
        List<ParkingSlot> slots = parkingSlotRepository.findAllByOrderBySlotNumberAsc();
        return slots.stream()
                .map(slot -> Map.<String, Object>of(
                        "slotId", slot.getSlotNumber(),
                        "isOccupied", slot.isOccupied(),
                        "category", slot.getCategory()
                ))
                .collect(Collectors.toList());
    }
}