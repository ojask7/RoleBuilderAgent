---

# **Product Specification — Access Bundle & Business Role Builder Agent**

**Version**: 2.0
**Status**: Draft
**Last Updated**: 2026-03-03

---

## **SECTION 1 — Problem Statement**

### **The Real Problem: Groups Without Business Context**

Every enterprise IAM program eventually hits the same wall: thousands of AD Security Groups (SGs) exist as disconnected IT artifacts with no traceable link to business functions. The result is an ungovernable mess:

| Symptom | Root Cause |
|---------|-----------|
| 4,500 SGs in AD, nobody knows what half of them do | SGs created ad-hoc by IT ops, never linked to a role catalog |
| SailPoint knows 900 AS-linked SGs, the rest are dark | Onboarding was project-by-project; legacy SGs were never imported |
| Audit asks "who approved this access?" — no answer | SGs were granted directly, bypassing any role-based request flow |
| New joiner gets 47 individual SG grants over 6 months | No Business Role exists to bundle job-function access |
| Offboarding misses 12 SGs because they're not in any role | Orphan SGs outside IGA governance |
| KC27 fails because no documented owner or recertification | SGs exist below the governance waterline |

**The core gap is architectural**: there is no role hierarchy connecting raw SG entitlements → IT Roles → Business Roles → Job Functions. Without this, IGA tools like SailPoint can only govern what they can see, and they can't see what was never modeled.

### **What This Product Does**

This agent platform closes the gap by:

1. **Discovering** all SGs across AD, SailPoint, and CMDB — including the ones SailPoint doesn't know about
2. **Mining** usage patterns to suggest which SGs should be bundled into IT Roles
3. **Composing** IT Roles into Business Roles aligned to actual job functions
4. **Packaging** Business Roles as Access Bundles with full lifecycle (draft → review → active → deprecated)
5. **Scoring** every bundle for KC27 compliance readiness
6. **Generating** audit evidence packs that prove the chain: User → Business Role → IT Role → SG → Application

---

## **SECTION 2 — Core Concepts**

### **The Role Hierarchy**

```
Job Function (e.g., "Finance Analyst")
  └── Business Role (e.g., "Finance-Analyst-EMEA")
        ├── IT Role: "SAP-FI-Reader"
        │     ├── SG: CH_SG_SAP_FI_STG_Read
        │     ├── SG: CH_SG_SAP_FI_PRD_Read
        │     └── SG: SG_SAP_RFC_FI_ReadOnly
        ├── IT Role: "Reporting-Viewer"
        │     ├── SG: SG_PowerBI_Finance_View
        │     └── SG: SG_Legacy_Reports
        └── IT Role: "ServiceDesk-Basic"
              └── SG: SG_ITSM_SelfService
```

### **Key Definitions**

| Concept | Definition | Example |
|---------|-----------|---------|
| **Entitlement** | An atomic permission — maps 1:1 to an AD SG or SailPoint entitlement | `CH_SG_SAP_FI_STG_Read` |
| **IT Role** | A bundle of entitlements for a specific technical function on one application | `SAP-FI-Reader` (3 SGs for read access to SAP FI) |
| **Business Role** | A bundle of IT Roles representing a job function | `Finance-Analyst-EMEA` (SAP + Reporting + ServiceDesk) |
| **Access Bundle** | A deployable, lifecycle-managed package wrapping a Business Role | Bundle #42: `Finance-Analyst-EMEA` v2, status=ACTIVE |
| **Role Mining** | AI-driven analysis of SG co-occurrence in user populations to discover natural IT Role clusters | "These 3 SGs always appear together on Finance users" |

### **Why Bundles Matter**

Without Access Bundles:
- Joiner gets 47 individual SG requests, each needing separate approval
- Mover between departments means manually removing 47 SGs and adding 39 new ones
- Leaver offboarding misses SGs because there's no bundle to revoke atomically
- Audit can't answer "why does this person have this access?" because there's no business justification layer

With Access Bundles:
- Joiner gets 1 Business Role assignment → automatically provisions all IT Roles → all SGs
- Mover: swap one Business Role for another
- Leaver: revoke Business Role → cascading revocation of all underlying access
- Audit: full chain from business justification to individual entitlement

---

## **SECTION 3 — Personas & User Stories**

### **IAM Engineer / Role Mining Specialist**

> "I need the AI to analyze 4,500 SGs and tell me which ones cluster together based on who actually has them. Then I want to approve those clusters as IT Roles and compose them into Business Roles — not manually one-by-one in a spreadsheet."

