package com.yourorg.aiplatform.agentapi.api;

import com.yourorg.aiplatform.agentapi.domain.service.AccessBundleService;
import com.yourorg.aiplatform.agentapi.domain.service.AccessBundleService.BundleDetail;
import com.yourorg.aiplatform.agentapi.domain.service.AccessBundleService.ComplianceDashboard;
import com.yourorg.aiplatform.agentapi.domain.service.AccessBundleService.Kc27AssessmentResult;
import com.yourorg.aiplatform.agentapi.domain.service.AccessBundleService.LifecycleAction;
import com.yourorg.aiplatform.agentapi.domain.service.AccessBundleService.LifecycleActionType;
import com.yourorg.aiplatform.agentapi.domain.model.AccessBundle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bundles")
@RequiredArgsConstructor
@Validated
public class AccessBundleController {

    private final AccessBundleService bundleService;

    @PostMapping
    public ResponseEntity<BundleResponse> create(@Valid @RequestBody CreateBundleRequest request) {
        AccessBundle bundle = bundleService.createBundle(request.businessRoleId());
        return ResponseEntity.ok(new BundleResponse(
            bundle.getId(),
            bundle.getBusinessRole().getName(),
            bundle.getVersion(),
            bundle.getStatus().name(),
            bundle.getTotalEntitlements()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BundleDetail> get(@PathVariable Long id) {
        return ResponseEntity.ok(bundleService.getBundleDetail(id));
    }

    @PatchMapping("/{id}/lifecycle")
    public ResponseEntity<BundleResponse> transitionLifecycle(
        @PathVariable Long id,
        @Valid @RequestBody LifecycleRequest request
    ) {
        var action = LifecycleAction.builder()
            .action(request.action())
            .performedBy(request.performedBy())
            .comment(request.comment())
            .build();

        AccessBundle bundle = bundleService.transitionLifecycle(id, action);
        return ResponseEntity.ok(new BundleResponse(
            bundle.getId(),
            bundle.getBusinessRole().getName(),
            bundle.getVersion(),
            bundle.getStatus().name(),
            bundle.getTotalEntitlements()
        ));
    }

    @PostMapping("/{id}/assess")
    public ResponseEntity<Kc27AssessmentResult> assessKc27(@PathVariable Long id) {
        return ResponseEntity.ok(bundleService.assessKc27(id));
    }

    @GetMapping("/{id}/evidence")
    public ResponseEntity<Kc27AssessmentResult> getEvidence(@PathVariable Long id) {
        return ResponseEntity.ok(bundleService.assessKc27(id));
    }

    @GetMapping("/compliance/dashboard")
    public ResponseEntity<ComplianceDashboard> complianceDashboard() {
        return ResponseEntity.ok(bundleService.getComplianceDashboard());
    }

    public record CreateBundleRequest(@NotNull Long businessRoleId) {}

    public record LifecycleRequest(
        @NotNull LifecycleActionType action,
        @NotBlank String performedBy,
        String comment
    ) {}

    public record BundleResponse(
        Long bundleId,
        String businessRole,
        int version,
        String status,
        int totalEntitlements
    ) {}
}
