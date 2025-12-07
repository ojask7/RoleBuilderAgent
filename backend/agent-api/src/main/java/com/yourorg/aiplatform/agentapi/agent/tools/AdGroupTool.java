package com.yourorg.aiplatform.agentapi.agent.tools;

import com.yourorg.aiplatform.agentapi.adapter.client.AdClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Placeholder for Microsoft Graph / AD lookups.
 */
@Component
@RequiredArgsConstructor
public class AdGroupTool {

    private final AdClient adClient;

    public List<String> sampleMembers(String securityGroupName) {
        return adClient.findOwners(securityGroupName);
    }
}
