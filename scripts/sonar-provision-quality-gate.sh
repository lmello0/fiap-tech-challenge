#!/bin/sh

set -e

GATE_NAME="tech-challenge-quality-gate"
PROJECT_KEY="fiap-techchallenge"

echo "Provisioning Quality Gate '${GATE_NAME}'..."

curl -sf \
  -u "${SONAR_USER}:${SONAR_PASSWORD}" \
  -X POST \
  "${SONAR_HOST_URL}/api/qualitygates/destroy" \
  -d "name=${GATE_NAME}" \
  || true

curl -sf \
  -u "${SONAR_USER}:${SONAR_PASSWORD}" \
  -X POST \
  "${SONAR_HOST_URL}/api/qualitygates/create" \
  -d "name=${GATE_NAME}"

curl -sf \
  -u "${SONAR_USER}:${SONAR_PASSWORD}" \
  -X POST \
  "${SONAR_HOST_URL}/api/qualitygates/create_condition" \
  -d "gateName=${GATE_NAME}" \
  -d "metric=coverage" \
  -d "op=LT" \
  -d "error=90"

echo "Ensuring project '${PROJECT_KEY}' exists so the gate can attach to it..."

curl -sf \
  -u "${SONAR_USER}:${SONAR_PASSWORD}" \
  -X POST \
  "${SONAR_HOST_URL}/api/projects/create" \
  -d "project=${PROJECT_KEY}" \
  -d "name=${PROJECT_KEY}" \
  || true

echo "Removing default Clean-as-You-Code conditions..."

SHOW_RESPONSE=$(curl -sf \
  -u "${SONAR_USER}:${SONAR_PASSWORD}" \
  "${SONAR_HOST_URL}/api/qualitygates/show?name=${GATE_NAME}")

echo "$SHOW_RESPONSE" \
  | grep -o '{"id"[^}]*"metric":"[^"]*"[^}]*}' \
  | while IFS= read -r CONDITION; do
      METRIC=$(echo "$CONDITION" | sed -n 's/.*"metric":"\([^"]*\)".*/\1/p')
      if [ "$METRIC" != "coverage" ]; then
        CONDITION_ID=$(echo "$CONDITION" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')
        curl -sf \
          -u "${SONAR_USER}:${SONAR_PASSWORD}" \
          -X POST \
          "${SONAR_HOST_URL}/api/qualitygates/delete_condition" \
          -d "id=${CONDITION_ID}"
      fi
    done

curl -sf \
  -u "${SONAR_USER}:${SONAR_PASSWORD}" \
  -X POST \
  "${SONAR_HOST_URL}/api/qualitygates/select" \
  -d "gateName=${GATE_NAME}" \
  -d "projectKey=${PROJECT_KEY}"

echo "Quality Gate provisioned."
