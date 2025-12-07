#!/usr/bin/env bash
set -euo pipefail

DATA_DIR="$(dirname "$0")/../data/raw"

echo "Loading sample CSVs from ${DATA_DIR}"
ls "${DATA_DIR}"/*.csv

echo "TODO: Implement ingestion into Postgres or vector store."
