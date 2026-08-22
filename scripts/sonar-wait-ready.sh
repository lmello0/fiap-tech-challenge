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