- **US-1**: As an IAM Engineer, I can trigger role mining on a population segment (e.g., department=Finance) and receive suggested IT Role bundles with confidence scores.
- **US-2**: As an IAM Engineer, I can review, modify, and approve a suggested IT Role before it becomes active.
- **US-3**: As an IAM Engineer, I can compose multiple IT Roles into a Business Role and assign it to a job function.

### **Application Owner / Service Manager**

> "Show me every SG that touches my application — including the ones SailPoint missed — and help me decide which IT Role they should belong to."

- **US-4**: As an Application Owner, I can see all entitlements (SGs) mapped to my application across all sources (AD, SailPoint, CMDB).
- **US-5**: As an Application Owner, I can validate or reject the AI's suggested IT Role groupings for my application.

### **CISO / IAM Director**

> "For every Business Role in production, I need to see: who owns it, what IT Roles it contains, what SGs those resolve to, when it was last recertified, and whether KC27 is satisfied."

- **US-6**: As a CISO, I can view a compliance dashboard showing bundle coverage (% of SGs governed by a role) and KC27 status.
- **US-7**: As a CISO, I can generate an evidence pack for any Access Bundle showing the full provenance chain.

---

## **SECTION 4 — Data Model**

### **Entity Relationship**

```
┌─────────────────┐      ┌─────────────────┐      ┌──────────────────┐
│   Entitlement    │      │    IT Role       │      │  Business Role   │
│─────────────────│      │─────────────────│      │──────────────────│
│ id              │◄─┐   │ id              │◄─┐   │ id               │
│ source_sg_name  │  │   │ name            │  │   │ name             │
│ source_system   │  │   │ application_id  │  │   │ job_function     │
│ application_svc │  │   │ description     │  │   │ department       │
│ business_app    │  └───│ status          │  └───│ region           │
│ description     │  n:m │ owner           │  n:m │ owner            │
│ owner           │      │ created_at      │      │ status           │
│ discovered_from │      │ mining_source   │      │ created_at       │
│ status          │      └─────────────────┘      └──────────────────┘
│ confidence      │                                       │
│ created_at      │                                       │ 1:1
└─────────────────┘                                       ▼
                                                 ┌──────────────────┐
                                                 │  Access Bundle   │
                                                 │──────────────────│
                                                 │ id               │
                                                 │ business_role_id │
                                                 │ version          │
                                                 │ status (lifecycle)│
                                                 │ approved_by      │
                                                 │ approved_at      │
                                                 │ kc27_status      │
                                                 │ kc27_assessed_at │
                                                 │ evidence_hash    │
                                                 │ created_at       │
                                                 └──────────────────┘
```

### **Join Tables**

- `it_role_entitlement` — maps IT Roles ↔ Entitlements (many-to-many)
- `business_role_it_role` — maps Business Roles ↔ IT Roles (many-to-many)
- `user_business_role` — tracks which users hold which Business Roles

### **Lifecycle States**

```
Access Bundle Lifecycle:
  DRAFT → PENDING_REVIEW → APPROVED → ACTIVE → RECERTIFICATION_DUE → DEPRECATED

IT Role Lifecycle:
  SUGGESTED → REVIEW → APPROVED → ACTIVE → DEPRECATED

Entitlement Status:
  DISCOVERED → MAPPED → ORPHAN → GOVERNED → DEPRECATED
```

---

## **SECTION 5 — Input Data Sources**

### **1. AD Security Groups (Source of Truth for raw SGs)**

```
SGName                    | Description          | MemberCount | CreatedDate | Source
─────────────────────────────────────────────────────────────────────────────────────
CH_SG_SAP_FI_STG_Read    | SAP FI staging read  | 45          | 2021-03-15  | AD
CH_SG_SAP_FI_PRD_Read    | SAP FI prod read     | 42          | 2021-03-15  | AD
SG_Legacy_Reports         | Reporting access     | 120         | 2018-02-01  | AD
SG_PowerBI_Finance_View   | PowerBI Finance      | 88          | 2022-06-10  | AD
SG_Unknown_Legacy_42      | (no description)     | 3           | 2015-11-20  | AD
```

### **2. SailPoint Entitlement Catalog**

