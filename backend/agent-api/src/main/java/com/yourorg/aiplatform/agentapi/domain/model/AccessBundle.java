package com.yourorg.aiplatform.agentapi.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
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
@Table(name = "access_bundle")
public class AccessBundle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "business_role_id", nullable = false, unique = true)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private BusinessRole businessRole;

    @Builder.Default
    private int version = 1;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BundleStatus status = BundleStatus.DRAFT;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "kc27_status")
    @Builder.Default
    private Kc27Status kc27Status = Kc27Status.NOT_ASSESSED;

    @Column(name = "kc27_assessed_at")
    private Instant kc27AssessedAt;

    @Column(name = "kc27_gaps", length = 4000)
    private String kc27Gaps;

    @Column(name = "evidence_hash")
    private String evidenceHash;

    @Column(name = "total_entitlements")
    @Builder.Default
    private int totalEntitlements = 0;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}
