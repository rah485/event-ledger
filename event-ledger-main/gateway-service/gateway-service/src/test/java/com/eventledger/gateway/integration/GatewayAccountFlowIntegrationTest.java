package com.eventledger.gateway.integration;

import com.eventledger.gateway.constant.ApplicationConstants;
import com.eventledger.gateway.dto.request.EventRequest;
import com.eventledger.gateway.enums.TransactionType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "account-service.base-url=http://localhost:18081")
@AutoConfigureMockMvc
class GatewayAccountFlowIntegrationTest {

    private static final int ACCOUNT_SERVICE_PORT = 18081;
    private static final Duration HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(60);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static Process accountServiceProcess;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void startAccountService() throws Exception {
        Path accountServiceDir = Path.of("..", "..", "account-service", "account-service")
                .toAbsolutePath()
                .normalize();

        ProcessBuilder processBuilder = new ProcessBuilder(
                "cmd.exe",
                "/c",
                "mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=" + ACCOUNT_SERVICE_PORT
        );

        processBuilder.directory(accountServiceDir.toFile());
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        accountServiceProcess = processBuilder.start();
        waitForAccountServiceHealth();
    }

    @AfterAll
    static void stopAccountService() {
        if (accountServiceProcess != null && accountServiceProcess.isAlive()) {
            accountServiceProcess.destroy();
            try {
                if (!accountServiceProcess.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
                    accountServiceProcess.destroyForcibly();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                accountServiceProcess.destroyForcibly();
            }
        }
    }

    @Test
    void shouldExecuteRealGatewayToAccountServiceFlow() throws Exception {
        String traceId = "real-flow-trace-123";

        EventRequest request = new EventRequest(
                "EVT-REAL-1001",
                "ACC-REAL-1001",
                TransactionType.CREDIT,
                new BigDecimal("150.00"),
                "USD",
                Instant.parse("2026-01-01T10:00:00Z"),
                null
        );

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ApplicationConstants.TRACE_ID_HEADER, traceId)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(ApplicationConstants.TRACE_ID_HEADER, traceId))
                .andExpect(jsonPath("$.eventId").value("EVT-REAL-1001"))
                .andExpect(jsonPath("$.accountId").value("ACC-REAL-1001"));

        HttpRequest balanceRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + ACCOUNT_SERVICE_PORT + "/accounts/ACC-REAL-1001/balance"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> balanceResponse = HTTP_CLIENT.send(
                balanceRequest,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, balanceResponse.statusCode());

        JsonNode body = objectMapper.readTree(balanceResponse.body());
        assertEquals("ACC-REAL-1001", body.get("accountId").asText());
        assertEquals(150.00d, body.get("currentBalance").asDouble(), 0.001d);
    }

    private static void waitForAccountServiceHealth() throws Exception {
        Instant deadline = Instant.now().plus(HEALTH_CHECK_TIMEOUT);
        URI healthUri = URI.create("http://localhost:" + ACCOUNT_SERVICE_PORT + "/health");

        while (Instant.now().isBefore(deadline)) {
            if (accountServiceProcess != null && !accountServiceProcess.isAlive()) {
                throw new IllegalStateException("Account service process exited before becoming healthy.");
            }

            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(healthUri)
                        .timeout(Duration.ofSeconds(3))
                        .GET()
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(
                        request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200 && response.body().contains("\"status\":\"UP\"")) {
                    return;
                }
            } catch (IOException ex) {
                // Service is still starting.
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for account service startup.", ex);
            }
        }

        throw new IllegalStateException("Timed out waiting for account service to become healthy.");
    }
}