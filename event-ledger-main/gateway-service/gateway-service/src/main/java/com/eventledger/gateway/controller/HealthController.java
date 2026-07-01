package com.eventledger.gateway.controller;

import com.eventledger.gateway.dto.response.HealthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;

@RestController
public class HealthController {

    private static final Logger log =
            LoggerFactory.getLogger(HealthController.class);

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {

        log.info("Health check request received.");

        String dbStatus = checkDatabase();
        String overallStatus = "UP".equals(dbStatus) ? "UP" : "DOWN";

        log.info("Health check completed. status={} db={}", overallStatus, dbStatus);

        HealthResponse response = new HealthResponse(
                overallStatus,
                "gateway-service",
                dbStatus,
                Instant.now()
        );

        HttpStatus httpStatus = "UP".equals(overallStatus)
                ? HttpStatus.OK
                : HttpStatus.SERVICE_UNAVAILABLE;

        return ResponseEntity.status(httpStatus).body(response);
    }

    private String checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1)) {
                return "UP";
            }
            return "DOWN";
        } catch (Exception ex) {
            log.error("Database health check failed: {}", ex.getMessage());
            return "DOWN";
        }
    }
}