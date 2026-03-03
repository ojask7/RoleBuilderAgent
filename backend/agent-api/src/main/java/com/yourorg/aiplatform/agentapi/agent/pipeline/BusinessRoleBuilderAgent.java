package com.yourorg.aiplatform.agentapi.agent.pipeline;

import com.yourorg.aiplatform.agentapi.domain.model.ITRole;
import com.yourorg.aiplatform.agentapi.domain.model.ITRoleStatus;
import com.yourorg.aiplatform.agentapi.domain.service.BusinessRoleBuilderService;
import com.yourorg.aiplatform.agentapi.domain.service.RoleMiningService;
import com.yourorg.aiplatform.agentapi.util.LoggingUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
public class BusinessRoleBuilderAgent {

    private final ChatClient chatClient;
    private final RoleMiningService roleMiningService;
    private final BusinessRoleBuilderService businessRoleBuilderService;

    public BusinessRoleSuggestion suggest(BusinessRoleSuggestionCommand command) {
        LoggingUtils.withAgentContext("business-role-builder", command.jobFunction(), () ->
            log.info("Suggesting Business Role for jobFunction={} department={}",
                command.jobFunction(), command.department())
        );

        // Step 1: Find active/approved IT Roles relevant to this department
        List<ITRole> activeRoles = roleMiningService.findByStatus(ITRoleStatus.ACTIVE);
        List<ITRole> approvedRoles = roleMiningService.findByStatus(ITRoleStatus.APPROVED);

        List<ITRole> candidateRoles = new ArrayList<>();
        candidateRoles.addAll(activeRoles);
        candidateRoles.addAll(approvedRoles);

        // Step 2: Analyze which IT Roles co-occur for users in this job function
        Map<Long, Integer> rolePopularity = new HashMap<>();
        if (command.userItRoleAssignments() != null) {
            for (Set<Long> roleIds : command.userItRoleAssignments().values()) {
                for (Long roleId : roleIds) {
                    rolePopularity.merge(roleId, 1, Integer::sum);
                }
            }
        }

        int totalUsers = command.userItRoleAssignments() != null
            ? command.userItRoleAssignments().size() : 0;

        // Step 3: Select IT Roles held by >= 50% of population
        List<ITRole> suggestedItRoles = candidateRoles.stream()
            .filter(r -> {
                int count = rolePopularity.getOrDefault(r.getId(), 0);
                return totalUsers == 0 || count >= totalUsers * 0.5;
            })
            .toList();

        // Step 4: Identify outlier access
        List<OutlierAccess> outliers = candidateRoles.stream()
            .filter(r -> {
                int count = rolePopularity.getOrDefault(r.getId(), 0);
                return totalUsers > 0 && count > 0 && count < totalUsers * 0.5;
            })
            .map(r -> new OutlierAccess(
                r.getName(),
                rolePopularity.getOrDefault(r.getId(), 0),
                "Held by only %d/%d users — likely a personal exception".formatted(
                    rolePopularity.getOrDefault(r.getId(), 0), totalUsers)))
            .toList();

        // Step 5: AI-generate the business role name and description
        String aiSuggestion = generateAiSuggestion(command, suggestedItRoles, outliers);

        String suggestedName = "%s-%s".formatted(
            command.jobFunction().replace(" ", "-"),
            command.region() != null ? command.region() : command.department()
        );

        double populationCoverage = totalUsers > 0
            ? suggestedItRoles.stream()
                .mapToInt(r -> rolePopularity.getOrDefault(r.getId(), 0))
                .min().orElse(0) / (double) totalUsers
            : 0.0;

        return BusinessRoleSuggestion.builder()
            .suggestedName(suggestedName)
            .jobFunction(command.jobFunction())
            .department(command.department())
            .region(command.region())
            .itRoles(suggestedItRoles.stream()
                .map(r -> new SuggestedITRoleRef(r.getId(), r.getName(), r.getConfidence()))
                .toList())
            .outlierAccess(outliers)
            .populationCoverage(populationCoverage)
            .aiReasoning(aiSuggestion)
            .build();
    }

    private String generateAiSuggestion(
        BusinessRoleSuggestionCommand command,
        List<ITRole> itRoles,
        List<OutlierAccess> outliers
    ) {
        try {
            String roleList = itRoles.stream()
                .map(r -> "- %s (app=%s, confidence=%.2f)".formatted(
                    r.getName(), r.getApplicationId(), r.getConfidence()))
                .collect(Collectors.joining("\n"));

            String outlierList = outliers.stream()
                .map(o -> "- %s: %s".formatted(o.roleName(), o.recommendation()))
                .collect(Collectors.joining("\n"));

            Prompt prompt = new Prompt(List.of(
                new SystemMessage(
                    "You are an IAM Business Role architect. Given a job function, department, and a " +
                    "set of IT Roles, suggest a Business Role name and provide a brief justification " +
                    "for the bundle composition. Note any risks with outlier access. Keep response under 5 sentences."
                ),
                new UserMessage(
                    "Job function: %s\nDepartment: %s\nRegion: %s\n\nIT Roles to include:\n%s\n\nOutlier access (excluded):\n%s"
                        .formatted(command.jobFunction(), command.department(),
                            command.region() != null ? command.region() : "N/A",
                            roleList, outlierList.isEmpty() ? "None" : outlierList)
                )
            ));

            return chatClient.call(prompt)
                .getResult()
                .getOutput()
                .getContent();
        } catch (Exception ex) {
            log.warn("AI suggestion failed for {}: {}", command.jobFunction(), ex.getMessage());
            return "Heuristic suggestion: bundle %d IT Roles for %s in %s."
                .formatted(itRoles.size(), command.jobFunction(), command.department());
        }
    }

    @Builder
    public record BusinessRoleSuggestionCommand(
        String jobFunction,
        String department,
        String region,
        Map<String, Set<Long>> userItRoleAssignments
    ) {}

    @Builder
    public record BusinessRoleSuggestion(
        String suggestedName,
        String jobFunction,
        String department,
        String region,
        List<SuggestedITRoleRef> itRoles,
        List<OutlierAccess> outlierAccess,
        double populationCoverage,
        String aiReasoning
    ) {}

    public record SuggestedITRoleRef(Long id, String name, double confidence) {}

    public record OutlierAccess(String roleName, int heldByUsers, String recommendation) {}
}
