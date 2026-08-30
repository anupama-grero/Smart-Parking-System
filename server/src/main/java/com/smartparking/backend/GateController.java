package com.smartparking.backend;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gates")
public class GateController {

    private volatile String entryGate = "CLOSED";
    private volatile String exitGate = "CLOSED";

    @GetMapping("/status")
    public Map<String, String> getGateStatus() {
        return Map.of(
            "entryGate", entryGate,
            "exitGate", exitGate
        );
    }

    @PostMapping("/entry/open")
    public Map<String, String> openEntryGate() {
        entryGate = "OPEN";
        return Map.of("message", "Entry barrier opened successfully!");
    }

    @PostMapping("/exit/open")
    public Map<String, String> openExitGate() {
        exitGate = "OPEN";
        return Map.of("message", "Exit barrier opened successfully!");
    }
}
