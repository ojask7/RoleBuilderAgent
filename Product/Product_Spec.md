

---

# **AI Product Specification — SG → Application Mapping Agent**


---

## **SECTION 1 — Product Idea**

### **1. Problem Statement**

IAM teams struggle because:

* AD Security Groups (SGs) are created without linkage to SailPoint.
* Some SGs mapped to Application Service (AS) and Business Service (BA), others not.
* CMDB contains more applications than SailPoint currently knows.
* Missing ownership mapping (MDT table).
* Missing approvals & recertification → KC27 non-compliance.

### **Example Pain Scenario**

* 4,500 AD SGs
* 900 mapped to SailPoint AS
* 600 mapped to SailPoint BA
* 2,300 applications in CMDB vs 700 in SailPoint
* 0 documented ownership → no audit trail

---

## **SECTION 2 — Personas**

### **IAM Engineer**

Needs automated visibility:

> “Which SG belongs to which application service, and why is SailPoint missing metadata?”

### **Application Owner**

Needs visibility of all SGs — including those SailPoint does not know.

### **CISO**

Wants:

> “For every SG → who owns it, which BA/AS it belongs to, who approved it, and is KC27 satisfied?”

---

## **SECTION 3 — Value Stream**

### **AI-Enhanced IAM Workflows**

1. Detect SGs mapped in SailPoint (AS/BA known).
2. Detect SGs where IT Role exists but not linked.
3. Detect SGs unknown to SailPoint → infer from CMDB + naming + MDT.
4. Assign provisional owners.
5. Produce KC27 compliance scoring.
6. Generate Evidence Packs.

---

## **SECTION 4 — Example Input Data**

### **1. User → SG Assignments**

```
UserID | SGName                  | SGDescription  | AssignedDate | Source
---------------------------------------------------------------------------
U12345 | CH_SG_App1_STG_Read     | Read access    | 2024-10-12   | AD
U12345 | SG_Legacy_Reports       | Reporting      | 2018-02-01   | AD
U54321 | SG_App2_PRD_Admin       | Admin PRD      | 2025-01-10   | AD
```

### **2. SailPoint AS List**

```
ASName       | AppName | Type | Owner
-----------------------------------------
App1-STG     | App1    | STG  | Alice
App1-PRD     | App1    | PRD  | Alice
App2-STG     | App2    | STG  | Bob
```

### **3. SailPoint BA List**

```
BAName   | BAOwner
-------------------
App1     | Alice
App2     | Bob
```

### **4. MDT Ownership Table**

```
ApplicationName | MDT_Owner | Department
----------------------------------------
App1            | Alice     | Finance IT
ReportingApp    | John      | Digital
```

### **5. CMDB AS/BA List**

```
ServiceName     | Type | AssociatedBA | Owner
-----------------------------------------------------
App1-STG        | AS   | App1         | Alice
App1-PRD        | AS   | App1         | Alice
App2-PRD        | AS   | App2         | Bob
LegacyApp-STG   | AS   | ReportingApp | John
ReportingApp    | BA   | ReportingApp | John
```

---

## **SECTION 5 — AI Workflow Example**

### **Input SG:**

`SG_Legacy_Reports`

### **AI Steps:**

1. Search in SailPoint → **Not found**
2. Search in CMDB → **Matches ReportingApp**
3. Lookup MDT → **Owner = John**
4. Infer:

   * AS = `LegacyApp-STG`
   * BA = `ReportingApp`
   * Suggested IT Role = `ReportingApp-Reader`
5. Identify:

   * No approval history
   * No recertification

### **Unified Record:**

```
AD: SG exists
SP: Unknown
CMDB: Linked to ReportingApp
MDT: Owner = John
AI: Create ReportingApp-Reader Role
KC27: Non-Compliant
```

---

## **SECTION 6 — API DESIGN (with Output Schemas)**

---

### **1. `/classifySG` — Classify SG into Application Category**

**Output:**

```json
{
  "sgName": "SG_Legacy_Reports",
  "classification": "Application Access - Reporting",
  "confidence": 0.91,
  "suggestedBA": "ReportingApp",
  "suggestedAS": "LegacyApp-STG"
}
```

---

### **2. `/mapSGToAS` — Determine Which AS the SG Belongs To**

```json
{
  "sgName": "CH_SG_App1_STG_Read",
  "AS_found_in_SP": "App1-STG",
  "AS_found_in_CMDB": "App1-STG",
  "finalAS": "App1-STG",
  "confidence": 0.99
}
```

---

### **3. `/mapSGToBA` — Map SG to Business Application**

```json
{
  "sgName": "SG_Legacy_Reports",
  "BA_found_in_SP": null,
  "BA_found_in_CMDB": "ReportingApp",
  "finalBA": "ReportingApp",
  "owner": "John",
  "confidence": 0.93
}
```

---

### **4. `/suggestITRole` — Recommend IT Role**

```json
{
  "sgName": "SG_Legacy_Reports",
  "recommendedITRoleName": "ReportingApp-Reader",
  "justification": "Matches BA 'ReportingApp' and usage pattern 'read-only'.",
  "confidence": 0.88
}
```

---

### **5. `/evaluateKC27` — Compliance Check**

```json
{
  "sgName": "CH_SG_App1_STG_Read",
  "approval": "Missing",
  "recertification": "Never done",
  "owner": "Alice",
  "reasonDocumented": "No",
  "kc27Status": "Non-Compliant",
  "risk": "Medium"
}
```

---

### **6. `/generateEvidence` — Produce Audit Evidence Pack**

```json
{
  "Control": "KC27",
  "sgName": "SG_Legacy_Reports",
  "users": ["U12345", "U67890"],
  "owner": "John",
  "AS": "LegacyApp-STG",
  "BA": "ReportingApp",
  "approvalHistory": [],
  "recertStatus": "None",
  "evidenceHash": "bf5c3a..."
}
```

---

## **SECTION 7 — MVP SCOPE**

### **MVP Includes**

* Ingest AD SGs
* Ingest SailPoint AS/BA
* Ingest CMDB AS/BA
* Merge into unified SG→AS→BA mapping
* Identify ownership
* Identify KC27 issues
* Suggest IT roles

---

## **SECTION 8 — BACKLOG**

### **EPIC 1 — Input Data Normalisation**

* Parse SG naming conventions
* Output: Structured SG metadata

### **EPIC 2 — SG→AS Mapping Engine**

* Use SailPoint, CMDB, MDT
* Output: JSON mapping (as above)

---

## **SECTION 9 — OPERATIONS**

### **Monitoring**

* SGs unmapped
* Conflicts (SailPoint ≠ CMDB)
* Missing owners
* KC27 evidence gaps

---

## **END OF DOCUMENT**
