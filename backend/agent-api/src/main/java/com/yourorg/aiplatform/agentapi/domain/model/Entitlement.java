package com.yourorg.aiplatform.agentapi.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "entitlement")
public class Entitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_sg_name", nullable = false)
    private String sourceSgName;

    @Column(name = "source_system")
    private String sourceSystem;

    @Column(name = "application_service")
    private String applicationService;

    @Column(name = "business_app")
    private String businessApp;

    private String description;

    private String owner;

    @Column(name = "owner_source")
    private String ownerSource;

    @Column(name = "discovered_from")
    private String discoveredFrom;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EntitlementStatus status = EntitlementStatus.DISCOVERED;

    @Builder.Default
    private double confidence = 0.0;

    @Column(name = "reasoning_trace", length = 4000)
    private String reasoningTrace;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @ManyToMany(mappedBy = "entitlements")
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<ITRole> itRoles = new HashSet<>();
}
