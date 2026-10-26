package com.smartparking.backend;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
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

        for (int i = 1; i <= 4; i++) {
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
                .content("{\"slotId\":1,\"distance\":2.5}"))
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
                .content("{\"slotId\":2,\"distance\":4.0}"))
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
                .content("{\"slotId\":1,\"distance\":4.0}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/sensors/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sensorId\":4,\"distance\":175.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotId", is(4)))
                .andExpect(jsonPath("$.occupied", is(false)));

        mockMvc.perform(get("/api/parking/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots", hasSize(4)))
                .andExpect(jsonPath("$.slots[0].slotNumber", is(1)))
                .andExpect(jsonPath("$.slots[0].occupied", is(true)))
                .andExpect(jsonPath("$.slots[3].slotNumber", is(4)))
                .andExpect(jsonPath("$.slots[3].occupied", is(false)));
    }

    @Test
    void parkingStatusApiStillWorksAfterSensorUpdate() throws Exception {
        mockMvc.perform(get("/api/parking/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots", hasSize(4)))
                .andExpect(jsonPath("$.slots[0].slotNumber", is(1)))
                .andExpect(jsonPath("$.slots[0].known", is(false)))
                .andExpect(jsonPath("$.unknownSlots", is(4)));

        mockMvc.perform(post("/api/sensors/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slotId\":1,\"distance\":4.0}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/parking/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots[0].slotNumber", is(1)))
                .andExpect(jsonPath("$.slots[0].occupied", is(true)));
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
                .andExpect(jsonPath("$.slots[0].occupied", is(true)));
    }

    // ---------------- Firmware batch integration ----------------------------

    @Test
    void firmwareBatchPayloadUpdatesEverySlot() throws Exception {
        // exact payload shape built by sendParkingStatusToBackend()
        String batchJson = "{\"availableSlots\":2,\"occupiedSlots\":2,"
                        + "\"slots\":["
                        + "{\"slot\":1,\"occupied\":true,\"distance\":4.5},"
                        + "{\"slot\":2,\"occupied\":true,\"distance\":2.5},"
                        + "{\"slot\":3,\"occupied\":false,\"distance\":120.0},"
                        + "{\"slot\":4,\"occupied\":false,\"distance\":150.0}]}";

        mockMvc.perform(post("/api/parking/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(batchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].slotId", is(1)))
                .andExpect(jsonPath("$[0].occupied", is(true)))
                .andExpect(jsonPath("$[2].slotId", is(3)))
                .andExpect(jsonPath("$[2].occupied", is(false)));

        // slot 2 is the designated reverse-manoeuvre space, so an object
        // detected there must raise the reverse warning
        mockMvc.perform(post("/api/parking/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(batchJson))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/parking/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occupiedSlots", is(2)))
                .andExpect(jsonPath("$.availableSlots", is(2)))
                .andExpect(jsonPath("$.full", is(false)))
                .andExpect(jsonPath("$.slots[0].slotNumber", is(1)))
                .andExpect(jsonPath("$.slots[0].occupied", is(true)))
                .andExpect(jsonPath("$.slots[2].slotNumber", is(3)))
                .andExpect(jsonPath("$.slots[2].occupied", is(false)));

        mockMvc.perform(get("/api/parking/reverse-warning"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(true)));
    }

