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
import jakarta.persistence.OneToOne;
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
@Table(name = "business_role")
public class BusinessRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "job_function")
    private String jobFunction;

    private String department;

    private String region;

    private String owner;

    private String description;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BusinessRoleStatus status = BusinessRoleStatus.DRAFT;

    @Builder.Default
    private double confidence = 0.0;

    @Column(name = "reasoning_trace", length = 4000)
    private String reasoningTrace;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @ManyToMany
    @JoinTable(
        name = "business_role_it_role",
        joinColumns = @JoinColumn(name = "business_role_id"),
        inverseJoinColumns = @JoinColumn(name = "it_role_id")
    )
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<ITRole> itRoles = new HashSet<>();

    @OneToOne(mappedBy = "businessRole")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private AccessBundle accessBundle;
}
