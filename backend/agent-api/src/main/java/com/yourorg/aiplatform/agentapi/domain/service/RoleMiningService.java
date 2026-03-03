package com.yourorg.aiplatform.agentapi.domain.service;

import com.yourorg.aiplatform.agentapi.adapter.repo.EntitlementRepository;
import com.yourorg.aiplatform.agentapi.adapter.repo.ITRoleRepository;
import com.yourorg.aiplatform.agentapi.domain.model.Entitlement;
import com.yourorg.aiplatform.agentapi.domain.model.ITRole;
import com.yourorg.aiplatform.agentapi.domain.model.ITRoleStatus;
import java.time.Instant;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleMiningService {

    private final ITRoleRepository itRoleRepository;
    private final EntitlementRepository entitlementRepository;

    @Transactional
    public MiningResult mineRoles(MiningRequest request) {
        // Step 1: Build user-SG co-occurrence matrix from provided assignments
        Map<String, Set<String>> userToSgs = request.userSgAssignments();
        Map<String, String> userDepartments = request.userDepartments() != null
            ? request.userDepartments() : Map.of();

        // Filter to target department if specified
        Map<String, Set<String>> filtered = userToSgs;
        if (request.department() != null) {
            filtered = userToSgs.entrySet().stream()
                .filter(e -> request.department().equals(userDepartments.get(e.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        // Step 2: Find SG clusters (SGs that co-occur frequently)
        List<SgCluster> clusters = findClusters(filtered, request.minClusterSize());

        // Step 3: Convert clusters to IT Role suggestions
        List<ITRole> suggestedRoles = new ArrayList<>();
        for (SgCluster cluster : clusters) {
            if (cluster.confidence() < request.minConfidence()) {
                continue;
            }

            // Resolve entitlements for this cluster
            Set<Entitlement> entitlements = new HashSet<>();
            for (String sgName : cluster.sgNames()) {
                entitlementRepository.findBySourceSgName(sgName)
                    .ifPresent(entitlements::add);
            }

            // Determine common application
            String commonApp = entitlements.stream()
                .map(Entitlement::getBusinessApp)
                .filter(app -> app != null && !"N/A".equals(app))
                .collect(Collectors.groupingBy(a -> a, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");

            String roleName = generateRoleName(commonApp, cluster);

            // Check if role already exists
            if (itRoleRepository.findByName(roleName).isPresent()) {
                continue;
            }

            ITRole role = ITRole.builder()
                .name(roleName)
                .description("AI-mined role for " + commonApp + " based on SG co-occurrence in "
                    + (request.department() != null ? request.department() : "all") + " department")
                .applicationId(commonApp)
                .status(ITRoleStatus.SUGGESTED)
                .miningSource("co-occurrence-" + (request.department() != null ? request.department() : "all"))
                .confidence(cluster.confidence())
                .reasoningTrace(cluster.reasoning())
                .entitlements(entitlements)
                .createdAt(Instant.now())
                .build();

            itRoleRepository.save(role);
            suggestedRoles.add(role);
        }

        double avgConfidence = suggestedRoles.stream()
            .mapToDouble(ITRole::getConfidence)
            .average()
            .orElse(0.0);

        return MiningResult.builder()
            .suggestedITRoles(suggestedRoles.size())
            .avgConfidence(avgConfidence)
            .roles(suggestedRoles)
            .build();
    }

    @Transactional
    public ITRole approveRole(Long roleId) {
        ITRole role = itRoleRepository.findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("IT Role not found: " + roleId));
        role.setStatus(ITRoleStatus.APPROVED);
        return itRoleRepository.save(role);
    }

    @Transactional
    public ITRole activateRole(Long roleId) {
        ITRole role = itRoleRepository.findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("IT Role not found: " + roleId));
        if (role.getStatus() != ITRoleStatus.APPROVED) {
            throw new IllegalStateException("Role must be APPROVED before activation, current: " + role.getStatus());
        }
        role.setStatus(ITRoleStatus.ACTIVE);
        return itRoleRepository.save(role);
    }

    public List<ITRole> findByStatus(ITRoleStatus status) {
        return itRoleRepository.findByStatus(status);
    }

    private List<SgCluster> findClusters(Map<String, Set<String>> userToSgs, int minClusterSize) {
        // Build pair co-occurrence counts
        Map<String, Map<String, Integer>> coOccurrence = new HashMap<>();
        int totalUsers = userToSgs.size();

        for (Set<String> sgs : userToSgs.values()) {
            List<String> sgList = new ArrayList<>(sgs);
            for (int i = 0; i < sgList.size(); i++) {
                for (int j = i + 1; j < sgList.size(); j++) {
                    String a = sgList.get(i);
                    String b = sgList.get(j);
                    coOccurrence.computeIfAbsent(a, k -> new HashMap<>())
                        .merge(b, 1, Integer::sum);
                    coOccurrence.computeIfAbsent(b, k -> new HashMap<>())
                        .merge(a, 1, Integer::sum);
                }
            }
        }

        // Greedy clustering: group SGs with high co-occurrence
        Set<String> assigned = new HashSet<>();
        List<SgCluster> clusters = new ArrayList<>();

        // Count how many users hold each SG
        Map<String, Long> sgUserCount = new HashMap<>();
        for (var entry : userToSgs.entrySet()) {
            for (String sg : entry.getValue()) {
                sgUserCount.merge(sg, 1L, Long::sum);
            }
        }

        // Sort SGs by popularity descending
        List<String> sortedSgs = sgUserCount.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .toList();

        for (String seed : sortedSgs) {
            if (assigned.contains(seed)) continue;

            Set<String> cluster = new HashSet<>();
            cluster.add(seed);

            Map<String, Integer> neighbors = coOccurrence.getOrDefault(seed, Map.of());
            long seedCount = sgUserCount.getOrDefault(seed, 0L);

            for (var neighbor : neighbors.entrySet()) {
                if (assigned.contains(neighbor.getKey())) continue;
                // Include if co-occurrence is >= 60% of the seed's user count
                if (seedCount > 0 && neighbor.getValue() >= seedCount * 0.6) {
                    cluster.add(neighbor.getKey());
                }
            }

            if (cluster.size() >= minClusterSize) {
                // Calculate cluster confidence
                long minUsers = cluster.stream()
                    .mapToLong(sg -> sgUserCount.getOrDefault(sg, 0L))
                    .min().orElse(0);
                double confidence = totalUsers > 0 ? Math.min(0.99, (double) minUsers / totalUsers + 0.5) : 0.5;

                String reasoning = "SGs %s co-occur in %d+ users (%.0f%% of %d total). Cluster size: %d."
                    .formatted(cluster, minUsers, confidence * 100, totalUsers, cluster.size());

                clusters.add(new SgCluster(new ArrayList<>(cluster), confidence, reasoning));
                assigned.addAll(cluster);
            }
        }

        return clusters;
    }

    private String generateRoleName(String appName, SgCluster cluster) {
        // Infer access level from SG names
        boolean hasRead = cluster.sgNames().stream().anyMatch(s -> s.toLowerCase().contains("read") || s.toLowerCase().contains("view"));
        boolean hasWrite = cluster.sgNames().stream().anyMatch(s -> s.toLowerCase().contains("write") || s.toLowerCase().contains("admin"));

        String level = hasWrite ? "Admin" : hasRead ? "Reader" : "User";
        return appName.replace(" ", "-") + "-" + level;
    }

    @Builder
    public record MiningRequest(
        Map<String, Set<String>> userSgAssignments,
        Map<String, String> userDepartments,
        String department,
        int minClusterSize,
        double minConfidence
    ) {}

    @Builder
    public record MiningResult(
        int suggestedITRoles,
        double avgConfidence,
        List<ITRole> roles
    ) {}

    record SgCluster(List<String> sgNames, double confidence, String reasoning) {}
}
