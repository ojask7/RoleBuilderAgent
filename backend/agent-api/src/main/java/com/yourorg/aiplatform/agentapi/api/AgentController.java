package com.yourorg.aiplatform.agentapi.api;

import com.yourorg.aiplatform.agentapi.agent.pipeline.Kc27VerificationAgent;
import com.yourorg.aiplatform.agentapi.agent.pipeline.SgMappingAgent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
@Validated
public class AgentController {

    private final Kc27VerificationAgent kc27VerificationAgent;
    private final SgMappingAgent sgMappingAgent;

    @PostMapping("/kc27/verify")
    public ResponseEntity<Kc27VerificationResponse> verifyKC27(@Valid @RequestBody Kc27VerificationRequest request) {
        var command = new Kc27VerificationAgent.Kc27VerificationCommand(
            request.securityGroup(),
            request.description(),
            request.applicationServices(),
            request.businessApplications(),
            request.owner()
        );
        var result = kc27VerificationAgent.evaluate(command);
        var response = new Kc27VerificationResponse(result.status(), result.confidence(), result.rationale());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sg/mapping")
    public ResponseEntity<SgMappingResponse> mapSecurityGroup(@Valid @RequestBody SgMappingRequest request) {
        var command = new SgMappingAgent.SgMappingCommand(request.name(), request.description());
        var result = sgMappingAgent.mapSecurityGroup(command);
        var response = new SgMappingResponse(
            result.securityGroupName(),
            result.suggestedApplicationService(),
            result.suggestedBusinessApplication(),
            result.ownerSuggestion(),
            result.rationale()
        );
        return ResponseEntity.ok(response);
    }

    public record Kc27VerificationRequest(
        @NotBlank String securityGroup,
        @NotBlank String description,
        @NotEmpty List<String> applicationServices,
        @NotEmpty List<String> businessApplications,
        @NotBlank String owner
    ) { }

    public record Kc27VerificationResponse(String status, double confidence, String rationale) { }

    public record SgMappingRequest(@NotBlank String name, @NotBlank String description) { }

    public record SgMappingResponse(
        String securityGroup,
        String suggestedApplicationService,
        String suggestedBusinessApplication,
        String owner,
        String reasoning
    ) { }
}
