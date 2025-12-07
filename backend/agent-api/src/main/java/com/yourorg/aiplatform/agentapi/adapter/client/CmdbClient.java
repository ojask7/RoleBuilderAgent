package com.yourorg.aiplatform.agentapi.adapter.client;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CmdbClient {

    public Map<String, String> findBusinessApplication(String appServiceCode) {
        return Map.of(
            "businessApplication", "BA-DigitalIdentity",
            "owner", "digital.identity@yourorg.com",
            "cmdbId", "CMDB-%s".formatted(appServiceCode)
        );
    }
}
