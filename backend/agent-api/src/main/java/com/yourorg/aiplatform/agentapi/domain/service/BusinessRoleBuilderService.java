package com.yourorg.aiplatform.agentapi.domain.service;

import com.yourorg.aiplatform.agentapi.adapter.repo.BusinessRoleRepository;
import com.yourorg.aiplatform.agentapi.adapter.repo.ITRoleRepository;
import com.yourorg.aiplatform.agentapi.domain.model.BusinessRole;
import com.yourorg.aiplatform.agentapi.domain.model.BusinessRoleStatus;
import com.yourorg.aiplatform.agentapi.domain.model.Entitlement;
import com.yourorg.aiplatform.agentapi.domain.model.ITRole;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
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
public class BusinessRoleBuilderService {

    private final BusinessRoleRepository businessRoleRepository;
    private final ITRoleRepository itRoleRepository;

    @Transactional
    public BusinessRole createBusinessRole(CreateBusinessRoleRequest request) {
        Set<ITRole> itRoles = new HashSet<>();
        for (Long itRoleId : request.itRoleIds()) {
            ITRole itRole = itRoleRepository.findById(itRoleId)
                .orElseThrow(() -> new IllegalArgumentException("IT Role not found: " + itRoleId));
            itRoles.add(itRole);
        }

        BusinessRole role = BusinessRole.builder()
            .name(request.name())
            .jobFunction(request.jobFunction())
            .department(request.department())
            .region(request.region())
            .owner(request.owner())
            .description(request.description())
            .status(BusinessRoleStatus.DRAFT)
            .itRoles(itRoles)
            .createdAt(Instant.now())
            .build();

        return businessRoleRepository.save(role);
    }

    @Transactional(readOnly = true)
    public BusinessRoleDetail getBusinessRoleDetail(Long id) {
        BusinessRole role = businessRoleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Business Role not found: " + id));

        List<ITRoleDetail> itRoleDetails = role.getItRoles().stream()
            .map(itRole -> {
                List<String> entitlementNames = itRole.getEntitlements().stream()
                    .map(Entitlement::getSourceSgName)
                    .toList();
                return new ITRoleDetail(itRole.getId(), itRole.getName(), itRole.getApplicationId(),
                    itRole.getStatus().name(), entitlementNames);
            })
            .toList();

        int totalEntitlements = role.getItRoles().stream()
            .mapToInt(it -> it.getEntitlements().size())
            .sum();

        return BusinessRoleDetail.builder()
            .id(role.getId())
            .name(role.getName())
            .jobFunction(role.getJobFunction())
            .department(role.getDepartment())
            .region(role.getRegion())
            .owner(role.getOwner())
            .status(role.getStatus().name())
            .itRoles(itRoleDetails)
            .totalEntitlements(totalEntitlements)
            .build();
    }

    @Transactional
    public BusinessRole transitionStatus(Long id, BusinessRoleStatus newStatus) {
        BusinessRole role = businessRoleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Business Role not found: " + id));
        role.setStatus(newStatus);
        return businessRoleRepository.save(role);
    }

    public List<BusinessRole> findByDepartment(String department) {
        return businessRoleRepository.findByDepartment(department);
    }

    public List<BusinessRole> findByStatus(BusinessRoleStatus status) {
        return businessRoleRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public Set<String> resolveAllEntitlements(Long businessRoleId) {
        BusinessRole role = businessRoleRepository.findById(businessRoleId)
            .orElseThrow(() -> new IllegalArgumentException("Business Role not found: " + businessRoleId));

        return role.getItRoles().stream()
            .flatMap(itRole -> itRole.getEntitlements().stream())
            .map(Entitlement::getSourceSgName)
            .collect(Collectors.toSet());
    }

    @Builder
    public record CreateBusinessRoleRequest(
        String name,
        String jobFunction,
        String department,
        String region,
        String owner,
        String description,
        List<Long> itRoleIds
    ) {}

    @Builder
    public record BusinessRoleDetail(
        Long id,
        String name,
        String jobFunction,
        String department,
        String region,
        String owner,
        String status,
        List<ITRoleDetail> itRoles,
        int totalEntitlements
    ) {}

    public record ITRoleDetail(
        Long id,
        String name,
        String applicationId,
        String status,
        List<String> entitlements
    ) {}
}
