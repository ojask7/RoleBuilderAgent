package com.yourorg.aiplatform.agentapi.domain.service;

import com.yourorg.aiplatform.agentapi.adapter.repo.AccessBundleRepository;
import com.yourorg.aiplatform.agentapi.adapter.repo.BusinessRoleRepository;
import com.yourorg.aiplatform.agentapi.domain.model.AccessBundle;
import com.yourorg.aiplatform.agentapi.domain.model.BundleStatus;
import com.yourorg.aiplatform.agentapi.domain.model.BusinessRole;
import com.yourorg.aiplatform.agentapi.domain.model.Entitlement;
import com.yourorg.aiplatform.agentapi.domain.model.EntitlementStatus;
import com.yourorg.aiplatform.agentapi.domain.model.Kc27Status;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessBundleService {

    private static final Set<BundleStatus> LIFECYCLE_ORDER = Set.of(
        BundleStatus.DRAFT, BundleStatus.PENDING_REVIEW, BundleStatus.APPROVED,
        BundleStatus.ACTIVE, BundleStatus.RECERTIFICATION_DUE, BundleStatus.DEPRECATED
    );

    private final AccessBundleRepository bundleRepository;
    private final BusinessRoleRepository businessRoleRepository;

    @Transactional
    public AccessBundle createBundle(Long businessRoleId) {
        BusinessRole role = businessRoleRepository.findById(businessRoleId)
            .orElseThrow(() -> new IllegalArgumentException("Business Role not found: " + businessRoleId));

        if (bundleRepository.findByBusinessRoleId(businessRoleId).isPresent()) {
            throw new IllegalStateException("Bundle already exists for Business Role: " + role.getName());
        }

        int totalEntitlements = role.getItRoles().stream()
            .mapToInt(it -> it.getEntitlements().size())
            .sum();

        AccessBundle bundle = AccessBundle.builder()
            .businessRole(role)
            .version(1)
            .status(BundleStatus.DRAFT)
            .kc27Status(Kc27Status.NOT_ASSESSED)
            .totalEntitlements(totalEntitlements)
            .createdAt(Instant.now())
            .build();

        return bundleRepository.save(bundle);
    }

    @Transactional
    public AccessBundle transitionLifecycle(Long bundleId, LifecycleAction action) {
        AccessBundle bundle = bundleRepository.findById(bundleId)
            .orElseThrow(() -> new IllegalArgumentException("Bundle not found: " + bundleId));

        BundleStatus current = bundle.getStatus();
        BundleStatus next = resolveNextStatus(current, action);

        bundle.setStatus(next);

        if (action.action() == LifecycleActionType.APPROVE) {
            bundle.setApprovedBy(action.performedBy());
            bundle.setApprovedAt(Instant.now());
        }

        return bundleRepository.save(bundle);
    }

    @Transactional
    public Kc27AssessmentResult assessKc27(Long bundleId) {
        AccessBundle bundle = bundleRepository.findById(bundleId)
            .orElseThrow(() -> new IllegalArgumentException("Bundle not found: " + bundleId));

        BusinessRole role = bundle.getBusinessRole();
        List<ComplianceGap> gaps = new ArrayList<>();
        int totalEntitlements = 0;
        int compliant = 0;

        for (var itRole : role.getItRoles()) {
            for (Entitlement ent : itRole.getEntitlements()) {
                totalEntitlements++;

                List<String> entGaps = new ArrayList<>();
                if (ent.getOwner() == null || ent.getOwner().isBlank()) {
                    entGaps.add("No owner assigned");
                }
                if (ent.getStatus() == EntitlementStatus.ORPHAN) {
                    entGaps.add("Orphan entitlement - not in SailPoint or CMDB");
                }
                if (ent.getStatus() == EntitlementStatus.DISCOVERED) {
                    entGaps.add("Not yet governed in SailPoint");
                }

                if (entGaps.isEmpty()) {
                    compliant++;
                } else {
                    gaps.add(new ComplianceGap(ent.getSourceSgName(), entGaps));
                }
            }
        }

        Kc27Status kc27Status;
        if (totalEntitlements == 0) {
            kc27Status = Kc27Status.NOT_ASSESSED;
        } else if (compliant == totalEntitlements) {
            kc27Status = Kc27Status.COMPLIANT;
        } else if (compliant > 0) {
            kc27Status = Kc27Status.PARTIALLY_COMPLIANT;
        } else {
            kc27Status = Kc27Status.NON_COMPLIANT;
        }

        // Serialize gaps
        String gapSummary = gaps.stream()
            .map(g -> g.entitlement() + ": " + String.join("; ", g.issues()))
            .collect(Collectors.joining("\n"));

        // Generate evidence hash
        String evidenceContent = "bundle=%d|role=%s|entitlements=%d|compliant=%d|ts=%s"
            .formatted(bundleId, role.getName(), totalEntitlements, compliant, Instant.now());
        String evidenceHash = sha256(evidenceContent);

        bundle.setKc27Status(kc27Status);
        bundle.setKc27AssessedAt(Instant.now());
        bundle.setKc27Gaps(gapSummary);
        bundle.setEvidenceHash(evidenceHash);
        bundle.setTotalEntitlements(totalEntitlements);
        bundleRepository.save(bundle);

        return Kc27AssessmentResult.builder()
            .bundleId(bundleId)
            .businessRole(role.getName())
            .kc27Status(kc27Status)
            .totalEntitlements(totalEntitlements)
            .compliantEntitlements(compliant)
            .nonCompliantEntitlements(totalEntitlements - compliant)
            .gaps(gaps)
            .evidenceHash(evidenceHash)
            .build();
    }

    @Transactional(readOnly = true)
    public BundleDetail getBundleDetail(Long bundleId) {
        AccessBundle bundle = bundleRepository.findById(bundleId)
            .orElseThrow(() -> new IllegalArgumentException("Bundle not found: " + bundleId));

        BusinessRole role = bundle.getBusinessRole();
        List<String> itRoleNames = role.getItRoles().stream()
            .map(it -> it.getName())
            .toList();
        Set<String> entitlements = role.getItRoles().stream()
            .flatMap(it -> it.getEntitlements().stream())
            .map(Entitlement::getSourceSgName)
            .collect(Collectors.toSet());

        return BundleDetail.builder()
            .bundleId(bundle.getId())
            .businessRole(role.getName())
            .version(bundle.getVersion())
            .status(bundle.getStatus().name())
            .approvedBy(bundle.getApprovedBy())
            .approvedAt(bundle.getApprovedAt())
            .kc27Status(bundle.getKc27Status().name())
            .kc27AssessedAt(bundle.getKc27AssessedAt())
            .evidenceHash(bundle.getEvidenceHash())
            .itRoles(itRoleNames)
            .entitlements(new ArrayList<>(entitlements))
            .totalEntitlements(bundle.getTotalEntitlements())
            .build();
    }

    public ComplianceDashboard getComplianceDashboard() {
        long totalBundles = bundleRepository.count();
        long active = bundleRepository.countByStatus(BundleStatus.ACTIVE);
        long compliant = bundleRepository.countByKc27Status(Kc27Status.COMPLIANT);
        long partial = bundleRepository.countByKc27Status(Kc27Status.PARTIALLY_COMPLIANT);
        long nonCompliant = bundleRepository.countByKc27Status(Kc27Status.NON_COMPLIANT);

        return ComplianceDashboard.builder()
            .totalBundles(totalBundles)
            .activeBundles(active)
            .bundlesCompliant(compliant)
            .bundlesPartiallyCompliant(partial)
            .bundlesNonCompliant(nonCompliant)
            .lastAssessment(Instant.now())
            .build();
    }

    private BundleStatus resolveNextStatus(BundleStatus current, LifecycleAction action) {
        return switch (action.action()) {
            case SUBMIT_FOR_REVIEW -> {
                if (current != BundleStatus.DRAFT) throw new IllegalStateException("Can only submit DRAFT bundles for review");
                yield BundleStatus.PENDING_REVIEW;
            }
            case APPROVE -> {
                if (current != BundleStatus.PENDING_REVIEW) throw new IllegalStateException("Can only approve PENDING_REVIEW bundles");
                yield BundleStatus.APPROVED;
            }
            case ACTIVATE -> {
                if (current != BundleStatus.APPROVED) throw new IllegalStateException("Can only activate APPROVED bundles");
                yield BundleStatus.ACTIVE;
            }
            case FLAG_RECERTIFICATION -> {
                if (current != BundleStatus.ACTIVE) throw new IllegalStateException("Can only flag ACTIVE bundles for recertification");
                yield BundleStatus.RECERTIFICATION_DUE;
            }
            case DEPRECATE -> BundleStatus.DEPRECATED;
        };
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return "hash-unavailable";
        }
    }

    public enum LifecycleActionType {
        SUBMIT_FOR_REVIEW,
        APPROVE,
        ACTIVATE,
        FLAG_RECERTIFICATION,
        DEPRECATE
    }

    @Builder
    public record LifecycleAction(
        LifecycleActionType action,
        String performedBy,
        String comment
    ) {}

    @Builder
    public record Kc27AssessmentResult(
        Long bundleId,
        String businessRole,
        Kc27Status kc27Status,
        int totalEntitlements,
        int compliantEntitlements,
        int nonCompliantEntitlements,
        List<ComplianceGap> gaps,
        String evidenceHash
    ) {}

    public record ComplianceGap(String entitlement, List<String> issues) {}

    @Builder
    public record BundleDetail(
        Long bundleId,
        String businessRole,
        int version,
        String status,
        String approvedBy,
        Instant approvedAt,
        String kc27Status,
        Instant kc27AssessedAt,
        String evidenceHash,
        List<String> itRoles,
        List<String> entitlements,
        int totalEntitlements
    ) {}

    @Builder
    public record ComplianceDashboard(
        long totalBundles,
        long activeBundles,
        long bundlesCompliant,
        long bundlesPartiallyCompliant,
        long bundlesNonCompliant,
        Instant lastAssessment
    ) {}
}
