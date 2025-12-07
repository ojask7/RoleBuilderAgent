package com.yourorg.aiplatform.agentapi.agent.tools;

import com.yourorg.aiplatform.agentapi.adapter.client.SailPointClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Stub integration that would eventually call SailPoint IdentityNow / IdentityIQ.
 */
@Component
@RequiredArgsConstructor
public class SailPointTool {

    private final SailPointClient sailPointClient;

    public List<String> fetchApplicationServices(String securityGroupName) {
        return sailPointClient.findApplicationServices(securityGroupName);
    }
}
