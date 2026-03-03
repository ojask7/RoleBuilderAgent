package com.yourorg.aiplatform.agentapi.api;

import com.yourorg.aiplatform.agentapi.domain.model.Entitlement;
import com.yourorg.aiplatform.agentapi.domain.model.EntitlementStatus;
import com.yourorg.aiplatform.agentapi.domain.service.EntitlementDiscoveryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/entitlements")
@RequiredArgsConstructor
@Validated
public class EntitlementController {

    private final EntitlementDiscoveryService discoveryService;

    @PostMapping("/discover")
    public ResponseEntity<DiscoveryResponse> discover(@Valid @RequestBody DiscoverRequest request) {
        var result = discoveryService.discoverEntitlements(request.sgNames());
        return ResponseEntity.ok(new DiscoveryResponse(
            result.totalSGs(),
            result.governed(),
            result.partiallyMapped(),
            result.discoverable(),
            result.orphan()
        ));
    }

    @GetMapping
    public ResponseEntity<Page<EntitlementResponse>> list(
        @RequestParam(required = false) EntitlementStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        Page<Entitlement> results;
        if (status != null) {
            results = discoveryService.findByStatus(status, PageRequest.of(page, size));
        } else {
            results = discoveryService.findByStatus(EntitlementStatus.DISCOVERED, PageRequest.of(page, size));
        }
        return ResponseEntity.ok(results.map(e -> new EntitlementResponse(
            e.getId(), e.getSourceSgName(), e.getApplicationService(), e.getBusinessApp(),
            e.getOwner(), e.getOwnerSource(), e.getStatus().name(), e.getConfidence(), e.getReasoningTrace()
        )));
    }

    @GetMapping("/summary")
    public ResponseEntity<EntitlementDiscoveryService.DiscoverySummary> summary() {
        return ResponseEntity.ok(discoveryService.getSummary());
    }

    public record DiscoverRequest(@NotEmpty List<String> sgNames) {}

    public record DiscoveryResponse(
        int totalSGs,
        int governed,
        int partiallyMapped,
        int discoverable,
        int orphan
    ) {}

    public record EntitlementResponse(
        Long id,
        String sourceSgName,
        String applicationService,
        String businessApp,
        String owner,
        String ownerSource,
        String status,
        double confidence,
        String reasoningTrace
    ) {}
}
