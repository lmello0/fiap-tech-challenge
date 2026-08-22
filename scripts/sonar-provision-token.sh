#!/bin/sh

set -e

curl -sf \
  -u "${SONAR_USER}:${SONAR_PASSWORD}" \
  -X POST \
  "${SONAR_HOST_URL}/api/user_tokens/revoke" \
  -d "name=demo-scanner" \
  >&2 \
  || true

RESPONSE=$(curl -sf \
  -u "${SONAR_USER}:${SONAR_PASSWORD}" \
  -X POST \
  "${SONAR_HOST_URL}/api/user_tokens/generate" \
  -d "name=demo-scanner")

TOKEN=$(echo "$RESPONSE" \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

if [ -z "$TOKEN" ]; then
  echo "Failed to create SonarQube token." >&2
  echo "$RESPONSE" >&2
  exit 1
fi

echo "$TOKEN"
