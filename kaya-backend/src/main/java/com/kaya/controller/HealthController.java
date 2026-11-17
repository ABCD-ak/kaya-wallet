package com.kaya.controller;

import com.kaya.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        boolean databaseUp = isDatabaseUp();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", databaseUp ? "UP" : "DEGRADED");
        data.put("database", databaseUp ? "UP" : "DOWN");
        data.put("application", "kaya-backend");
        data.put("javaVersion", System.getProperty("java.version"));
        data.put("time", OffsetDateTime.now(ZoneOffset.UTC).toString());

        if (databaseUp) {
            return ResponseEntity.ok(ApiResponse.ok(data));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(50300, "数据库不可用", data));
    }

    private boolean isDatabaseUp() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return result != null && result == 1;
        } catch (Exception ex) {
            log.warn("Database health check failed: {}", ex.getMessage());
            return false;
        }
    }
}
