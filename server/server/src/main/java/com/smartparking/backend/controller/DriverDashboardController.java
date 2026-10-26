package com.smartparking.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DriverDashboardController {

    @GetMapping("/driver")
    public String driverDashboard() {
        return "driver-dashboard";
    }

    @GetMapping("/favicon.ico")
    public String favicon() {
        return "redirect:/favicon.svg";
    }
}