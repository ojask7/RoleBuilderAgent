package com.yourorg.aiplatform.agentapi.agent.pipeline;

import com.yourorg.aiplatform.agentapi.domain.model.Entitlement;
import com.yourorg.aiplatform.agentapi.domain.model.ITRole;
import com.yourorg.aiplatform.agentapi.domain.service.EntitlementDiscoveryService;
import com.yourorg.aiplatform.agentapi.domain.service.RoleMiningService;
import com.yourorg.aiplatform.agentapi.util.LoggingUtils;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleMiningAgent {

    private final ChatClient chatClient;
    private final RoleMiningService roleMiningService;
    private final EntitlementDiscoveryService discoveryService;

    public RoleMiningResult mineAndSuggest(RoleMiningCommand command) {
        LoggingUtils.withAgentContext("role-mining", command.department(), () ->
            log.info("Starting AI role mining for department={}", command.department())
        );

        // Step 1: Ensure entitlements are discovered
        List<String> allSgs = command.userSgAssignments().values().stream()
            .flatMap(Set::stream)
            .distinct()
            .toList();
        var discoveryResult = discoveryService.discoverEntitlements(allSgs);

        // Step 2: Run co-occurrence mining
        var miningRequest = RoleMiningService.MiningRequest.builder()
            .userSgAssignments(command.userSgAssignments())
            .userDepartments(command.userDepartments())
            .department(command.department())
            .minClusterSize(command.minClusterSize() > 0 ? command.minClusterSize() : 2)
            .minConfidence(command.minConfidence() > 0 ? command.minConfidence() : 0.7)
            .build();

        var miningResult = roleMiningService.mineRoles(miningRequest);

        // Step 3: AI enhancement — generate human-readable reasoning for each role
        List<SuggestedRole> enrichedRoles = miningResult.roles().stream()
            .map(this::enrichWithAiReasoning)
            .toList();

        return RoleMiningResult.builder()
            .department(command.department())
            .totalSGsAnalyzed(allSgs.size())
            .entitlementsDiscovered(discoveryResult.totalSGs())
            .suggestedRoles(enrichedRoles)
            .avgConfidence(miningResult.avgConfidence())
            .build();
    }

    private SuggestedRole enrichWithAiReasoning(ITRole role) {
        String aiReasoning;
        try {
            String entitlementList = role.getEntitlements().stream()
                .map(Entitlement::getSourceSgName)
                .collect(Collectors.joining(", "));

            Prompt prompt = new Prompt(List.of(
                new SystemMessage(
                    "You are an IAM role mining specialist. Given a suggested IT Role and its member " +
                    "security groups, provide a concise business justification for why these SGs should " +
                    "be bundled together. Focus on: (1) common application context, (2) access level " +
                    "consistency, (3) naming pattern evidence. Keep response under 3 sentences."
                ),
                new UserMessage(
                    "Suggested IT Role: '%s' for application '%s'. Member SGs: [%s]. " +
                    "Mining confidence: %.2f. Original reasoning: %s"
                        .formatted(role.getName(), role.getApplicationId(),
                            entitlementList, role.getConfidence(), role.getReasoningTrace())
                )
            ));

            aiReasoning = chatClient.call(prompt)
                .getResult()
                .getOutput()
                .getContent();
        } catch (Exception ex) {
            log.warn("AI reasoning generation failed for role {}: {}", role.getName(), ex.getMessage());
            aiReasoning = role.getReasoningTrace();
        }

        return SuggestedRole.builder()
            .id(role.getId())
            .name(role.getName())
            .applicationId(role.getApplicationId())
            .entitlements(role.getEntitlements().stream()
                .map(Entitlement::getSourceSgName).toList())
            .confidence(role.getConfidence())
            .reasoning(aiReasoning)
            .status(role.getStatus().name())
            .build();
    }

    @Builder
    public record RoleMiningCommand(
        Map<String, Set<String>> userSgAssignments,
        Map<String, String> userDepartments,
        String department,
        int minClusterSize,
        double minConfidence
    ) {}

    @Builder
    public record RoleMiningResult(
        String department,
        int totalSGsAnalyzed,
        int entitlementsDiscovered,
        List<SuggestedRole> suggestedRoles,
        double avgConfidence
    ) {}

    @Builder
    public record SuggestedRole(
        Long id,
        String name,
        String applicationId,
        List<String> entitlements,
        double confidence,
        String reasoning,
        String status
    ) {}
}
