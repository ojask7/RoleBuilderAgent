# Running the RoleBuilderAgent

## Prerequisites

| Tool | Version | Check |
|------|---------|-------|
| Docker + Docker Compose | 24+ | `docker --version && docker compose version` |
| Java JDK | 21 | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| psql (optional) | 14+ | `psql --version` |

## Quick Start (5 minutes)

```bash
# 1. Clone and enter the repo
git clone <repo-url> && cd RoleBuilderAgent

# 2. First-time setup: creates .env, starts DB, runs migrations, loads seed data
make setup

# 3. Edit .env with your Azure OpenAI key (required for AI features)
#    Without this key, the app still runs but AI agents will fall back to heuristics.
vi .env

# 4. Start the API (DB in Docker, app on host with hot-reload)
make dev
```

The API is now running at **http://localhost:8080**.

## Running Options

### Option A: Local Dev (recommended for development)

Database runs in Docker. Spring Boot runs on your machine with hot-reload.

```bash
make dev          # Start DB + run Spring Boot
make dev-stop     # Stop the database
```

### Option B: Full Docker (recommended for demo/CI)

Everything runs in Docker containers.

```bash
make run-docker   # Build JAR, build image, start all containers
make status       # Check container health
make logs         # Tail all container logs
```

### Option C: Manual Step-by-Step

```bash
# Start Postgres
docker compose -f infra/local/docker-compose.yml up -d postgres

# Run Flyway migrations
docker compose -f infra/local/docker-compose.yml up flyway

# Load sample data
bash scripts/load-sample-data.sh

# Run Spring Boot
mvn -pl backend/agent-api spring-boot:run -Dspring-boot.run.profiles=dev \
    -Dspring-boot.run.jvmArguments="-Dspring.flyway.enabled=false"
```

## Environment Variables

All configuration is in `.env`. Copy from `.env.example` and edit:

```bash
cp .env.example .env
```

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `POSTGRES_DB` | No | `agentdb` | Database name |
| `POSTGRES_USER` | No | `agent` | Database user |
| `POSTGRES_PASSWORD` | No | `agentpass` | Database password |
| `POSTGRES_PORT` | No | `5432` | Host port for Postgres |
| `AGENT_API_PORT` | No | `8080` | Host port for the API |
| `AGENT_API_USERNAME` | No | `agent` | Basic auth username |
| `AGENT_API_PASSWORD` | No | `agent-secret` | Basic auth password |
| `AZURE_OPENAI_API_KEY` | **Yes*** | `dummy-key` | Azure OpenAI API key |
| `AZURE_OPENAI_ENDPOINT` | **Yes*** | — | Azure OpenAI endpoint URL |
| `AZURE_OPENAI_CHAT_DEPLOYMENT` | No | `gpt-4o-mini` | Chat model deployment name |
| `AZURE_OPENAI_EMBEDDING_DEPLOYMENT` | No | `text-embedding-3-large` | Embedding model name |

*Required for AI agent features (role mining, business role suggestion). The app starts without them but AI calls will fall back to heuristic responses.

## Database

### Connect to Postgres

```bash
# Via Makefile
make db-psql

# Or directly
docker exec -it rolebuilder-postgres psql -U agent -d agentdb
```

### Run Migrations

Migrations live in `data/migrations/flyway/` and are applied by Flyway.

```bash
make db-migrate   # Run pending migrations via Docker Flyway
```

### Reset Database (destructive)

```bash
make db-reset     # Drops DB, recreates, runs migrations
make seed         # Reload sample data
```

### pgAdmin (web UI)

```bash
make pgadmin      # Start at http://localhost:5050
```

Login: `admin@local.dev` / `admin`
Add server: host=`postgres`, port=`5432`, user=`agent`, password=`agentpass`

## Sample Data

The seed script (`scripts/load-sample-data.sh`) loads realistic IAM data:

| Entity | Count | Description |
|--------|-------|-------------|
| Security Groups | 15 | AD SGs across SAP, Reporting, HR, IT Ops |
| Entitlements | 15 | Enriched SG catalog with classification |
| IT Roles | 6 | SAP-FI-Reader, Reporting-Viewer, HR-Read, etc. |
| Business Roles | 3 | Finance-Analyst-EMEA, HR-Specialist, HR-Admin |
| Access Bundles | 2 | Finance bundle (ACTIVE), HR bundle (DRAFT) |
| User Assignments | 15 | 12 Finance users, 3 HR users |
| App Services | 7 | CMDB application services |
| Business Apps | 5 | Tier-1 and Tier-2 applications |

```bash
make seed         # Load/reload sample data
```

## API Quick Reference

All endpoints require Basic Auth (`agent` / `agent-secret` by default).

