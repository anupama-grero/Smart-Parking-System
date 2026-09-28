package com.smartparking.backend;

import com.smartparking.backend.model.Gate;
import com.smartparking.backend.model.GateStatus;
import com.smartparking.backend.model.GateType;
import com.smartparking.backend.repository.GateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class GateIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private GateRepository gateRepository;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Ensure default gates exist and are closed before each test
        Gate entrance = gateRepository.findByGateType(GateType.ENTRANCE)
                .orElseGet(() -> new Gate("Main Entrance Gate", GateType.ENTRANCE, GateStatus.CLOSED));
        entrance.setStatus(GateStatus.CLOSED);
        gateRepository.save(entrance);

        Gate exit = gateRepository.findByGateType(GateType.EXIT)
                .orElseGet(() -> new Gate("Main Exit Gate", GateType.EXIT, GateStatus.CLOSED));
        exit.setStatus(GateStatus.CLOSED);
        gateRepository.save(exit);
    }

    @Test
    void testGateDatabasePersistenceOnEntryOpenAndClose() throws Exception {
        // 1. Initial State from DB
        Gate initialGate = gateRepository.findByGateType(GateType.ENTRANCE).orElseThrow();
        assertEquals(GateStatus.CLOSED, initialGate.getStatus());

        // 2. Open Entrance via API
        mockMvc.perform(post("/api/gates/entry/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryGate").value("OPEN"));

        // Verify Database has been updated
        Gate openedGate = gateRepository.findByGateType(GateType.ENTRANCE).orElseThrow();
        assertEquals(GateStatus.OPEN, openedGate.getStatus());
        assertNotNull(openedGate.getUpdatedAt());

        // 3. Close Entrance via API
        mockMvc.perform(post("/api/gates/entry/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryGate").value("CLOSED"));

        // Verify Database has been updated to CLOSED
        Gate closedGate = gateRepository.findByGateType(GateType.ENTRANCE).orElseThrow();
        assertEquals(GateStatus.CLOSED, closedGate.getStatus());
    }

    @Test
    void testGateDatabasePersistenceOnExitOpenAndClose() throws Exception {
        // 1. Initial State from DB
        Gate initialGate = gateRepository.findByGateType(GateType.EXIT).orElseThrow();
        assertEquals(GateStatus.CLOSED, initialGate.getStatus());

        // 2. Open Exit via API
        mockMvc.perform(post("/api/gates/exit/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitGate").value("OPEN"));

        // Verify Database
        Gate openedGate = gateRepository.findByGateType(GateType.EXIT).orElseThrow();
        assertEquals(GateStatus.OPEN, openedGate.getStatus());

        // 3. Close Exit via API
        mockMvc.perform(post("/api/gates/exit/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitGate").value("CLOSED"));

        // Verify Database
        Gate closedGate = gateRepository.findByGateType(GateType.EXIT).orElseThrow();
        assertEquals(GateStatus.CLOSED, closedGate.getStatus());
    }

    @Test
    void testGateStatusEndpointFetchesFromDatabase() throws Exception {
        // Manually update DB status
        Gate entrance = gateRepository.findByGateType(GateType.ENTRANCE).orElseThrow();
        entrance.setStatus(GateStatus.OPEN);
        gateRepository.save(entrance);

        mockMvc.perform(get("/api/gates/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryGate").value("OPEN"))
                .andExpect(jsonPath("$.exitGate").value("CLOSED"));
    }

    @Test
    void testControlEndpointWithJsonPayloadUpdatesDatabase() throws Exception {
        String jsonPayload = "{\"gate\":\"ENTRANCE\",\"action\":\"OPEN\"}";

        mockMvc.perform(post("/api/gates/control")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryGate").value("OPEN"));

        Gate gate = gateRepository.findByGateType(GateType.ENTRANCE).orElseThrow();
        assertEquals(GateStatus.OPEN, gate.getStatus());
    }
}
