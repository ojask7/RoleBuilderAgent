# REST API Contracts

## Legacy Agents (v0)

### POST /api/agents/kc27/verify
Request:
```json
{
  "securityGroup": "SG-BILLING-ADMINS",
  "description": "Billing admin permissions",
  "applicationServices": ["APP_CORE_BILLING"],
  "businessApplications": ["BA-Finance"],
  "owner": "iam@yourorg.com"
}
```
Response:
```json
{
  "status": "COMPLIANT",
  "confidence": 0.87,
  "rationale": "..."
}
```

### POST /api/agents/sg/mapping
Request:
```json
{
  "name": "SG-BILLING-ADMINS",
  "description": "Billing admin permissions"
}
```
Response:
```json
{
  "securityGroup": "SG-BILLING-ADMINS",
  "suggestedApplicationService": "APP_CORE_BILLING",
  "suggestedBusinessApplication": "BA-DigitalIdentity",
  "owner": "iam@yourorg.com",
  "reasoning": "..."
}
```

---

## Access Bundle & Role Builder APIs (v1)

### Entitlement Discovery

#### POST /api/v1/entitlements/discover
Trigger discovery scan for a set of SG names. Classifies each SG as GOVERNED, MAPPED, DISCOVERED, or ORPHAN.

Request:
```json
{
  "sgNames": [
    "CH_SG_SAP_FI_STG_Read",
    "SG_Legacy_Reports",
    "SG_Unknown_Legacy_42"
  ]
}
```
Response:
```json
{
  "totalSGs": 3,
  "governed": 1,
  "partiallyMapped": 0,
  "discoverable": 1,
  "orphan": 1
}
```

#### GET /api/v1/entitlements?status=ORPHAN&page=0&size=50
List entitlements filtered by status (DISCOVERED, MAPPED, ORPHAN, GOVERNED, DEPRECATED).

#### GET /api/v1/entitlements/summary
Aggregate counts by entitlement status.

---

### Role Mining

#### POST /api/v1/roles/mine
Trigger AI-powered role mining. Analyzes SG co-occurrence patterns to suggest IT Role bundles.

Request:
```json
{
  "userSgAssignments": {
    "U001": ["CH_SG_SAP_FI_STG_Read", "CH_SG_SAP_FI_PRD_Read", "SG_PowerBI_Finance_View"],
    "U002": ["CH_SG_SAP_FI_STG_Read", "CH_SG_SAP_FI_PRD_Read", "SG_PowerBI_Finance_View"],
    "U003": ["SG_Legacy_Reports", "SG_Unknown_Legacy_42"]
  },
  "userDepartments": {
    "U001": "Finance",
    "U002": "Finance",
    "U003": "IT Ops"
  },
  "department": "Finance",
  "minClusterSize": 2,
  "minConfidence": 0.7
}
```
Response:
```json
{
  "department": "Finance",
  "totalSGsAnalyzed": 4,
  "entitlementsDiscovered": 4,
  "suggestedRoles": [
    {
      "id": 1,
      "name": "SAP-Finance-Reader",
      "applicationId": "SAP-Finance",
      "entitlements": ["CH_SG_SAP_FI_STG_Read", "CH_SG_SAP_FI_PRD_Read"],
      "confidence": 0.94,
      "reasoning": "These SGs co-occur in 100% of Finance users...",
      "status": "SUGGESTED"
    }
  ],
  "avgConfidence": 0.94
}
```

#### POST /api/v1/roles/it/{id}/approve
Approve a suggested IT Role.

#### POST /api/v1/roles/it/{id}/activate
Activate an approved IT Role.

#### GET /api/v1/roles/it?status=SUGGESTED
List IT Roles by status.

---

### Business Role Builder

#### POST /api/v1/roles/business
Create a Business Role by composing IT Roles.

Request:
```json
{
  "name": "Finance-Analyst-EMEA",
  "jobFunction": "Finance Analyst",
  "department": "Finance",
  "region": "EMEA",
  "owner": "alice@corp.com",
  "description": "Standard access bundle for EMEA Finance Analysts",
  "itRoleIds": [1, 2, 3]
}
```

#### POST /api/v1/roles/business/suggest
AI-suggested Business Role composition for a job function.

Request:
```json
{
  "jobFunction": "Finance Analyst",
  "department": "Finance",
  "region": "EMEA",
  "userItRoleAssignments": {
    "U001": [1, 2, 3],
    "U002": [1, 2],
    "U003": [1, 2, 3]
  }
}
```

#### GET /api/v1/roles/business/{id}
Full Business Role details with resolved IT Role -> Entitlement chain.

#### GET /api/v1/roles/business/{id}/entitlements
Resolve all entitlements (SG names) for a Business Role.

#### GET /api/v1/roles/business?department=Finance
List Business Roles by department or status.

---

### Access Bundles

#### POST /api/v1/bundles
Create an Access Bundle from a Business Role.

Request:
```json
{ "businessRoleId": 42 }
```

#### GET /api/v1/bundles/{id}
Full bundle details including KC27 status and evidence.

#### PATCH /api/v1/bundles/{id}/lifecycle
Transition bundle lifecycle state.

Request:
```json
{
  "action": "APPROVE",
  "performedBy": "iam-governance@corp.com",
  "comment": "Reviewed and approved for Q1 rollout"
}
```

Valid actions: `SUBMIT_FOR_REVIEW`, `APPROVE`, `ACTIVATE`, `FLAG_RECERTIFICATION`, `DEPRECATE`

Lifecycle: `DRAFT -> PENDING_REVIEW -> APPROVED -> ACTIVE -> RECERTIFICATION_DUE -> DEPRECATED`

#### POST /api/v1/bundles/{id}/assess
Run KC27 compliance assessment on a bundle.

Response:
```json
{
  "bundleId": 1,
  "businessRole": "Finance-Analyst-EMEA",
  "kc27Status": "PARTIALLY_COMPLIANT",
  "totalEntitlements": 6,
  "compliantEntitlements": 4,
  "nonCompliantEntitlements": 2,
  "gaps": [
    { "entitlement": "SG_Legacy_Reports", "issues": ["No recertification history"] }
  ],
  "evidenceHash": "sha256:bf5c3a2d..."
}
```

#### GET /api/v1/bundles/{id}/evidence
Generate audit evidence pack for a bundle.

#### GET /api/v1/bundles/compliance/dashboard
Aggregate compliance dashboard.

Response:
```json
{
  "totalBundles": 49,
  "activeBundles": 34,
  "bundlesCompliant": 28,
  "bundlesPartiallyCompliant": 12,
  "bundlesNonCompliant": 3,
  "lastAssessment": "2026-03-03T08:00:00Z"
}
```
