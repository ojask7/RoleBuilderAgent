package com.yourorg.aiplatform.agentapi.agent.tools;

import com.yourorg.aiplatform.agentapi.adapter.client.CmdbClient;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Placeholder for ServiceNow/CMDB queries.
 */
@Component
@RequiredArgsConstructor
public class CmdbTool {

    private final CmdbClient cmdbClient;

    public Map<String, String> lookupBusinessApplication(String appServiceCode) {
        return cmdbClient.findBusinessApplication(appServiceCode);
    }
}