```
EntitlementName          | ApplicationService | BusinessApp  | Owner  | Governed
────────────────────────────────────────────────────────────────────────────────
CH_SG_SAP_FI_STG_Read   | SAP-FI-STG        | SAP-Finance  | Alice  | YES
CH_SG_SAP_FI_PRD_Read   | SAP-FI-PRD        | SAP-Finance  | Alice  | YES
SG_PowerBI_Finance_View  | PowerBI-Finance   | Reporting    | Bob    | YES
SG_Legacy_Reports        | (not mapped)      | (not mapped) | —      | NO
SG_Unknown_Legacy_42     | (not in SailPoint)| —            | —      | NO
```

### **3. CMDB Application Inventory**

```
ServiceName      | Type | BusinessApp    | Owner | Department
─────────────────────────────────────────────────────────────
SAP-FI-STG      | AS   | SAP-Finance   | Alice | Finance IT
SAP-FI-PRD      | AS   | SAP-Finance   | Alice | Finance IT
PowerBI-Finance  | AS   | Reporting     | Bob   | Digital
LegacyReports   | AS   | Reporting     | John  | Digital
```

### **4. MDT Ownership Table**

```
ApplicationName | MDT_Owner | Department  | CostCenter
────────────────────────────────────────────────────────
SAP-Finance    | Alice     | Finance IT  | CC-4400
Reporting      | Bob       | Digital     | CC-5500
LegacyReports  | John      | Digital     | CC-5500
```

### **5. User-SG Assignment Matrix (for Role Mining)**

```
UserID  | Department  | SGs Held
────────────────────────────────────────────────────────────────────
U001    | Finance     | CH_SG_SAP_FI_STG_Read, CH_SG_SAP_FI_PRD_Read, SG_PowerBI_Finance_View, SG_Legacy_Reports
U002    | Finance     | CH_SG_SAP_FI_STG_Read, CH_SG_SAP_FI_PRD_Read, SG_PowerBI_Finance_View
U003    | Finance     | CH_SG_SAP_FI_STG_Read, CH_SG_SAP_FI_PRD_Read, SG_PowerBI_Finance_View, SG_Legacy_Reports
U004    | IT Ops      | SG_Legacy_Reports, SG_Unknown_Legacy_42
```

---

## **SECTION 6 — AI Workflow: End-to-End Role Building**

### **Phase 1: Entitlement Discovery**

**Input**: Raw AD SG export
**Process**:
1. Ingest all SGs from AD
2. Cross-reference against SailPoint entitlement catalog
3. Cross-reference against CMDB application inventory
4. Lookup ownership from MDT
5. Classify each SG:
   - `GOVERNED` — exists in SailPoint with AS/BA mapping and owner
   - `PARTIALLY_MAPPED` — exists in SailPoint but missing AS, BA, or owner
   - `DISCOVERABLE` — not in SailPoint but matches a CMDB application (inferable)
   - `ORPHAN` — not in SailPoint, no CMDB match, no ownership trail

**Output**: Enriched entitlement catalog with status and confidence

```json
{
  "entitlement": "SG_Legacy_Reports",
  "status": "DISCOVERABLE",
  "inferredApplicationService": "LegacyReports",
  "inferredBusinessApp": "Reporting",
  "inferredOwner": "John",
  "ownerSource": "MDT",
  "confidence": 0.85,
  "reasoningTrace": [
    "Not found in SailPoint entitlement catalog",
    "SG name pattern 'Legacy_Reports' fuzzy-matched to CMDB service 'LegacyReports' (similarity=0.91)",
    "CMDB links LegacyReports → BA 'Reporting'",
    "MDT confirms owner=John for Reporting applications"
  ]
}
```

### **Phase 2: Role Mining (IT Role Suggestion)**

**Input**: User-SG assignment matrix + enriched entitlement catalog
**Process**:
1. Analyze SG co-occurrence patterns within department populations
2. Identify clusters of SGs that consistently appear together
3. For each cluster, determine the common application context
4. Suggest IT Role name, description, and member entitlements
5. Score confidence based on cluster tightness and population coverage

**Output**: Suggested IT Roles

```json
{
  "suggestedITRole": "SAP-FI-Reader",
  "application": "SAP-Finance",
  "entitlements": [
    "CH_SG_SAP_FI_STG_Read",
    "CH_SG_SAP_FI_PRD_Read"
  ],
  "populationMatch": {
    "department": "Finance",
    "usersWithAllSGs": 45,
    "usersWithPartialSGs": 3,
    "totalDepartmentUsers": 60
  },
  "confidence": 0.94,
  "reasoning": "These 2 SGs co-occur in 93% of Finance department users. Both map to SAP-Finance in CMDB. Naming convention 'CH_SG_SAP_FI_*_Read' confirms read-level access pattern."
}
```