### Health Check (no auth)
```bash
curl http://localhost:8080/actuator/health
```

### Entitlement Discovery
```bash
# Summary of all entitlements by status
curl -u agent:agent-secret http://localhost:8080/api/v1/entitlements/summary

# List orphan SGs
curl -u agent:agent-secret "http://localhost:8080/api/v1/entitlements?status=ORPHAN&page=0&size=50"

# Discover new SGs
curl -u agent:agent-secret -X POST http://localhost:8080/api/v1/entitlements/discover \
  -H "Content-Type: application/json" \
  -d '{"sgNames": ["SG_NEW_TEST_GROUP"]}'
```

### Role Mining
```bash
# Trigger role mining for Finance department
curl -u agent:agent-secret -X POST http://localhost:8080/api/v1/roles/mine \
  -H "Content-Type: application/json" \
  -d '{
    "userSgAssignments": {
      "U001": ["CH_SG_SAP_FI_STG_Read", "CH_SG_SAP_FI_PRD_Read", "SG_PowerBI_Finance_View"],
      "U002": ["CH_SG_SAP_FI_STG_Read", "CH_SG_SAP_FI_PRD_Read", "SG_PowerBI_Finance_View"]
    },
    "userDepartments": {"U001": "Finance", "U002": "Finance"},
    "department": "Finance",
    "minClusterSize": 2,
    "minConfidence": 0.7
  }'
```

### Business Roles
```bash
# List by department
curl -u agent:agent-secret "http://localhost:8080/api/v1/roles/business?department=Finance"

# Get details with resolved entitlement chain
curl -u agent:agent-secret http://localhost:8080/api/v1/roles/business/1

# AI-suggest a Business Role
curl -u agent:agent-secret -X POST http://localhost:8080/api/v1/roles/business/suggest \
  -H "Content-Type: application/json" \
  -d '{
    "jobFunction": "Finance Analyst",
    "department": "Finance",
    "region": "EMEA",
    "userItRoleAssignments": {"U001": [1,2,3], "U002": [1,2]}
  }'
```

### Access Bundles
```bash
# Create bundle from business role
curl -u agent:agent-secret -X POST http://localhost:8080/api/v1/bundles \
  -H "Content-Type: application/json" \
  -d '{"businessRoleId": 2}'

# Get bundle details
curl -u agent:agent-secret http://localhost:8080/api/v1/bundles/1

# Transition lifecycle
curl -u agent:agent-secret -X PATCH http://localhost:8080/api/v1/bundles/1/lifecycle \
  -H "Content-Type: application/json" \
  -d '{"action": "SUBMIT_FOR_REVIEW", "performedBy": "alice@corp.com"}'

# Run KC27 compliance assessment
curl -u agent:agent-secret -X POST http://localhost:8080/api/v1/bundles/1/assess

# Compliance dashboard
curl -u agent:agent-secret http://localhost:8080/api/v1/bundles/compliance/dashboard
```

## Smoke Test

Run a quick check against all major endpoints:

```bash
make smoke-test
```

Or manually:

```bash
bash scripts/smoke-test.sh http://localhost:8080
```

## Troubleshooting

### Port already in use

```bash
# Check what's using port 5432 or 8080
lsof -i :5432
lsof -i :8080

# Change ports in .env
POSTGRES_PORT=5433
AGENT_API_PORT=8081
```

### Flyway migration fails

```bash
# Check Flyway logs
docker logs rolebuilder-flyway

# Reset and re-run
make db-reset
```

### "dummy-key" errors on AI endpoints

The AI agents need real Azure OpenAI credentials. Edit `.env`:

```
AZURE_OPENAI_API_KEY=sk-your-real-key
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com/
```

Non-AI endpoints (CRUD, lifecycle, compliance) work without OpenAI keys.

### Container won't start

```bash
# Check all container statuses
make status

# View logs
docker logs rolebuilder-postgres
docker logs rolebuilder-flyway
docker logs rolebuilder-api

# Nuclear option: tear down and rebuild
make teardown
make setup
```

## Make Targets Reference

```
make help          Show all available targets
make setup         First-time setup (DB + migrations + seed data)
make dev           Start DB + Spring Boot locally
make dev-stop      Stop the local database
make seed          Load sample IAM data
make test          Run unit/integration tests
make build         Build the JAR (skip tests)
make run-docker    Build & run everything in Docker
make status        Check container health
make logs          Tail all container logs
make smoke-test    Quick API endpoint check
make db-up         Start Postgres only
make db-migrate    Run Flyway migrations
make db-psql       Open psql shell
make db-reset      Drop and recreate database
make pgadmin       Start pgAdmin at localhost:5050
make clean         Remove build artifacts
make teardown      Stop all + remove volumes
```
