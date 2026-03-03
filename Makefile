# =============================================================================
# AccessForge — From AD Chaos to Governed Access in Days
# =============================================================================
# Quick reference:
#   make setup        — first-time setup (copy .env, start DB, migrate, seed)
#   make dev          — backend API + DB locally (for backend dev)
#   make dev-ui       — frontend dev server with hot-reload (for UI dev)
#   make run          — full-stack Docker: API + UI + DB + migrations
#   make seed         — load sample IAM data
#   make status       — check container health
#   make smoke-test   — quick API endpoint check
#   make teardown     — stop everything, remove volumes
# =============================================================================

SHELL := /bin/bash
COMPOSE := docker compose -f infra/local/docker-compose.yml
MVN := ./mvnw -B 2>/dev/null || mvn -B

.PHONY: help setup dev dev-ui dev-stop seed test build run run-docker \
        status logs teardown db-up db-migrate db-psql db-reset pgadmin \
        smoke-test clean deploy-check

# ---------------------------------------------------------------------------
# Default
# ---------------------------------------------------------------------------
help: ## Show this help
	@echo ""
	@echo "  \033[1;36mAccessForge\033[0m — Intelligent Role Governance Platform"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'
	@echo ""

# ---------------------------------------------------------------------------
# First-time setup
# ---------------------------------------------------------------------------
setup: ## First-time setup: env file, DB, migrations, seed data
	@echo ""
	@echo "  ╔═══════════════════════════════════════════════════╗"
	@echo "  ║       AccessForge — Initial Setup                 ║"
	@echo "  ╚═══════════════════════════════════════════════════╝"
	@echo ""
	@echo "==> Step 1/4: Environment file..."
	@test -f .env || (cp .env.example .env && echo "    Created .env from .env.example")
	@echo "==> Step 2/4: Starting Postgres..."
	@$(COMPOSE) up -d postgres
	@echo "==> Step 3/4: Running database migrations..."
	@$(COMPOSE) up flyway
	@echo "==> Step 4/4: Loading sample IAM data..."
	@bash scripts/load-sample-data.sh
	@echo ""
	@echo "  ╔═══════════════════════════════════════════════════╗"
	@echo "  ║  Setup complete!                                  ║"
	@echo "  ║                                                   ║"
	@echo "  ║  Next steps:                                      ║"
	@echo "  ║  1. Edit .env with your connector credentials     ║"
	@echo "  ║  2. make run  (full-stack Docker)                 ║"
	@echo "  ║     — or —                                        ║"
	@echo "  ║  2. make dev  (backend only, local dev)           ║"
	@echo "  ╚═══════════════════════════════════════════════════╝"
	@echo ""

# ---------------------------------------------------------------------------
# Local development
# ---------------------------------------------------------------------------
dev: db-up db-migrate ## Start DB + run Spring Boot API locally
	@echo "==> Starting AccessForge API (Ctrl+C to stop)..."
	$(MVN) -pl backend/agent-api spring-boot:run \
		-Dspring-boot.run.profiles=dev \
		-Dspring-boot.run.jvmArguments="-Dspring.flyway.enabled=false"

dev-ui: ## Start React frontend dev server (hot-reload on port 3000)
	@cd frontend && npm install --legacy-peer-deps && npm run dev

dev-stop: ## Stop local database
	@$(COMPOSE) stop postgres

# ---------------------------------------------------------------------------
# Full-stack Docker deployment
# ---------------------------------------------------------------------------
run: ## Full-stack: DB + migrations + API + Frontend in Docker
	@test -f .env || (cp .env.example .env && echo "Created .env — edit connector credentials for your environment")
	$(COMPOSE) --profile app up --build -d
	@echo ""
	@echo "  ╔═══════════════════════════════════════════════════╗"
	@echo "  ║  AccessForge is starting...                       ║"
	@echo "  ║                                                   ║"
	@echo "  ║  Dashboard:  http://localhost:$${FRONTEND_PORT:-3000}            ║"
	@echo "  ║  API:        http://localhost:$${AGENT_API_PORT:-8080}            ║"
	@echo "  ║  Health:     http://localhost:$${AGENT_API_PORT:-8080}/actuator/health ║"
	@echo "  ║                                                   ║"
	@echo "  ║  Login:  agent / agent-secret                     ║"
	@echo "  ║  Status: make status                              ║"
	@echo "  ╚═══════════════════════════════════════════════════╝"
	@echo ""

run-docker: run ## Alias for 'make run'

# ---------------------------------------------------------------------------
# Database
# ---------------------------------------------------------------------------
db-up: ## Start Postgres in Docker
	@$(COMPOSE) up -d postgres
	@echo "==> Postgres on localhost:$${POSTGRES_PORT:-5432}"

