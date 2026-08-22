#!/bin/sh
set -e

echo "Waiting for SonarQube..."

until curl -sf \
  -u "${SONAR_USER}:${SONAR_PASSWORD}" \
  "${SONAR_HOST_URL}/api/system/status" \
  | grep -q '"status":"UP"'; do
  sleep 5
done

echo "SonarQube is ready."

echo "Revoking previous demo token..."

curl -sf \
  -u "${SONAR_USER}:${SONAR_PASSWORD}" \
  -X POST \
  "${SONAR_HOST_URL}/api/user_tokens/revoke" \
  -d "name=demo-scanner" \
  || true

echo "Creating demo scanner token..."

RESPONSE=$(curl -sf \
  -u "${SONAR_USER}:${SONAR_PASSWORD}" \
  -X POST \
  "${SONAR_HOST_URL}/api/user_tokens/generate" \
  -d "name=demo-scanner")

TOKEN=$(echo "$RESPONSE" \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

if [ -z "$TOKEN" ]; then
  echo "Failed to create SonarQube token."
  echo "$RESPONSE"
  exit 1
fi

echo "SonarQube token created successfully."

export SONAR_TOKEN="$TOKEN"

echo "======================================"
echo " Running Maven tests + Sonar analysis "
echo "======================================"

mvn \
  -B \
  clean verify \
  org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
  -Dsonar.host.url="${SONAR_HOST_URL}" \
  -Dsonar.token="${SONAR_TOKEN}" \
  -Dsonar.qualitygate.wait=true

echo "======================================"
echo " Sonar analysis completed "
echo "======================================"
