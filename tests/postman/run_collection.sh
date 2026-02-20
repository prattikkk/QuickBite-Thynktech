#!/bin/bash

# QuickBite E2E Postman Collection Runner
# Runs the Postman collection using Newman CLI

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== QuickBite E2E Postman Tests ===${NC}"

# Check if newman is installed
if ! command -v newman &> /dev/null; then
    echo -e "${YELLOW}Newman not found. Installing...${NC}"
    npm install -g newman newman-reporter-htmlextra
fi

# Set base URL (can be overridden by environment variable)
BASE_URL=${API_BASE_URL:-"http://localhost:8080"}

echo -e "${GREEN}Testing against: ${BASE_URL}${NC}"

# Check if backend is up
echo -e "${YELLOW}Waiting for backend to be ready...${NC}"
max_attempts=30
attempt=0
while [ $attempt -lt $max_attempts ]; do
    if curl -s "${BASE_URL}/actuator/health" > /dev/null 2>&1; then
        echo -e "${GREEN}Backend is ready!${NC}"
        break
    fi
    attempt=$((attempt + 1))
    echo "Attempt $attempt/$max_attempts..."
    sleep 2
done

if [ $attempt -eq $max_attempts ]; then
    echo -e "${RED}Backend not available at ${BASE_URL}${NC}"
    exit 1
fi

# Run the collection
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COLLECTION_FILE="${SCRIPT_DIR}/quickbite-e2e.postman_collection.json"
REPORTS_DIR="${SCRIPT_DIR}/../../reports/newman"

mkdir -p "${REPORTS_DIR}"

echo -e "${GREEN}Running Newman collection...${NC}"

newman run "${COLLECTION_FILE}" \
    --env-var "baseUrl=${BASE_URL}" \
    --reporters cli,htmlextra \
    --reporter-htmlextra-export "${REPORTS_DIR}/report.html" \
    --reporter-htmlextra-darkTheme \
    --timeout-request 10000 \
    --bail \
    --color on

exit_code=$?

if [ $exit_code -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed!${NC}"
    echo -e "Report: ${REPORTS_DIR}/report.html"
else
    echo -e "${RED}✗ Tests failed with exit code: ${exit_code}${NC}"
    echo -e "Report: ${REPORTS_DIR}/report.html"
fi

exit $exit_code
