#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Local development runner
# Starts Postgres + Flyway in Docker, then runs Spring Boot on the host.
# ---------------------------------------------------------------------------
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE="docker compose -f ${ROOT}/infra/local/docker-compose.yml"

# --- Pre-flight checks ---
if ! command -v docker &>/dev/null; then
  echo "[ERROR] Docker is not installed. Install Docker Desktop or Docker Engine."
  exit 1
fi

if ! command -v mvn &>/dev/null && [ ! -f "${ROOT}/mvnw" ]; then
  echo "[ERROR] Maven is not installed and no mvnw wrapper found."
  exit 1
fi

MVN="mvn"
[ -f "${ROOT}/mvnw" ] && MVN="${ROOT}/mvnw"

# --- .env file ---
if [ ! -f "${ROOT}/.env" ]; then
  echo "[dev-run] No .env file found. Copying from .env.example..."
  cp "${ROOT}/.env.example" "${ROOT}/.env"
  echo "[dev-run] Created .env — edit AZURE_OPENAI_API_KEY for AI features."
fi

# shellcheck disable=SC1091
source "${ROOT}/.env"

# --- Start infrastructure ---
echo "[dev-run] Starting Postgres..."
${COMPOSE} up -d postgres

echo "[dev-run] Waiting for Postgres to be healthy..."
for i in $(seq 1 30); do
  if ${COMPOSE} exec -T postgres pg_isready -U "${POSTGRES_USER:-agent}" -d "${POSTGRES_DB:-agentdb}" &>/dev/null; then
    echo "[dev-run] Postgres is ready."
    break
  fi
  if [ "$i" -eq 30 ]; then
    echo "[ERROR] Postgres did not become healthy in 30 seconds."
    exit 1
  fi
  sleep 1
done

# --- Run Flyway migrations ---
echo "[dev-run] Running Flyway migrations..."
${COMPOSE} up flyway

# --- Start Spring Boot ---
echo ""
echo "============================================================"
echo "  Starting RoleBuilderAgent API on http://localhost:${AGENT_API_PORT:-8080}"
echo "  Press Ctrl+C to stop."
echo "============================================================"
echo ""

cd "${ROOT}"
${MVN} -B -pl backend/agent-api spring-boot:run \
  -Dspring-boot.run.profiles=dev \
  -Dspring-boot.run.jvmArguments="-Dspring.flyway.enabled=false"
