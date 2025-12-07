package com.yourorg.aiplatform.agentapi.agent.pipeline;

import com.yourorg.aiplatform.agentapi.domain.model.SecurityGroup;
import com.yourorg.aiplatform.agentapi.domain.service.SgMappingService;
import com.yourorg.aiplatform.agentapi.util.LoggingUtils;
import java.util.List;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SgMappingAgent {

    private final ChatClient chatClient;
    private final SgMappingService mappingService;

    public SgMappingResult mapSecurityGroup(SgMappingCommand command) {
        SecurityGroup group = mappingService.enrichSecurityGroup(command.name(), command.description());
        String reasoning = generateReasoning(group);

        var insight = mappingService.deriveMappingInsight(group);

        LoggingUtils.withAgentContext("sg-mapping", group.getName(), () ->
            log.info("Suggested mapping {} -> {}", insight.applicationService(), insight.businessApplication())
        );

        return SgMappingResult.builder()
            .securityGroupName(group.getName())
            .suggestedApplicationService(insight.applicationService())
            .suggestedBusinessApplication(insight.businessApplication())
            .ownerSuggestion(insight.owner())
            .rationale(reasoning)
            .build();
    }

    @Builder
    public record SgMappingCommand(String name, String description) { }

    @Builder
    public record SgMappingResult(
        String securityGroupName,
        String suggestedApplicationService,
        String suggestedBusinessApplication,
        String ownerSuggestion,
        String rationale
    ) { }

    private String generateReasoning(SecurityGroup group) {
        try {
            Prompt prompt = new Prompt(
                List.of(
                    new SystemMessage("You map IAM security groups to owning applications."),
                    new UserMessage("Suggest AS/BA mapping for %s described as %s"
                        .formatted(group.getName(), group.getDescription()))
                )
            );
            return chatClient.call(prompt)
                .getResult()
                .getOutput()
                .getContent();
        } catch (Exception ex) {
            log.warn("Falling back to heuristic reasoning for group {}: {}", group.getName(), ex.getMessage());
            return "Heuristic suggestion using cached signals for %s.".formatted(group.getName());
        }
    }
}