### **Phase 3: Business Role Composition**

**Input**: Approved IT Roles + department/job-function metadata
**Process**:
1. Analyze which IT Roles co-occur across users in the same job function
2. Suggest Business Role groupings
3. Align with department and regional boundaries
4. Identify outlier access that shouldn't be in the base Business Role

**Output**: Suggested Business Roles

```json
{
  "suggestedBusinessRole": "Finance-Analyst-EMEA",
  "jobFunction": "Finance Analyst",
  "department": "Finance",
  "region": "EMEA",
  "itRoles": [
    { "name": "SAP-FI-Reader", "confidence": 0.94 },
    { "name": "Reporting-Viewer", "confidence": 0.87 },
    { "name": "ServiceDesk-Basic", "confidence": 0.72 }
  ],
  "outlierAccess": [
    {
      "sg": "SG_Unknown_Legacy_42",
      "heldBy": 1,
      "recommendation": "EXCLUDE — held by only 1 Finance user, likely personal exception"
    }
  ],
  "populationCoverage": "78% of Finance Analyst users would be fully covered by this role"
}
```

### **Phase 4: Access Bundle Packaging**

**Input**: Approved Business Role
**Process**:
1. Create Access Bundle wrapping the Business Role
2. Resolve full entitlement chain: Bundle → Business Role → IT Roles → Entitlements
3. Run KC27 compliance check on every node in the chain
4. Generate evidence pack
5. Set lifecycle to PENDING_REVIEW

**Output**: Access Bundle with compliance status

```json
{
  "bundleId": "AB-2026-042",
  "businessRole": "Finance-Analyst-EMEA",
  "version": 2,
  "status": "PENDING_REVIEW",
  "totalEntitlements": 6,
  "kc27": {
    "status": "PARTIALLY_COMPLIANT",
    "compliantEntitlements": 4,
    "nonCompliantEntitlements": 2,
    "gaps": [
      { "entitlement": "SG_Legacy_Reports", "issue": "No recertification history" },
      { "entitlement": "SG_PowerBI_Finance_View", "issue": "Missing approval documentation" }
    ]
  },
  "evidenceHash": "sha256:bf5c3a..."
}
```

---

## **SECTION 7 — API Design**

### **Entitlement Discovery**

#### `POST /api/v1/entitlements/discover`
Trigger discovery scan across all sources for a given scope.

**Request**:
```json
{ "scope": "ALL" }
```

**Response**:
```json
{
  "discoveryId": "disc-20260303-001",
  "totalSGs": 4500,
  "governed": 900,
  "partiallyMapped": 340,
  "discoverable": 1960,
  "orphan": 1300,
  "status": "COMPLETED"
}
```

#### `GET /api/v1/entitlements?status=ORPHAN&page=0&size=50`
List entitlements filtered by status.

---

### **Role Mining**

#### `POST /api/v1/roles/mine`
Trigger AI role mining on a population segment.

**Request**:
```json
{
  "department": "Finance",
  "minClusterSize": 3,
  "minConfidence": 0.7
}
```

**Response**:
```json
{
  "miningJobId": "mine-20260303-001",
  "suggestedITRoles": 8,
  "avgConfidence": 0.86,
  "roles": [ "..." ]
}
```

#### `POST /api/v1/roles/it`
Create/approve an IT Role from a mining suggestion.

#### `GET /api/v1/roles/it/{id}`
Get IT Role details including member entitlements.

---

### **Business Role Builder**

#### `POST /api/v1/roles/business`
Create a Business Role by composing IT Roles.

**Request**:
```json
{
  "name": "Finance-Analyst-EMEA",
  "jobFunction": "Finance Analyst",
  "department": "Finance",
  "region": "EMEA",
  "itRoleIds": [101, 102, 103],
  "owner": "alice@corp.com"
}
```

#### `POST /api/v1/roles/business/suggest`
AI-suggested Business Role composition for a job function.

#### `GET /api/v1/roles/business/{id}`
Full Business Role details with resolved IT Role → Entitlement chain.

---

### **Access Bundles**

#### `POST /api/v1/bundles`
Create an Access Bundle from a Business Role.

#### `PATCH /api/v1/bundles/{id}/lifecycle`
Transition bundle lifecycle state.

**Request**:
```json
{
  "action": "APPROVE",
  "approvedBy": "iam-governance@corp.com",
  "comment": "Reviewed and approved for Q1 rollout"
}
```

