package com.yourorg.aiplatform.agentapi.adapter.client;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SailPointClient {

    public List<String> findApplicationServices(String securityGroupName) {
        return List.of("APP_CORE_BILLING", "APP_INTG_FRAUD");
    }
}
