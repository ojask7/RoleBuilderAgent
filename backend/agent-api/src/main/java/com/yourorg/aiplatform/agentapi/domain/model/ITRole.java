package com.yourorg.aiplatform.agentapi.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
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
@Table(name = "it_role")
public class ITRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "application_id")
    private String applicationId;

    private String owner;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ITRoleStatus status = ITRoleStatus.SUGGESTED;

    @Column(name = "mining_source")
    private String miningSource;

    @Builder.Default
    private double confidence = 0.0;

    @Column(name = "reasoning_trace", length = 4000)
    private String reasoningTrace;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @ManyToMany
    @JoinTable(
        name = "it_role_entitlement",
        joinColumns = @JoinColumn(name = "it_role_id"),
        inverseJoinColumns = @JoinColumn(name = "entitlement_id")
    )
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Entitlement> entitlements = new HashSet<>();

    @ManyToMany(mappedBy = "itRoles")
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<BusinessRole> businessRoles = new HashSet<>();
}
