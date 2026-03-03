package com.yourorg.aiplatform.agentapi.api;

import com.yourorg.aiplatform.agentapi.agent.pipeline.BusinessRoleBuilderAgent;
import com.yourorg.aiplatform.agentapi.domain.model.BusinessRole;
import com.yourorg.aiplatform.agentapi.domain.model.BusinessRoleStatus;
import com.yourorg.aiplatform.agentapi.domain.service.BusinessRoleBuilderService;
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
@RequestMapping("/api/v1/roles/business")
@RequiredArgsConstructor
@Validated
public class BusinessRoleController {

    private final BusinessRoleBuilderService businessRoleBuilderService;
    private final BusinessRoleBuilderAgent businessRoleBuilderAgent;

    @PostMapping
    public ResponseEntity<BusinessRoleResponse> create(@Valid @RequestBody CreateRequest request) {
        var cmd = BusinessRoleBuilderService.CreateBusinessRoleRequest.builder()
            .name(request.name())
            .jobFunction(request.jobFunction())
            .department(request.department())
            .region(request.region())
            .owner(request.owner())
            .description(request.description())
            .itRoleIds(request.itRoleIds())
            .build();

        BusinessRole role = businessRoleBuilderService.createBusinessRole(cmd);
        return ResponseEntity.ok(toResponse(role));
    }

    @PostMapping("/suggest")
    public ResponseEntity<BusinessRoleBuilderAgent.BusinessRoleSuggestion> suggest(
        @Valid @RequestBody SuggestRequest request
    ) {
        var command = BusinessRoleBuilderAgent.BusinessRoleSuggestionCommand.builder()
            .jobFunction(request.jobFunction())
            .department(request.department())
            .region(request.region())
            .userItRoleAssignments(request.userItRoleAssignments())
            .build();

        return ResponseEntity.ok(businessRoleBuilderAgent.suggest(command));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BusinessRoleBuilderService.BusinessRoleDetail> get(@PathVariable Long id) {
        return ResponseEntity.ok(businessRoleBuilderService.getBusinessRoleDetail(id));
    }

    @GetMapping
    public ResponseEntity<List<BusinessRoleResponse>> list(
        @RequestParam(required = false) String department,
        @RequestParam(required = false) BusinessRoleStatus status
    ) {
        List<BusinessRole> roles;
        if (department != null) {
            roles = businessRoleBuilderService.findByDepartment(department);
        } else if (status != null) {
            roles = businessRoleBuilderService.findByStatus(status);
        } else {
            roles = businessRoleBuilderService.findByStatus(BusinessRoleStatus.DRAFT);
        }
        return ResponseEntity.ok(roles.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}/entitlements")
    public ResponseEntity<Set<String>> resolveEntitlements(@PathVariable Long id) {
        return ResponseEntity.ok(businessRoleBuilderService.resolveAllEntitlements(id));
    }

    private BusinessRoleResponse toResponse(BusinessRole role) {
        return new BusinessRoleResponse(
            role.getId(), role.getName(), role.getJobFunction(), role.getDepartment(),
            role.getRegion(), role.getOwner(), role.getStatus().name(),
            role.getItRoles().stream().map(it -> it.getName()).toList()
        );
    }

    public record CreateRequest(
        @NotBlank String name,
        @NotBlank String jobFunction,
        @NotBlank String department,
        String region,
        @NotBlank String owner,
        String description,
        @NotEmpty List<Long> itRoleIds
    ) {}

    public record SuggestRequest(
        @NotBlank String jobFunction,
        @NotBlank String department,
        String region,
        Map<String, Set<Long>> userItRoleAssignments
    ) {}

    public record BusinessRoleResponse(
        Long id,
        String name,
        String jobFunction,
        String department,
        String region,
        String owner,
        String status,
        List<String> itRoles
    ) {}
}
