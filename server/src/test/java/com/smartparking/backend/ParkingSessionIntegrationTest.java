package com.smartparking.backend;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;
import com.smartparking.backend.model.ParkingSlot;
import com.smartparking.backend.repository.DriverRepository;
import com.smartparking.backend.repository.ParkingSessionRepository;
import com.smartparking.backend.repository.ParkingSlotRepository;

@SpringBootTest
public class ParkingSessionIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ParkingSessionRepository sessionRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private ParkingSlotRepository slotRepository;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        sessionRepository.deleteAll();
        driverRepository.deleteAll();

        // Ensure standard slots 1-8 exist and are available
        for (int i = 1; i <= 8; i++) {
            final int slotNum = i;
            ParkingSlot slot = slotRepository.findBySlotNumber(slotNum)
                    .orElseGet(() -> slotRepository.save(new ParkingSlot(slotNum, "Category " + slotNum, false)));
            slot.setOccupied(false);
            slotRepository.save(slot);
        }
    }

    private String buildJsonRequest(String driverId, String name, String vehicleNumber, Integer slotNumber) {
        return String.format(
                "{\"driverId\":\"%s\",\"name\":\"%s\",\"vehicleNumber\":\"%s\",\"slotNumber\":%d}",
                driverId != null ? driverId : "",
                name != null ? name : "",
                vehicleNumber != null ? vehicleNumber : "",
                slotNumber != null ? slotNumber : 0
        );
    }

    // 1. Create valid parking session
    @Test
    public void testCreateValidParkingSession() throws Exception {
        String jsonPayload = buildJsonRequest("D101", "John Doe", "CAR-1234", 1);

        mockMvc.perform(post("/api/parking-sessions/entry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.driver.driverId", is("D101")))
                .andExpect(jsonPath("$.assignedSlot", is(1)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        // Verify slot status in database is occupied
        mockMvc.perform(get("/api/parking/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slotId", is(1)))
                .andExpect(jsonPath("$[0].isOccupied", is(true)));
    }

    // 2. Create session with invalid driver (missing driverId)
    @Test
    public void testCreateSessionInvalidDriver() throws Exception {
        String jsonPayload = buildJsonRequest("", "John Doe", "CAR-1234", 1);

        mockMvc.perform(post("/api/parking-sessions/entry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isBadRequest());
    }

    // 3. Create session with invalid slot number
    @Test
    public void testCreateSessionInvalidSlot() throws Exception {
        String jsonPayload = buildJsonRequest("D102", "Jane Doe", "CAR-5678", 999);

        mockMvc.perform(post("/api/parking-sessions/entry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isNotFound());
    }

    // 4. Create session for occupied slot
    @Test
    public void testCreateSessionForOccupiedSlot() throws Exception {
        String req1 = buildJsonRequest("D101", "John Doe", "CAR-1234", 1);
        mockMvc.perform(post("/api/parking-sessions/entry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req1))
                .andExpect(status().isCreated());

        String req2 = buildJsonRequest("D102", "Jane Doe", "CAR-5678", 1);
        mockMvc.perform(post("/api/parking-sessions/entry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req2))
                .andExpect(status().isConflict());
    }

    // 5. Create duplicate active session for same driver
    @Test
    public void testCreateDuplicateActiveSessionForSameDriver() throws Exception {
        String req1 = buildJsonRequest("D101", "John Doe", "CAR-1234", 1);
        mockMvc.perform(post("/api/parking-sessions/entry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req1))
                .andExpect(status().isCreated());

        String req2 = buildJsonRequest("D101", "John Doe", "CAR-1234", 2);
        mockMvc.perform(post("/api/parking-sessions/entry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req2))
                .andExpect(status().isConflict());
    }

    // 6. Retrieve session by ID
    @Test
    public void testGetSessionById() throws Exception {
        String req = buildJsonRequest("D101", "John Doe", "CAR-1234", 1);
        String responseStr = mockMvc.perform(post("/api/parking-sessions/entry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer sessionId = JsonPath.read(responseStr, "$.id");

        mockMvc.perform(get("/api/parking-sessions/" + sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(sessionId)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    // 7. Retrieve active sessions list
    @Test
    public void testGetActiveSessions() throws Exception {
        String req = buildJsonRequest("D101", "John Doe", "CAR-1234", 1);
        mockMvc.perform(post("/api/parking-sessions/entry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/parking-sessions/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].driver.driverId", is("D101")));
    }

    // 8. Complete session & verify slot becomes available
    @Test
    public void testCompleteSessionAndReleaseSlot() throws Exception {
        String req = buildJsonRequest("D101", "John Doe", "CAR-1234", 1);
        String responseStr = mockMvc.perform(post("/api/parking-sessions/entry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer sessionId = JsonPath.read(responseStr, "$.id");

        // Exit flow
        mockMvc.perform(put("/api/parking-sessions/exit/" + sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.exitTime", notNullValue()));

        // Verify slot is now AVAILABLE in real-time polling API
        mockMvc.perform(get("/api/parking/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slotId", is(1)))
                .andExpect(jsonPath("$[0].isOccupied", is(false)));
    }

    // 9. Attempt to complete session twice
    @Test
    public void testCompleteSessionTwiceFails() throws Exception {
        String req = buildJsonRequest("D101", "John Doe", "CAR-1234", 1);
        String responseStr = mockMvc.perform(post("/api/parking-sessions/entry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer sessionId = JsonPath.read(responseStr, "$.id");

        // First exit succeeds
        mockMvc.perform(put("/api/parking-sessions/exit/" + sessionId))
                .andExpect(status().isOk());

        // Second exit fails with 400 Bad Request
        mockMvc.perform(put("/api/parking-sessions/exit/" + sessionId))
                .andExpect(status().isBadRequest());
    }

    // 10. Verify history includes completed session
    @Test
    public void testGetParkingHistory() throws Exception {
        String req = buildJsonRequest("D101", "John Doe", "CAR-1234", 1);
        String responseStr = mockMvc.perform(post("/api/parking-sessions/entry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer sessionId = JsonPath.read(responseStr, "$.id");
        mockMvc.perform(put("/api/parking-sessions/exit/" + sessionId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/parking-sessions/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("COMPLETED")));
    }
}
