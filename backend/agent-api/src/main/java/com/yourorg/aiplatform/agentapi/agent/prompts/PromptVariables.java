package com.yourorg.aiplatform.agentapi.agent.prompts;

import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PromptVariables {

    public Map<String, Object> kc27Variables(String securityGroup, String owner) {
        return Map.of(
            "securityGroup", securityGroup,
            "owner", owner
        );
    }
}
