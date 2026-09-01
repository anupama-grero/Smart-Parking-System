package com.smartparking.backend.controller;

import com.smartparking.backend.dto.GateControlRequest;
import com.smartparking.backend.dto.GateStatusResponse;
import com.smartparking.backend.exception.InvalidGateException;
import com.smartparking.backend.model.GateAction;
import com.smartparking.backend.model.GateType;
import com.smartparking.backend.service.BarrierService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<GateStatusResponse> openEntranceBarrier() {
        return ResponseEntity.ok(barrierService.openEntranceBarrier());
    }

    @PostMapping({"/gates/entry/close", "/gates/entrance/close", "/barrier/entrance/close"})
    public ResponseEntity<GateStatusResponse> closeEntranceBarrier() {
        return ResponseEntity.ok(barrierService.closeEntranceBarrier());
    }

    @PostMapping({"/gates/exit/open", "/barrier/exit/open"})
    public ResponseEntity<GateStatusResponse> openExitBarrier() {
        return ResponseEntity.ok(barrierService.openExitBarrier());
    }

    @PostMapping({"/gates/exit/close", "/barrier/exit/close"})
    public ResponseEntity<GateStatusResponse> closeExitBarrier() {
        return ResponseEntity.ok(barrierService.closeExitBarrier());
    }

    @PostMapping("/gates/control")
    public ResponseEntity<GateStatusResponse> controlGate(@RequestBody GateControlRequest request) {
        if (request == null || request.getGate() == null || request.getAction() == null) {
            throw new InvalidGateException("Request body must contain 'gate' and 'action' fields.");
        }

        GateType gateType = GateType.fromString(request.getGate());
        GateAction gateAction = GateAction.fromString(request.getAction());

        return ResponseEntity.ok(barrierService.controlBarrier(gateType, gateAction));
    }
}
