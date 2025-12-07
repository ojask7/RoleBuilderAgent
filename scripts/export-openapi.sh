#!/usr/bin/env bash
set -euo pipefail

OUTPUT=${1:-build/openapi.json}

mvn -pl backend/agent-api springdoc-openapi:generate -Dspringdoc.outputDir=$(dirname "$OUTPUT") -Dspringdoc.outputFile=$(basename "$OUTPUT")
