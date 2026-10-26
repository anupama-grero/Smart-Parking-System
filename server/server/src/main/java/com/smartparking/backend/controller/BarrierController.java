package com.smartparking.backend.controller;

import com.smartparking.backend.dto.GateControlRequest;
import com.smartparking.backend.dto.GateStatusResponse;
import com.smartparking.backend.exception.InvalidGateException;
import com.smartparking.backend.model.GateAction;
import com.smartparking.backend.model.GateStatus;
import com.smartparking.backend.model.GateType;
import com.smartparking.backend.service.BarrierService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

/**
 * BarrierController
 * REST Controller for Security Guard Gate Control & Barrier Override operations.
 * Part of the Security Guard Module (MVC Pattern - Controller Layer).
 */
@RestController
@RequestMapping("/api")
public class BarrierController {

    private final BarrierService barrierService;

    public BarrierController(BarrierService barrierService) {
        this.barrierService = barrierService;
    }

    @GetMapping("/gates/status")
    public ResponseEntity<GateStatusResponse> getGateStatus() {
        return ResponseEntity.ok(barrierService.getGateStatus());
    }

    @PostMapping({"/gates/entry/open", "/gates/entrance/open", "/barrier/entrance/open"})
    public ResponseEntity<GateStatusResponse> openEntranceBarrier(
            @RequestHeader(value = "X-Gate-Control-Token", required = false) String token) {
        authorizeGateControl(token);
        return ResponseEntity.ok(barrierService.openEntranceBarrier());
    }

    @PostMapping({"/gates/entry/close", "/gates/entrance/close", "/barrier/entrance/close"})
    public ResponseEntity<GateStatusResponse> closeEntranceBarrier(
            @RequestHeader(value = "X-Gate-Control-Token", required = false) String token) {
        authorizeGateControl(token);
        return ResponseEntity.ok(barrierService.closeEntranceBarrier());
    }

    @PostMapping({"/gates/exit/open", "/barrier/exit/open"})
    public ResponseEntity<GateStatusResponse> openExitBarrier(
            @RequestHeader(value = "X-Gate-Control-Token", required = false) String token) {
        authorizeGateControl(token);
        return ResponseEntity.ok(barrierService.openExitBarrier());
    }

    @PostMapping({"/gates/exit/close", "/barrier/exit/close"})
    public ResponseEntity<GateStatusResponse> closeExitBarrier(
            @RequestHeader(value = "X-Gate-Control-Token", required = false) String token) {
        authorizeGateControl(token);
        return ResponseEntity.ok(barrierService.closeExitBarrier());
    }

    @PostMapping("/gates/entry/status")
    public ResponseEntity<GateStatusResponse> reportEntryGateStatus(@RequestParam String state) {
        GateStatus gateStatus;
        try {
            gateStatus = GateStatus.valueOf(state.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidGateException("Gate state must be OPEN or CLOSED.");
        }
        return ResponseEntity.ok(barrierService.recordGateStatus(GateType.ENTRANCE, gateStatus));
    }

    @PostMapping("/gates/control")
    public ResponseEntity<GateStatusResponse> controlGate(
            @RequestBody GateControlRequest request,
            @RequestHeader(value = "X-Gate-Control-Token", required = false) String token) {
        authorizeGateControl(token);
        if (request == null || request.getGate() == null || request.getAction() == null) {
            throw new InvalidGateException("Request body must contain 'gate' and 'action' fields.");
        }

        GateType gateType = GateType.fromString(request.getGate());
        GateAction gateAction = GateAction.fromString(request.getAction());

        return ResponseEntity.ok(barrierService.controlBarrier(gateType, gateAction));
    }

    /**
     * Legacy HTTP command-polling endpoints retained for older clients. The
     * active firmware reports gate state directly to the backend over Wi-Fi.
     *
     * The firmware calls these every BACKEND_INTERVAL seconds and opens its
     * servo whenever the response body contains the word {@code OPEN}.
     *
     * GET /api/gate/entry/command  -> "OPEN" | "CLOSED"
     * GET /api/gate/exit/command  -> "OPEN" | "CLOSED"
     *
     * {@code /api/gates/...} aliases are also accepted so both the firmware
     * and any future client can share one namespace.
     */
    @GetMapping(value = {
            "/gate/entry/command",
            "/gates/entry/command",
            "/gates/entrance/command"
    }, produces = "text/plain")
    public String getEntryGateCommand() {
        return barrierService.getEntryGateCommand();
    }

    @GetMapping(value = {
            "/gate/exit/command",
            "/gates/exit/command"
    }, produces = "text/plain")
    public String getExitGateCommand() {
        return barrierService.getExitGateCommand();
    }

    private void authorizeGateControl(String token) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SECURITY_GUARD"))) {
            return;
        }
        if (!barrierService.isGateControlConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Gate control is disabled until ESP32_GATE_CONTROL_TOKEN is configured.");
        }
        if (!barrierService.isGateControlTokenValid(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid gate-control token.");
        }
    }
}
