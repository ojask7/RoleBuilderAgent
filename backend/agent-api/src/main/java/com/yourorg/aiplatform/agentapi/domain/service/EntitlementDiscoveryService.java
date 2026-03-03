package com.yourorg.aiplatform.agentapi.domain.service;

import com.yourorg.aiplatform.agentapi.adapter.repo.EntitlementRepository;
import com.yourorg.aiplatform.agentapi.agent.tools.AdGroupTool;
import com.yourorg.aiplatform.agentapi.agent.tools.CmdbTool;
import com.yourorg.aiplatform.agentapi.agent.tools.SailPointTool;
import com.yourorg.aiplatform.agentapi.domain.model.Entitlement;
import com.yourorg.aiplatform.agentapi.domain.model.EntitlementStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntitlementDiscoveryService {

    private final EntitlementRepository entitlementRepository;
    private final SailPointTool sailPointTool;
    private final CmdbTool cmdbTool;
    private final AdGroupTool adGroupTool;

    @Transactional
    public DiscoveryResult discoverEntitlements(List<String> sgNames) {
        int governed = 0, partiallyMapped = 0, discoverable = 0, orphan = 0;
        List<Entitlement> results = new ArrayList<>();

        for (String sgName : sgNames) {
            var existing = entitlementRepository.findBySourceSgName(sgName);
            if (existing.isPresent()) {
                results.add(existing.get());
                continue;
            }

            Entitlement entitlement = classifyAndEnrich(sgName);
            entitlementRepository.save(entitlement);
            results.add(entitlement);

            switch (entitlement.getStatus()) {
                case GOVERNED -> governed++;
                case MAPPED -> partiallyMapped++;
                case DISCOVERED -> discoverable++;
                case ORPHAN -> orphan++;
                default -> {}
            }
        }

        return DiscoveryResult.builder()
            .totalSGs(sgNames.size())
            .governed(governed)
            .partiallyMapped(partiallyMapped)
            .discoverable(discoverable)
            .orphan(orphan)
            .entitlements(results)
            .build();
    }

    public Page<Entitlement> findByStatus(EntitlementStatus status, Pageable pageable) {
        return entitlementRepository.findByStatus(status, pageable);
    }

    public DiscoverySummary getSummary() {
        return DiscoverySummary.builder()
            .totalEntitlements(entitlementRepository.count())
            .governed(entitlementRepository.countByStatus(EntitlementStatus.GOVERNED))
            .partiallyMapped(entitlementRepository.countByStatus(EntitlementStatus.MAPPED))
            .discoverable(entitlementRepository.countByStatus(EntitlementStatus.DISCOVERED))
            .orphan(entitlementRepository.countByStatus(EntitlementStatus.ORPHAN))
            .build();
    }

    private Entitlement classifyAndEnrich(String sgName) {
        StringJoiner trace = new StringJoiner("\n");
        String appService = null;
        String businessApp = null;
        String owner = null;
        String ownerSource = null;
        EntitlementStatus status;
        double confidence = 0.0;

        // Step 1: Check SailPoint
        List<String> spServices = sailPointTool.fetchApplicationServices(sgName);
        if (!spServices.isEmpty()) {
            appService = spServices.get(0);
            trace.add("Found in SailPoint: AS=" + appService);
            confidence = 0.9;
        } else {
            trace.add("Not found in SailPoint entitlement catalog");
        }

        // Step 2: Check CMDB
        if (appService != null) {
            Map<String, String> cmdbRecord = cmdbTool.lookupBusinessApplication(appService);
            businessApp = cmdbRecord.get("businessApplication");
            if (businessApp != null) {
                trace.add("CMDB links " + appService + " -> BA '" + businessApp + "'");
                confidence = Math.max(confidence, 0.85);
            }
            String cmdbOwner = cmdbRecord.get("owner");
            if (cmdbOwner != null) {
                owner = cmdbOwner;
                ownerSource = "CMDB";
                trace.add("Owner from CMDB: " + owner);
            }
        } else {
            // Try to infer from SG name pattern via CMDB
            String inferredApp = inferApplicationFromSgName(sgName);
            if (inferredApp != null) {
                Map<String, String> cmdbRecord = cmdbTool.lookupBusinessApplication(inferredApp);
                businessApp = cmdbRecord.get("businessApplication");
                if (businessApp != null && !"N/A".equals(businessApp)) {
                    appService = inferredApp;
                    trace.add("SG name pattern inferred CMDB service '" + inferredApp + "' -> BA '" + businessApp + "'");
                    confidence = 0.7;
                    owner = cmdbRecord.get("owner");
                    ownerSource = "CMDB-inferred";
                }
            }
        }

        // Step 3: Check AD for ownership fallback
        if (owner == null) {
            List<String> adOwners = adGroupTool.sampleMembers(sgName);
            if (!adOwners.isEmpty()) {
                owner = adOwners.get(0);
                ownerSource = "AD";
                trace.add("Owner fallback from AD group membership: " + owner);
            }
        }

        // Step 4: Classify
        if (appService != null && businessApp != null && owner != null) {
            status = EntitlementStatus.GOVERNED;
            trace.add("Classification: GOVERNED (AS, BA, and owner all resolved)");
        } else if (appService != null) {
            status = EntitlementStatus.MAPPED;
            trace.add("Classification: MAPPED (AS found but missing BA or owner)");
        } else if (businessApp != null) {
            status = EntitlementStatus.DISCOVERED;
            trace.add("Classification: DISCOVERED (inferred from CMDB but not in SailPoint)");
        } else {
            status = EntitlementStatus.ORPHAN;
            confidence = 0.1;
            trace.add("Classification: ORPHAN (no match in SailPoint or CMDB)");
        }

        return Entitlement.builder()
            .sourceSgName(sgName)
            .sourceSystem("AD")
            .applicationService(appService)
            .businessApp(businessApp)
            .owner(owner)
            .ownerSource(ownerSource)
            .discoveredFrom("discovery-scan")
            .status(status)
            .confidence(confidence)
            .reasoningTrace(trace.toString())
            .createdAt(Instant.now())
            .build();
    }

    private String inferApplicationFromSgName(String sgName) {
        // Extract application hint from common SG naming patterns:
        // CH_SG_App1_STG_Read -> App1
        // SG_Legacy_Reports -> LegacyReports
        // SG_App2_PRD_Admin -> App2
        String normalized = sgName
            .replaceAll("^(CH_)?SG_", "")
            .replaceAll("_(STG|PRD|DEV|UAT)_(Read|Write|Admin|Full|View)$", "")
            .replaceAll("_(Reader|Writer|Admin|Viewer)$", "");

        if (normalized.isEmpty() || normalized.equals(sgName)) {
            return null;
        }
        return normalized.replace("_", "");
    }

    @Builder
    public record DiscoveryResult(
        int totalSGs,
        int governed,
        int partiallyMapped,
        int discoverable,
        int orphan,
        List<Entitlement> entitlements
    ) {}

    @Builder
    public record DiscoverySummary(
        long totalEntitlements,
        long governed,
        long partiallyMapped,
        long discoverable,
        long orphan
    ) {}
}
