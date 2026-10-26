package com.smartparking.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.smartparking.backend.model.GateAction;
import com.smartparking.backend.model.GateStatus;
import com.smartparking.backend.model.GateType;

class Esp32WifiServiceTest {

    private static final String TOKEN = "test-gate-token";

    private final AtomicReference<String> gateState = new AtomicReference<>("CLOSED");
    private final AtomicBoolean rejectOpen = new AtomicBoolean();
    private HttpServer server;
    private final AtomicBoolean authorizedCommandReceived = new AtomicBoolean();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/gate/status", this::handleStatus);
        server.createContext("/api/gate/open", this::handleOpen);
        server.createContext("/api/gate/close", this::handleClose);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void pollsPhysicalStateAndConfirmsWifiGateCommand() {
        Esp32WifiService service = createService();

        service.pollGateStatus();
        assertTrue(service.isConnected());
        assertEquals(GateStatus.CLOSED, service.getGateState());
        assertTrue(service.isGateStateConfirmed());

        service.requestCommand(GateType.ENTRANCE, GateAction.OPEN);
        assertTrue(authorizedCommandReceived.get());
        assertEquals("PENDING", service.getCommandState());
        assertFalse(service.getGateState() == GateStatus.OPEN);

        service.pollGateStatus();
        assertEquals(GateStatus.OPEN, service.getGateState());
        assertEquals("CONFIRMED", service.getCommandState());
        assertTrue(service.isGateStateConfirmed());
    }

    @Test
    void malformedStatusDoesNotLookLikeAConnectedDevice() {
        gateState.set("unknown");
        Esp32WifiService service = createService();

        service.pollGateStatus();

        assertFalse(service.isConnected());
        assertFalse(service.isGateStateConfirmed());
    }

    @Test
    void firmwareFullCapacityRejectionIsPreserved() {
        rejectOpen.set(true);
        Esp32WifiService service = createService();
        service.pollGateStatus();

        assertThrows(IllegalStateException.class,
                () -> service.requestCommand(GateType.ENTRANCE, GateAction.OPEN));
        assertEquals("REJECTED", service.getCommandState());
        assertEquals(GateStatus.CLOSED, service.getGateState());
    }

    private Esp32WifiService createService() {
        return new Esp32WifiService(
                "http://127.0.0.1:" + server.getAddress().getPort(),
                TOKEN,
                5000,
                1000);
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        respond(exchange, 200, gateState.get());
    }

    private void handleOpen(HttpExchange exchange) throws IOException {
        boolean authorized = TOKEN.equals(exchange.getRequestHeaders().getFirst("X-Gate-Control-Token"));
        authorizedCommandReceived.set(authorized);
        if (!authorized) {
            respond(exchange, 403, "Invalid token");
            return;
        }
        if (rejectOpen.get()) {
            respond(exchange, 409, "ESP32 rejected OPEN because all four parking slots are occupied.");
            return;
        }
        gateState.set("OPEN");
        respond(exchange, 200, "OPEN command accepted.");
    }

    private void handleClose(HttpExchange exchange) throws IOException {
        gateState.set("CLOSED");
        respond(exchange, 200, "CLOSE command accepted.");
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, response.length);
        try (var output = exchange.getResponseBody()) {
            output.write(response);
        }
    }
}
