# AccessForge — Client Deployment Guide

## Product Overview

**AccessForge** transforms flat, ungoverned AD Security Groups into a structured role hierarchy with full compliance traceability. It bridges the gap between raw IT access and business-aligned governance.

```
HR System → Active Directory → AccessForge → SailPoint → ServiceNow
(org data)   (flat SG groups)   (AI role mining)  (governance)  (requests)
```

### What You Get

| Capability | Description |
|-----------|-------------|
| Entitlement Discovery | Auto-discovers all SGs across AD/Azure AD, classifies each as Governed, Mapped, Discovered, or Orphan |
| AI Role Mining | Analyzes SG co-occurrence patterns to suggest IT Role bundles with confidence scoring |
| Business Role Builder | Composes IT Roles into job-function-aligned Business Roles |
| Access Bundles | Lifecycle-managed packages (Draft→Review→Active→Deprecated) with KC27 compliance |
| CISO Dashboard | Executive metrics: governance coverage, orphan counts, compliance status, risk heatmap |
| Integration Connectors | Pre-built for Azure AD, SailPoint IdentityNow, ServiceNow, on-prem AD |

---

## Deployment Architecture

```
┌─────────────────────────────────────────────────────────┐
│  Client Network                                         │
│                                                         │
│  ┌──────────┐   ┌──────────┐   ┌──────────────────┐   │
│  │ Azure AD  │   │ SailPoint│   │   ServiceNow      │   │
│  │ /Entra ID │   │ IDN      │   │                    │   │
│  └─────┬─────┘   └─────┬────┘   └────────┬──────────┘   │
│        │ Graph API      │ REST API       │ REST API     │
│        └────────────────┼────────────────┘              │
│                         │                               │
│              ┌──────────┴──────────┐                    │
│              │   AccessForge       │                    │
│              │  ┌────────────────┐ │                    │
│              │  │ React Frontend │ │ :3000              │
│              │  ├────────────────┤ │                    │
│              │  │  Spring Boot   │ │ :8080              │
│              │  │  + AI Agents   │ │                    │
│              │  ├────────────────┤ │                    │
│              │  │  PostgreSQL    │ │ :5432              │
│              │  └────────────────┘ │                    │
│              └─────────────────────┘                    │
│                                                         │
│  ┌──────────┐   ┌──────────┐                           │
│  │ On-Prem  │   │ HR System│                           │
│  │ AD (LDAP)│   │ (Workday)│                           │
│  └──────────┘   └──────────┘                           │
└─────────────────────────────────────────────────────────┘
```

---

## Prerequisites

| Component | Requirement |
|-----------|------------|
| Docker Engine | 24+ with Docker Compose v2 |
| Memory | 4 GB minimum for containers |
| Network | Outbound to Azure AD, SailPoint, ServiceNow APIs |
| Azure AD | App Registration with Directory.Read.All, Group.Read.All |
| SailPoint | Personal Access Token with read + role management |
| ServiceNow | Integration user with sc_cat_item and incident table access |
| Azure OpenAI | Deployment with gpt-4o-mini (for AI features) |

---

## Step-by-Step Deployment

### 1. Clone and Configure

```bash
git clone <repo-url> AccessForge
cd AccessForge
cp .env.example .env
```

### 2. Configure Client Connectors

Edit `.env` with the client's environment credentials:

```bash
# === Azure AD / Entra ID ===
# Azure Portal > App Registrations > your-app > Overview
AZURE_AD_TENANT_ID=contoso.onmicrosoft.com
AZURE_AD_CLIENT_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
AZURE_AD_CLIENT_SECRET=your-client-secret

# === SailPoint IdentityNow ===
# Admin Console > API Management > Personal Access Tokens
SAILPOINT_TENANT=contoso
SAILPOINT_CLIENT_ID=your-pat-client-id
SAILPOINT_CLIENT_SECRET=your-pat-client-secret

# === ServiceNow ===
# System Web Services > create integration user
SERVICENOW_INSTANCE=contoso
SERVICENOW_USERNAME=accessforge-integration
SERVICENOW_PASSWORD=your-snow-password

# === Azure OpenAI (for AI role mining) ===
AZURE_OPENAI_API_KEY=your-openai-key
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com/
```

### 3. Deploy

```bash
# Full stack: DB + migrations + API + Frontend
make run

# Check status
make status

# Verify deployment
make deploy-check
```

### 4. Load Initial Data

```bash
# Option A: Load demo data (for POC/demo)
make seed

# Option B: Trigger live discovery from Azure AD
curl -u agent:agent-secret -X POST http://localhost:8080/api/v1/integrations/azure-ad/sync

# Option C: Trigger SailPoint entitlement sync
curl -u agent:agent-secret -X POST http://localhost:8080/api/v1/integrations/sailpoint/sync
```

