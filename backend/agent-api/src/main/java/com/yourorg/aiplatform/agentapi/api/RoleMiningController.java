package com.yourorg.aiplatform.agentapi.api;

import com.yourorg.aiplatform.agentapi.agent.pipeline.RoleMiningAgent;
import com.yourorg.aiplatform.agentapi.domain.model.ITRole;
import com.yourorg.aiplatform.agentapi.domain.model.ITRoleStatus;
import com.yourorg.aiplatform.agentapi.domain.service.RoleMiningService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Validated
public class RoleMiningController {

    private final RoleMiningAgent roleMiningAgent;
    private final RoleMiningService roleMiningService;

    @PostMapping("/mine")
    public ResponseEntity<RoleMiningAgent.RoleMiningResult> mine(
        @Valid @RequestBody MineRequest request
    ) {
        var command = RoleMiningAgent.RoleMiningCommand.builder()
            .userSgAssignments(request.userSgAssignments())
            .userDepartments(request.userDepartments())
            .department(request.department())
            .minClusterSize(request.minClusterSize())
            .minConfidence(request.minConfidence())
            .build();

        return ResponseEntity.ok(roleMiningAgent.mineAndSuggest(command));
    }

    @PostMapping("/it/{id}/approve")
    public ResponseEntity<ITRoleResponse> approve(@PathVariable Long id) {
        ITRole role = roleMiningService.approveRole(id);
        return ResponseEntity.ok(toResponse(role));
    }

    @PostMapping("/it/{id}/activate")
    public ResponseEntity<ITRoleResponse> activate(@PathVariable Long id) {
        ITRole role = roleMiningService.activateRole(id);
        return ResponseEntity.ok(toResponse(role));
    }

    @GetMapping("/it")
    public ResponseEntity<List<ITRoleResponse>> listITRoles(
        @RequestParam(required = false) ITRoleStatus status
    ) {
        List<ITRole> roles = status != null
            ? roleMiningService.findByStatus(status)
            : roleMiningService.findByStatus(ITRoleStatus.SUGGESTED);
        return ResponseEntity.ok(roles.stream().map(this::toResponse).toList());
    }

    private ITRoleResponse toResponse(ITRole role) {
        return new ITRoleResponse(
            role.getId(), role.getName(), role.getApplicationId(),
            role.getStatus().name(), role.getConfidence(), role.getReasoningTrace(),
            role.getEntitlements().stream()
                .map(e -> e.getSourceSgName()).toList()
        );
    }

    public record MineRequest(
        @NotEmpty Map<String, Set<String>> userSgAssignments,
        Map<String, String> userDepartments,
        String department,
        int minClusterSize,
        double minConfidence
    ) {}

    public record ITRoleResponse(
        Long id,
        String name,
        String applicationId,
        String status,
        double confidence,
        String reasoningTrace,
        List<String> entitlements
    ) {}
}
