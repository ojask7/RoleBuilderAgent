package com.yourorg.aiplatform.agentapi.api;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health/ping")
    public Map<String, Object> ping() {
        return Map.of(
            "status", "UP",
            "timestamp", Instant.now().toString());
    }
}