#### `GET /api/v1/bundles/{id}`
Full bundle details including KC27 status and evidence.

#### `GET /api/v1/bundles/{id}/evidence`
Generate audit evidence pack for a bundle.

---

### **KC27 Compliance**

#### `POST /api/v1/compliance/assess/{bundleId}`
Run KC27 assessment on a specific bundle.

#### `GET /api/v1/compliance/dashboard`
Aggregate compliance dashboard.

**Response**:
```json
{
  "totalSGs": 4500,
  "governedByBundle": 2800,
  "coveragePercent": 62.2,
  "bundlesCompliant": 34,
  "bundlesPartiallyCompliant": 12,
  "bundlesNonCompliant": 3,
  "orphanSGs": 1300,
  "lastAssessment": "2026-03-03T08:00:00Z"
}
```

---

## **SECTION 8 — MVP Scope**

### **MVP Includes (Phase 1 — 8 weeks)**

| # | Capability | Deliverable |
|---|-----------|-------------|
| 1 | Entitlement Discovery | Ingest SGs from AD/SailPoint/CMDB, classify status, infer ownership |
| 2 | IT Role Mining | AI-suggested IT Role bundles from SG co-occurrence analysis |
| 3 | IT Role CRUD | Create, review, approve IT Roles |
| 4 | Business Role Composition | AI-suggested and manual Business Role building from IT Roles |
| 5 | Access Bundle Lifecycle | Draft → Review → Active lifecycle for bundles |
| 6 | KC27 Assessment | Per-bundle compliance scoring with gap identification |
| 7 | Evidence Generation | Audit pack with full provenance chain |
| 8 | REST APIs | All endpoints documented above |

### **Post-MVP (Phase 2)**

- SailPoint IdentityNow provisioning integration (push approved bundles to IGA)
- ServiceNow request catalog integration (Business Roles as request items)
- Automated recertification scheduling
- Role explosion detection (flag when a Business Role has > N entitlements)
- Peer-group analysis for outlier access detection
- Human-in-the-loop review dashboard

---

## **SECTION 9 — Backlog (Epics)**

### **EPIC 1 — Entitlement Discovery Engine**
- Ingest AD SG exports (CSV/API)
- Cross-reference SailPoint entitlement catalog
- Cross-reference CMDB application inventory
- MDT ownership resolution
- SG classification (GOVERNED / PARTIALLY_MAPPED / DISCOVERABLE / ORPHAN)
- Naming convention parser for SG → application inference

### **EPIC 2 — AI Role Mining Pipeline**
- User-SG matrix ingestion
- SG co-occurrence analysis per department
- Cluster detection with configurable thresholds
- IT Role suggestion with confidence scoring
- Reasoning trace generation for each suggestion
- Review and approval workflow for suggested IT Roles

### **EPIC 3 — Business Role Builder**
- IT Role → Business Role composition engine
- Job function alignment analysis
- Department and regional boundary detection
- Outlier access identification
- Business Role CRUD with versioning

### **EPIC 4 — Access Bundle Lifecycle**
- Bundle creation from Business Role
- Lifecycle state machine (DRAFT → PENDING_REVIEW → APPROVED → ACTIVE → RECERTIFICATION_DUE → DEPRECATED)
- Approval workflow with audit trail
- Version management (new version when composition changes)
- Cascading status updates when underlying IT Roles change

### **EPIC 5 — KC27 Compliance & Evidence**
- Per-entitlement compliance check (owner, approval, recertification)
- Per-bundle aggregate compliance score
- Gap identification and remediation suggestions
- Evidence pack generation with cryptographic hash
- Compliance dashboard aggregation

### **EPIC 6 — Monitoring & Operations**
- Orphan SG count and trend
- Bundle coverage percentage
- Conflict detection (SailPoint vs CMDB disagreements)
- Mining job health and confidence distribution
- KC27 compliance drift alerts

---

## **SECTION 10 — Success Metrics**

| Metric | Baseline | Target (6 months) |
|--------|----------|--------------------|
| SGs governed by a role/bundle | 20% | 70% |
| Time to build a new Business Role | 2-3 weeks (manual) | < 2 hours (AI-assisted) |
| KC27 evidence gaps | ~60% of SGs | < 15% of SGs |
| New joiner provisioning | 47 individual requests | 1 Business Role assignment |
| Orphan SGs | ~1,300 unknown | < 200 remaining |
| Role mining false positive rate | N/A | < 20% |

---

## **END OF DOCUMENT**