@Test
    void firmwareBatchSkipsNoEchoSlots() throws Exception {
        // slot 1 report -1 (HC-SR04 returned no echo) while slot 3 has a
        // valid reading: the batch must still be processed and accepted.
        String batchJson = "{\"availableSlots\":3,\"occupiedSlots\":1,"
                + "\"slots\":["
                + "{\"slot\":1,\"occupied\":false,\"distance\":-1},"
                + "{\"slot\":2,\"occupied\":false,\"distance\":200.0},"
                + "{\"slot\":3,\"occupied\":true,\"distance\":4.5},"
                + "{\"slot\":4,\"occupied\":false,\"distance\":180.0}]}";

        mockMvc.perform(post("/api/parking/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(batchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].slotId", is(2)))
                .andExpect(jsonPath("$[1].slotId", is(3)))
                .andExpect(jsonPath("$[1].occupied", is(true)))
                .andExpect(jsonPath("$[2].slotId", is(4)));
    }

    @Test
    void serialCableFirmwareBatchUsesSlotNumberAndDistanceCm() throws Exception {
        // The unchanged active firmware emits "slotNumber" / "distanceCm".
        String batchJson = "{\"availableSlots\":2,\"occupiedSlots\":2,"
                + "\"slots\":["
                + "{\"slotNumber\":1,\"occupied\":true,\"distanceCm\":4.5,\"reverseWarning\":false},"
                + "{\"slotNumber\":2,\"occupied\":true,\"distanceCm\":2.5,\"reverseWarning\":true},"
                + "{\"slotNumber\":3,\"occupied\":false,\"distanceCm\":12.5,\"reverseWarning\":false},"
                + "{\"slotNumber\":4,\"occupied\":false,\"distanceCm\":null,\"reverseWarning\":false}]}";

        mockMvc.perform(post("/api/parking/occupancy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(batchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].slotId", is(1)))
                .andExpect(jsonPath("$[0].occupied", is(true)))
                .andExpect(jsonPath("$[1].slotId", is(2)))
                .andExpect(jsonPath("$[1].occupied", is(true)))
                .andExpect(jsonPath("$[2].slotId", is(3)))
                .andExpect(jsonPath("$[2].occupied", is(false)));

        // the null-distance slot must keep its last known state, not be touched
        mockMvc.perform(get("/api/parking/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots[0].slotNumber", is(1)))
                .andExpect(jsonPath("$.slots[0].occupied", is(true)))
                .andExpect(jsonPath("$.slots[1].slotNumber", is(2)))
                .andExpect(jsonPath("$.slots[1].occupied", is(true)))
                .andExpect(jsonPath("$.slots[3].slotNumber", is(4)))
                .andExpect(jsonPath("$.slots[3].known", is(false)));
    }

    @Test
    void fullParkingReturnsToAvailableAfterSensorUpdate() throws Exception {
        String fullPayload = "{\"availableSlots\":0,\"occupiedSlots\":4,\"slots\":["
                + "{\"slotNumber\":1,\"occupied\":true,\"distanceCm\":2},"
                + "{\"slotNumber\":2,\"occupied\":true,\"distanceCm\":3},"
                + "{\"slotNumber\":3,\"occupied\":true,\"distanceCm\":4},"
                + "{\"slotNumber\":4,\"occupied\":true,\"distanceCm\":5}]}";
        mockMvc.perform(post("/api/sensors/occupancy/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fullPayload))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/parking/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occupiedSlots", is(4)))
                .andExpect(jsonPath("$.availableSlots", is(0)))
                .andExpect(jsonPath("$.unknownSlots", is(0)))
                .andExpect(jsonPath("$.full", is(true)));

        String availablePayload = "{\"availableSlots\":4,\"occupiedSlots\":0,\"slots\":["
                + "{\"slotNumber\":1,\"occupied\":false,\"distanceCm\":100},"
                + "{\"slotNumber\":2,\"occupied\":false,\"distanceCm\":100},"
                + "{\"slotNumber\":3,\"occupied\":false,\"distanceCm\":100},"
                + "{\"slotNumber\":4,\"occupied\":false,\"distanceCm\":100}]}";
        mockMvc.perform(post("/api/sensors/occupancy/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(availablePayload))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/parking/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occupiedSlots", is(0)))
                .andExpect(jsonPath("$.availableSlots", is(4)))
                .andExpect(jsonPath("$.full", is(false)));
    }

    @Test
    void malformedOrDuplicateFourSlotBatchIsRejected() throws Exception {
        String duplicatePayload = "{\"availableSlots\":4,\"occupiedSlots\":0,\"slots\":["
                + "{\"slotNumber\":1,\"occupied\":false,\"distanceCm\":100},"
                + "{\"slotNumber\":1,\"occupied\":false,\"distanceCm\":100},"
                + "{\"slotNumber\":3,\"occupied\":false,\"distanceCm\":100},"
                + "{\"slotNumber\":4,\"occupied\":false,\"distanceCm\":100}]}";

        mockMvc.perform(post("/api/sensors/occupancy/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/sensors/occupancy/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicatePayload))
                .andExpect(status().isBadRequest());
    }

    // ---------------- Serial bridge reverse-warning events --------------

    @Test
    void reverseWarningPostActivatesSlot2() throws Exception {
        // bridge forwards the firmware's REVERSE_WARNING message
        mockMvc.perform(post("/api/parking/reverse-warning")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slotNumber\":2,\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.reverseWarning", is(true)));

        // the GET status endpoint must now reflect the warning
        mockMvc.perform(get("/api/parking/reverse-warning"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(true)));

        // the "active" alias also works
        mockMvc.perform(post("/api/parking/reverse-warning")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slotNumber\":2,\"reverseWarning\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));

        mockMvc.perform(get("/api/parking/reverse-warning"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));
    }
}
