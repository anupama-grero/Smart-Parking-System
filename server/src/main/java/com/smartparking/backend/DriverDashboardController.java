package com.smartparking.backend;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DriverDashboardController {

    @GetMapping("/driver")
    public String driverDashboard() {
        return "driver-dashboard";
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