db-migrate: ## Run Flyway migrations
	@$(COMPOSE) up flyway

db-psql: ## Open psql shell
	@docker exec -it rolebuilder-postgres psql -U $${POSTGRES_USER:-agent} -d $${POSTGRES_DB:-agentdb}

db-reset: ## Drop and recreate database (destructive!)
	@echo "WARNING: This destroys all data. Press Ctrl+C to cancel..."
	@sleep 3
	@docker exec rolebuilder-postgres psql -U $${POSTGRES_USER:-agent} -d postgres \
		-c "DROP DATABASE IF EXISTS $${POSTGRES_DB:-agentdb};" \
		-c "CREATE DATABASE $${POSTGRES_DB:-agentdb} OWNER $${POSTGRES_USER:-agent};"
	@$(COMPOSE) up flyway
	@echo "==> Database reset. Run 'make seed' to reload sample data."

# ---------------------------------------------------------------------------
# Data
# ---------------------------------------------------------------------------
seed: ## Load sample IAM data (SGs, users, roles, bundles)
	@bash scripts/load-sample-data.sh

# ---------------------------------------------------------------------------
# Build & Test
# ---------------------------------------------------------------------------
test: ## Run unit and integration tests
	$(MVN) -pl backend/agent-api verify

build: ## Build backend JAR (skip tests)
	$(MVN) -pl backend/agent-api -am package -DskipTests

# ---------------------------------------------------------------------------
# Tools
# ---------------------------------------------------------------------------
pgadmin: ## Start pgAdmin at http://localhost:5050
	$(COMPOSE) --profile tools up -d pgadmin
	@echo "==> pgAdmin: http://localhost:5050 (admin@local.dev / admin)"

# ---------------------------------------------------------------------------
# Operations
# ---------------------------------------------------------------------------
status: ## Show container health and service status
	@echo ""
	@echo "  AccessForge Service Status"
	@echo "  ─────────────────────────────────────"
	@docker ps --filter "name=accessforge-" --filter "name=rolebuilder-" \
		--format "  {{.Names}}\t{{.Status}}" 2>/dev/null || echo "  No containers running"
	@echo ""
	@echo "  API Health:"
	@curl -sf http://localhost:$${AGENT_API_PORT:-8080}/actuator/health 2>/dev/null \
		| python3 -m json.tool 2>/dev/null \
		|| echo "  API not reachable"
	@echo ""

logs: ## Tail all container logs
	$(COMPOSE) --profile app logs -f

smoke-test: ## Quick API endpoint verification
	@bash scripts/smoke-test.sh

deploy-check: ## Verify deployment readiness (pre-client-deploy)
	@echo "==> Deployment Readiness Check"
	@echo ""
	@echo "  1. Docker containers..."
	@docker ps --filter "name=accessforge-" --format "     OK: {{.Names}} — {{.Status}}" 2>/dev/null
	@echo ""
	@echo "  2. API health..."
	@curl -sf http://localhost:$${AGENT_API_PORT:-8080}/actuator/health >/dev/null 2>&1 \
		&& echo "     OK: API responding" || echo "     FAIL: API not reachable"
	@echo ""
	@echo "  3. Frontend..."
	@curl -sf http://localhost:$${FRONTEND_PORT:-3000}/ >/dev/null 2>&1 \
		&& echo "     OK: Frontend serving" || echo "     FAIL: Frontend not reachable"
	@echo ""
	@echo "  4. Database..."
	@docker exec rolebuilder-postgres pg_isready -U $${POSTGRES_USER:-agent} >/dev/null 2>&1 \
		&& echo "     OK: Postgres healthy" || echo "     FAIL: Postgres not ready"
	@echo ""
	@echo "  5. Integration connectors..."
	@curl -sf -u $${AGENT_API_USERNAME:-agent}:$${AGENT_API_PASSWORD:-agent-secret} \
		http://localhost:$${AGENT_API_PORT:-8080}/api/v1/integrations/status 2>/dev/null \
		| python3 -m json.tool 2>/dev/null \
		|| echo "     Integration status unavailable"
	@echo ""

# ---------------------------------------------------------------------------
# Cleanup
# ---------------------------------------------------------------------------
clean: ## Remove build artifacts
	$(MVN) clean

teardown: ## Stop all containers and remove volumes (destructive!)
	@echo "WARNING: Removes all containers and data volumes. Ctrl+C to cancel..."
	@sleep 3
	$(COMPOSE) --profile app --profile tools down -v
	@echo "==> All containers and volumes removed."
