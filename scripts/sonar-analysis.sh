#!/bin/sh
set -e

SCRIPT_DIR="$(dirname "$0")"

sh "${SCRIPT_DIR}/sonar-wait-ready.sh"

echo "Provisioning demo scanner token..."
SONAR_TOKEN=$(sh "${SCRIPT_DIR}/sonar-provision-token.sh")
export SONAR_TOKEN
echo "SonarQube token created successfully."

sh "${SCRIPT_DIR}/sonar-provision-quality-gate.sh"

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
