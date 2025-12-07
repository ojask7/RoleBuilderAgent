package com.yourorg.aiplatform.agentapi.util;

import java.util.Map;
import java.util.function.Supplier;
import lombok.experimental.UtilityClass;
import org.slf4j.MDC;

@UtilityClass
public class LoggingUtils {

    public void withAgentContext(String agentId, String subject, Runnable runnable) {
        withContext(agentId, subject, () -> {
            runnable.run();
            return null;
        });
    }

    public <T> T withContext(String agentId, String subject, Supplier<T> supplier) {
        Map<String, String> context = Map.of(
            "agentId", agentId,
            "subject", subject
        );
        try {
            context.forEach(MDC::put);
            return supplier.get();
        } finally {
            context.keySet().forEach(MDC::remove);
        }
    }
}
