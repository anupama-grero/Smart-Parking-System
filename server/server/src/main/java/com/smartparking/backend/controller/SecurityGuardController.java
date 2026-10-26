package com.smartparking.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SecurityGuardController
 * Handles view rendering and page navigation for the Security Guard Override Station.
 * Part of the Security Guard Module (MVC Pattern - Controller Layer).
 */
@Controller
public class SecurityGuardController {

    @GetMapping("/security-guard")
    public String securityGuardDashboard() {
        return "security-guard";
    }
}
