package com.smartparking.backend;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class DriverDashboardController {

    @GetMapping("/driver")
    public String driverDashboard() {
        return "driver-dashboard";
    }

    @GetMapping("/security-guard")
    public String securityGuard() {
        return "security-guard";
    }

    // Real-time polling API for parking slot statuses
    @GetMapping("/api/parking/status")
    @ResponseBody
    public List<Map<String, Object>> getParkingStatus() {
        return List.of(
            Map.of("slotId", 1, "isOccupied", false),
            Map.of("slotId", 2, "isOccupied", true),
            Map.of("slotId", 3, "isOccupied", false),
            Map.of("slotId", 4, "isOccupied", true),
            Map.of("slotId", 5, "isOccupied", false),
            Map.of("slotId", 6, "isOccupied", false),
            Map.of("slotId", 7, "isOccupied", true),
            Map.of("slotId", 8, "isOccupied", false)
        );
    }
}