### 5. Access the Dashboard

Open `http://localhost:3000` and sign in:
- **Username**: `agent`
- **Password**: `agent-secret`

---

## Client Environment Scenarios

### Scenario: Flat AD Groups, No Role Mapping

This is the most common scenario. The client has thousands of AD SGs with no connection to business roles.

**Step 1: Discovery**
1. Dashboard > Entitlement Discovery
2. AccessForge syncs all SGs from Azure AD / on-prem AD
3. Cross-references against SailPoint entitlement catalog and CMDB
4. Classifies each SG: Governed, Mapped, Discovered, Orphan

**Step 2: Role Mining**
1. Dashboard > Role Mining
2. Select department (e.g., Finance)
3. Click "Start Role Mining"
4. AI analyzes SG co-occurrence patterns across users
5. Review suggested IT Roles with confidence scores
6. Approve or modify suggestions

**Step 3: Business Role Composition**
1. Dashboard > Business Roles
2. Click "AI Suggest Role" or "Create Role"
3. Select IT Roles to bundle into a Business Role
4. Assign job function, department, region, owner
5. Review the full hierarchy: Business Role → IT Roles → SGs

**Step 4: Bundle and Govern**
1. Dashboard > Access Bundles
2. Create Access Bundle from Business Role
3. Submit for Review → Approve → Activate
4. Run KC27 compliance assessment
5. Push approved roles to SailPoint for provisioning
6. Create ServiceNow catalog items for request workflows

**Step 5: CISO Report**
1. Dashboard > Executive Dashboard
2. Show governance coverage (% of SGs in a role)
3. Show orphan SG reduction trend
4. Show KC27 compliance status across all bundles
5. Risk heatmap by application area

---

## Integration Details

### Azure AD / Entra ID

**Setup:**
1. Azure Portal > App Registrations > New Registration
2. Name: `AccessForge-ReadOnly`
3. API Permissions > Add:
   - `Directory.Read.All` (Application)
   - `Group.Read.All` (Application)
   - `User.Read.All` (Application)
4. Grant Admin Consent
5. Certificates & Secrets > New Client Secret
6. Copy Tenant ID, Client ID, Client Secret to `.env`

**What AccessForge reads:**
- All security-enabled groups (displayName, description, members)
- User attributes (UPN, department, jobTitle, manager)
- Group membership matrix for role mining

### SailPoint IdentityNow

**Setup:**
1. Admin Console > API Management
2. Create Personal Access Token
3. Copy Client ID and Client Secret to `.env`

**What AccessForge does:**
- **Reads**: entitlements, sources, access profiles, roles
- **Writes**: creates access profiles (IT Roles) and roles (Business Roles)
- Cross-references SailPoint entitlements against AD SGs to determine governance status

### ServiceNow

**Setup:**
1. Create integration user: `accessforge-integration`
2. Assign roles: `catalog_admin`, `itil`
3. Copy instance name, username, password to `.env`

**What AccessForge does:**
- Creates service catalog items for approved Business Roles
- Handles access request workflows (RITM creation)
- Creates governance incidents for orphan SGs and compliance gaps
- Closes fulfilled request items after provisioning

---

## Operational Runbook

### Daily Operations

```bash
# Check system health
make status

# View recent logs
make logs

# Run compliance dashboard refresh
curl -u agent:agent-secret http://localhost:8080/api/v1/bundles/compliance/dashboard
```

### Weekly Tasks

```bash
# Sync latest groups from Azure AD
curl -u agent:agent-secret -X POST http://localhost:8080/api/v1/integrations/azure-ad/sync

# Sync SailPoint entitlements
curl -u agent:agent-secret -X POST http://localhost:8080/api/v1/integrations/sailpoint/sync

# Check for new orphan SGs
curl -u agent:agent-secret "http://localhost:8080/api/v1/entitlements?status=ORPHAN&page=0&size=100"
```

### Troubleshooting

```bash
# Full deployment readiness check
make deploy-check

# Reset database (destructive)
make db-reset && make seed

# Rebuild containers
make teardown && make run
```

---

## Success Criteria for Client Engagement

| Metric | Baseline (Week 0) | Target (Week 2) | Target (Week 8) |
|--------|-------------------|------------------|-------------------|
| SGs discovered | 0 | 100% of AD SGs | 100% |
| SGs classified | 0% | 80% | 95% |
| IT Roles created | 0 | 20-40 (AI-suggested) | 60+ (validated) |
| Business Roles | 0 | 5-10 (pilot depts) | 30+ (org-wide) |
| Active Bundles | 0 | 3-5 (pilot) | 20+ |
| KC27 coverage | 0% | 30% | 70%+ |
| Orphan SGs | Unknown | Identified | Reduced by 50%+ |
| CISO dashboard | N/A | Live | Board-ready |
