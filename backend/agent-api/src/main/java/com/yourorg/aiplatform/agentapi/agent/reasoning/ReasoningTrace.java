package com.yourorg.aiplatform.agentapi.agent.reasoning;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReasoningTrace {
    Instant timestamp;
    String stage;
    String message;
}
