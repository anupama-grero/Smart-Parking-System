package com.smartparking.backend;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.smartparking.backend.model.Gate;
import com.smartparking.backend.model.GateStatus;
import com.smartparking.backend.model.GateType;
import com.smartparking.backend.model.ParkingSlot;
import com.smartparking.backend.repository.GateRepository;
import com.smartparking.backend.repository.ParkingSlotRepository;

@SpringBootTest
@TestPropertySource(properties = {
        "esp32.gate-control-token=test-token",
        "esp32.wifi.base-url="
})
class GateIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private GateRepository gateRepository;

    @Autowired
    private ParkingSlotRepository slotRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        for (int number = 1; number <= 4; number++) {
            ParkingSlot slot = slotRepository.findBySlotNumber(number).orElseThrow();
            slot.setOccupied(false);
            slot.setLastDistanceCm(null);
            slot.setLastSensorUpdate(null);
            slotRepository.save(slot);
        }
        gateRepository.findByGateType(GateType.ENTRANCE).ifPresent(gate -> {
            gate.setStatus(GateStatus.CLOSED);
            gateRepository.save(gate);
        });
    }

    @Test
    void gateStatusReportsDisconnectedHardwareAndUnknownPhysicalState() throws Exception {
        mockMvc.perform(get("/api/gates/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryGate", is("UNKNOWN")))
                .andExpect(jsonPath("$.exitGate", is("UNSUPPORTED")))
                .andExpect(jsonPath("$.hardwareConnected", is(false)))
                .andExpect(jsonPath("$.gateStateConfirmed", is(false)));
    }

    @Test
    void unauthorizedGateRequestIsRejected() throws Exception {
        mockMvc.perform(post("/api/gates/entry/open"))
                .andExpect(status().isForbidden());
    }

    @Test
    void failedHardwareCommandIsNotReportedAsPhysicalSuccess() throws Exception {
        sendValidFourSlotBatch();

        mockMvc.perform(post("/api/gates/entry/open")
                        .header("X-Gate-Control-Token", "test-token"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status", is("error")));

        mockMvc.perform(get("/api/gates/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryGate", is("UNKNOWN")))
                .andExpect(jsonPath("$.gateStateConfirmed", is(false)));
    }

    @Test
    void entryRequestIsRejectedWhenAllFourSlotsAreOccupied() throws Exception {
        for (int number = 1; number <= 4; number++) {
            ParkingSlot slot = slotRepository.findBySlotNumber(number).orElseThrow();
            slot.setOccupied(true);
            slotRepository.save(slot);
        }

        mockMvc.perform(post("/api/gates/entry/open")
                        .header("X-Gate-Control-Token", "test-token"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("all four")));
    }

    @Test
    void legacyCommandPollingNeverTriggersACommand() throws Exception {
        mockMvc.perform(get("/api/gate/entry/command"))
                .andExpect(status().isOk())
                .andExpect(content().string("CLOSED"));
        mockMvc.perform(get("/api/gate/exit/command"))
                .andExpect(status().isOk())
                .andExpect(content().string("CLOSED"));
    }

    @Test
    void exitAutomaticSensorPathIsNotCapacityBlockedByBackend() throws Exception {
        List<ParkingSlot> slots = slotRepository.findAllByOrderBySlotNumberAsc();
        slots.stream().filter(slot -> slot.getSlotNumber() >= 1 && slot.getSlotNumber() <= 4)
                .forEach(slot -> {
                    slot.setOccupied(true);
                    slotRepository.save(slot);
                });

        // Remote exit commands are unsupported for this single-servo firmware;
        // exit remains driven locally by its unchanged sensor logic.
        mockMvc.perform(get("/api/gates/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("sensor-controlled")));
    }

    private void sendValidFourSlotBatch() throws Exception {
        String payload = "{\"availableSlots\":4,\"occupiedSlots\":0,\"slots\":["
                + "{\"slotNumber\":1,\"occupied\":false,\"distanceCm\":100},"
                + "{\"slotNumber\":2,\"occupied\":false,\"distanceCm\":100},"
                + "{\"slotNumber\":3,\"occupied\":false,\"distanceCm\":100},"
                + "{\"slotNumber\":4,\"occupied\":false,\"distanceCm\":100}]}";
        mockMvc.perform(post("/api/sensors/occupancy/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }
}
