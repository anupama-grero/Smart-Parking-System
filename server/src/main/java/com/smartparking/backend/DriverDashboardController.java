package com.smartparking.backend;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

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
}