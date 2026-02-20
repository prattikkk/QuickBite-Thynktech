#!/bin/bash

# QuickBite Webhook Simulator
# Generates HMAC-SHA256 signature and sends webhook to backend
# Usage: ./send_webhook.sh <payload_file> [endpoint_url]

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Configuration
WEBHOOK_SECRET=${PAYMENT_WEBHOOK_SECRET:-"whsec_test_placeholder_secret_2026"}
BASE_URL=${API_BASE_URL:-"http://localhost:8080"}
ENDPOINT=${2:-"${BASE_URL}/api/payments/webhook"}

# Check arguments
if [ -z "$1" ]; then
    echo -e "${RED}Usage: $0 <payload_file> [endpoint_url]${NC}"
    echo "Example: $0 webhook_payload.json"
    echo "         $0 webhook_payload.json http://localhost:8080/api/payments/webhook"
    exit 1
fi

PAYLOAD_FILE=$1

if [ ! -f "$PAYLOAD_FILE" ]; then
    echo -e "${RED}Error: Payload file not found: ${PAYLOAD_FILE}${NC}"
    exit 1
fi

echo -e "${GREEN}=== QuickBite Webhook Simulator ===${NC}"
echo "Payload file: ${PAYLOAD_FILE}"
echo "Endpoint: ${ENDPOINT}"
echo "Secret: ${WEBHOOK_SECRET:0:10}..."

# Read payload
PAYLOAD=$(cat "${PAYLOAD_FILE}")
echo -e "\n${YELLOW}Payload:${NC}"
echo "${PAYLOAD}" | jq . 2>/dev/null || echo "${PAYLOAD}"

# Generate HMAC-SHA256 signature (hex)
SIGNATURE_HEX=$(echo -n "${PAYLOAD}" | openssl dgst -sha256 -hmac "${WEBHOOK_SECRET}" -binary | xxd -p -c 256)
echo -e "\n${YELLOW}Signature (hex):${NC} ${SIGNATURE_HEX}"

# Generate HMAC-SHA256 signature (base64)
SIGNATURE_BASE64=$(echo -n "${PAYLOAD}" | openssl dgst -sha256 -hmac "${WEBHOOK_SECRET}" -binary | base64)
echo -e "${YELLOW}Signature (base64):${NC} ${SIGNATURE_BASE64}"

# Generate timestamp
TIMESTAMP=$(date +%s)
echo -e "${YELLOW}Timestamp:${NC} ${TIMESTAMP}"

# Construct signature header (format: t=timestamp,v1=signature_hex)
SIGNATURE_HEADER="t=${TIMESTAMP},v1=${SIGNATURE_HEX}"
echo -e "${YELLOW}X-Webhook-Signature:${NC} ${SIGNATURE_HEADER}"

# Send webhook
echo -e "\n${GREEN}Sending webhook...${NC}"
HTTP_CODE=$(curl -s -o /tmp/webhook_response.json -w "%{http_code}" \
    -X POST \
    -H "Content-Type: application/json" \
    -H "X-Webhook-Signature: ${SIGNATURE_HEADER}" \
    -d "${PAYLOAD}" \
    "${ENDPOINT}")

echo -e "\n${YELLOW}HTTP Status:${NC} ${HTTP_CODE}"

if [ -f /tmp/webhook_response.json ]; then
    echo -e "${YELLOW}Response:${NC}"
    cat /tmp/webhook_response.json | jq . 2>/dev/null || cat /tmp/webhook_response.json
    rm /tmp/webhook_response.json
fi

echo ""
if [ "$HTTP_CODE" -ge 200 ] && [ "$HTTP_CODE" -lt 300 ]; then
    echo -e "${GREEN}✓ Webhook sent successfully!${NC}"
    exit 0
else
    echo -e "${RED}✗ Webhook failed with HTTP ${HTTP_CODE}${NC}"
    exit 1
fi
