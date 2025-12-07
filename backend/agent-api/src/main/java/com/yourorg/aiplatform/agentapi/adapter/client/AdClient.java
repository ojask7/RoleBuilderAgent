package com.yourorg.aiplatform.agentapi.adapter.client;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AdClient {

    public List<String> findOwners(String securityGroupName) {
        return List.of("sg-owner@yourorg.com", "iam-duty@yourorg.com");
    }
}
