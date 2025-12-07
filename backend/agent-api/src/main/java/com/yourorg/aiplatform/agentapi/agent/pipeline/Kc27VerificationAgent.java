package com.yourorg.aiplatform.agentapi.agent.pipeline;

import com.yourorg.aiplatform.agentapi.domain.service.Kc27EvaluationService;
import com.yourorg.aiplatform.agentapi.util.LoggingUtils;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class Kc27VerificationAgent {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final Kc27EvaluationService evaluationService;

    public Kc27EvaluationResult evaluate(Kc27VerificationCommand command) {
        LoggingUtils.withAgentContext("kc27", command.securityGroup(), () ->
            log.info("Running KC27 evaluation for owner={}", command.owner())
        );

        String ragContext = vectorStore.similaritySearch(
            SearchRequest.query(command.description()).withTopK(2))
            .stream()
            .map(document -> document.getContent())
            .collect(Collectors.joining("\n---\n"));

        String aiSummary = generateAiSummary(command, ragContext);

        boolean compliant = evaluationService.isCompliant(command.securityGroup(), command.applicationServices());
        double confidence = evaluationService.estimateConfidence(command.applicationServices(), command.businessApplications());

        return Kc27EvaluationResult.builder()
            .status(compliant ? "COMPLIANT" : "REVIEW")
            .confidence(confidence)
            .rationale(aiSummary)
            .build();
    }

    @Builder
    public record Kc27VerificationCommand(
        String securityGroup,
        String description,
        List<String> applicationServices,
        List<String> businessApplications,
        String owner
    ) { }

    @Builder
    public record Kc27EvaluationResult(String status, double confidence, String rationale) { }

    private String generateAiSummary(Kc27VerificationCommand command, String ragContext) {
        try {
            Prompt prompt = new Prompt(
                List.of(
                    new SystemMessage("You are a KC27 compliance co-pilot."),
                    new UserMessage("Assess security group %s with context %s"
                        .formatted(command.securityGroup(), ragContext))
                )
            );
            return chatClient.call(prompt)
                .getResult()
                .getOutput()
                .getContent();
        } catch (Exception ex) {
            log.warn("Azure OpenAI call failed, returning fallback summary: {}", ex.getMessage());
            return "Unable to reach Azure OpenAI; defaulting to heuristic review for %s."
                .formatted(command.securityGroup());
        }
    }
}
