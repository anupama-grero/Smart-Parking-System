package com.smartparking.backend;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.smartparking.backend.model.ParkingSlot;
import com.smartparking.backend.repository.DriverRepository;
import com.smartparking.backend.repository.ParkingSessionRepository;
import com.smartparking.backend.repository.ParkingSlotRepository;

@SpringBootTest
class SensorOccupancyIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ParkingSlotRepository slotRepository;

    @Autowired
    private ParkingSessionRepository sessionRepository;

    @Autowired
    private DriverRepository driverRepository;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        sessionRepository.deleteAll();
        driverRepository.deleteAll();

        for (int i = 1; i <= 8; i++) {
            final int slotNum = i;
            ParkingSlot slot = slotRepository.findBySlotNumber(slotNum)
                    .orElseGet(() -> slotRepository.save(new ParkingSlot(slotNum, "Category " + slotNum, false)));
            slot.setOccupied(false);
            slot.setLastDistanceCm(null);
            slot.setLastSensorUpdate(null);
            slotRepository.save(slot);
        }
    }

    @Test
    void validReadingMarksAvailable() throws Exception {
        mockMvc.perform(post("/api/sensors/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slotId\":1,\"distance\":120.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotId", is(1)))
                .andExpect(jsonPath("$.occupied", is(false)))
                .andExpect(jsonPath("$.availability", is("AVAILABLE")))
                .andExpect(jsonPath("$.updatedAt", notNullValue()));
    }

    @Test
    void validReadingMarksOccupied() throws Exception {
        mockMvc.perform(post("/api/sensors/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slotId\":1,\"distance\":12.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotId", is(1)))
                .andExpect(jsonPath("$.occupied", is(true)))
                .andExpect(jsonPath("$.availability", is("OCCUPIED")));
    }

    @Test
    void unknownParkingSlotReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/sensors/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slotId\":99,\"distance\":20.0}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("error")));
    }

    @Test
    void negativeDistanceReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/sensors/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slotId\":1,\"distance\":-5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingDistanceReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/sensors/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slotId\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingSlotAndSensorReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/sensors/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"distance\":20.0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/sensors/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("malformed JSON")));
    }

    @Test
    void repeatedReadingUpdatesSameSlot() throws Exception {
        mockMvc.perform(post("/api/sensors/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slotId\":2,\"distance\":10.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occupied", is(true)));

        mockMvc.perform(post("/api/sensors/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slotId\":2,\"distance\":200.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occupied", is(false)));

        ParkingSlot slot = slotRepository.findBySlotNumber(2).orElseThrow();
        org.junit.jupiter.api.Assertions.assertFalse(slot.isOccupied());
        org.junit.jupiter.api.Assertions.assertEquals(200.0, slot.getLastDistanceCm());
    }

    @Test
    void multipleSlotsReceiveIndependentUpdates() throws Exception {
        mockMvc.perform(post("/api/sensors/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slotId\":1,\"distance\":8.0}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/sensors/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sensorId\":5,\"distance\":175.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotId", is(5)))
                .andExpect(jsonPath("$.occupied", is(false)));

        mockMvc.perform(get("/api/parking/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slotId", is(1)))
                .andExpect(jsonPath("$[0].isOccupied", is(true)))
                .andExpect(jsonPath("$[4].slotId", is(5)))
                .andExpect(jsonPath("$[4].isOccupied", is(false)));
    }

    @Test
    void parkingStatusApiStillWorksAfterSensorUpdate() throws Exception {
        mockMvc.perform(get("/api/parking/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slotId", is(1)))
                .andExpect(jsonPath("$[0].isOccupied", is(false)))
                .andExpect(jsonPath("$[7].slotId", is(8)));

        mockMvc.perform(post("/api/sensors/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slotId\":1,\"distance\":15.0}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/parking/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slotId", is(1)))
                .andExpect(jsonPath("$[0].isOccupied", is(true)));
    }

    @Test
    void activeSessionIsNotClearedByEmptySensorReading() throws Exception {
        String entry = "{\"driverId\":\"D201\",\"name\":\"Sensor Test\",\"vehicleNumber\":\"SEN-1\",\"slotNumber\":1}";
        mockMvc.perform(post("/api/parking-sessions/entry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(entry))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/sensors/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slotId\":1,\"distance\":250.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occupied", is(true)))
                .andExpect(jsonPath("$.heldByActiveSession", is(true)));

        mockMvc.perform(get("/api/parking/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isOccupied", is(true)));
    }
}
