#!/usr/bin/env bash
# End-to-end check of a deployed environment: real tokens, work order opening with
# customer and vehicle data, prioritized listing, signed webhook and status e-mail.
set -euo pipefail

CONTEXT="${KUBE_CONTEXT:-workshop}"
NAMESPACE="${NAMESPACE:-workshop}"
APP="${APP_URL:-http://localhost:8080}"
KEYCLOAK="${KEYCLOAK_URL:-http://localhost:8081}"
MAILPIT="${MAILPIT_URL:-http://localhost:8025}"
SERVICE_ID="0198a000-0000-7000-8000-000000000201"
SEED_CUSTOMER_WITH_DELIVERED_ORDER="0198a000-0000-7000-8000-000000000002"

forwards=()
cleanup() { for pid in "${forwards[@]:-}"; do kill "$pid" 2>/dev/null || true; done; }
trap cleanup EXIT

step() { printf '\n==> %s\n' "$1"; }
fail() { printf 'FAILED: %s\n' "$1" >&2; exit 1; }

if [[ "${START_PORT_FORWARD:-1}" == "1" ]]; then
  step "Opening port-forwards"
  for mapping in "keycloak 8081:8080" "workshop 8080:8080" "mailpit 8025:8025"; do
    set -- $mapping
    kubectl --context "$CONTEXT" -n "$NAMESPACE" port-forward "svc/$1" "$2" >/dev/null 2>&1 &
    forwards+=("$!")
  done
fi

wait_for() {
  local url="$1" attempts=60
  until curl -fs -o /dev/null "$url"; do
    attempts=$((attempts - 1))
    [[ $attempts -gt 0 ]] || fail "timeout waiting for $url"
    sleep 2
  done
}

step "Waiting for the services"
wait_for "$APP/actuator/health/readiness"
wait_for "$KEYCLOAK/realms/workshop"
wait_for "$MAILPIT/readyz"

token() {
  curl -fs -d grant_type=password -d client_id=workshop-cli -d "username=$1" -d "password=$1" \
    "$KEYCLOAK/realms/workshop/protocol/openid-connect/token" | jq -r .access_token
}

api() {
  local method="$1" path="$2" token="$3" body="${4:-}"
  if [[ -n "$body" ]]; then
    curl -fs -X "$method" -H "Authorization: Bearer $token" -H 'Content-Type: application/json' -d "$body" "$APP$path"
  else
    curl -fs -X "$method" -H "Authorization: Bearer $token" "$APP$path"
  fi
}

random_cpf() {
  local d=() sum r i
  for i in 0 1 2 3 4 5 6 7 8; do d+=($((RANDOM % 10))); done
  sum=0; for i in 0 1 2 3 4 5 6 7 8; do sum=$((sum + d[i] * (10 - i))); done
  r=$(((sum * 10) % 11)); [[ $r -eq 10 ]] && r=0; d+=($r)
  sum=0; for i in 0 1 2 3 4 5 6 7 8 9; do sum=$((sum + d[i] * (11 - i))); done
  r=$(((sum * 10) % 11)); [[ $r -eq 10 ]] && r=0; d+=($r)
  printf '%s' "${d[@]}"
}

random_plate() {
  local letters=ABCDEFGHIJKLMNOPQRSTUVWXYZ
  printf '%s%s%s%d%s%d%d' "${letters:RANDOM%26:1}" "${letters:RANDOM%26:1}" "${letters:RANDOM%26:1}" $((RANDOM % 10)) \
    "${letters:RANDOM%26:1}" $((RANDOM % 10)) $((RANDOM % 10))
}

step "Getting tokens from Keycloak"
ATTENDANT="$(token attendant)"
MECHANIC="$(token mechanic)"
[[ -n "$ATTENDANT" && "$ATTENDANT" != null ]] || fail "no attendant token"

step "Opening a work order with the customer and vehicle data in the body"
DOCUMENT="$(random_cpf)"
PLATE="$(random_plate)"
EMAIL="smoke-$DOCUMENT@example.com"
OPENED="$(api POST /api/v1/work-orders "$ATTENDANT" "$(jq -nc --arg d "$DOCUMENT" --arg p "$PLATE" --arg e "$EMAIL" --arg s "$SERVICE_ID" \
  '{customer:{document:$d,name:"Smoke Test",email:$e},vehicle:{plate:$p,brand:"Fiat",model:"Argo",year:2022},complaint:"smoke test",items:[{type:"SERVICE",referenceId:$s,quantity:1}]}')")"
WORK_ORDER_ID="$(jq -r .id <<<"$OPENED")"
CUSTOMER_ID="$(jq -r .customer.id <<<"$OPENED")"
NUMBER="$(jq -r .number <<<"$OPENED")"
[[ "$(jq -r .status <<<"$OPENED")" == RECEIVED ]] || fail "new work order is not RECEIVED"
echo "opened $NUMBER"

step "Issuing the budget"
api POST "/api/v1/work-orders/$WORK_ORDER_ID/diagnosis/start" "$MECHANIC" >/dev/null
BUDGET="$(api POST "/api/v1/work-orders/$WORK_ORDER_ID/diagnosis/complete" "$MECHANIC")"
BUDGET_ID="$(jq -r '.budgets[-1].id' <<<"$BUDGET")"
[[ "$(jq -r .status <<<"$BUDGET")" == WAITING_APPROVAL ]] || fail "work order is not WAITING_APPROVAL"

step "Checking the prioritized listing"
OWN_QUEUE="$(api GET "/api/v1/work-orders?customerId=$CUSTOMER_ID" "$ATTENDANT")"
jq -e --arg id "$WORK_ORDER_ID" '[.content[].id] | index($id) != null' <<<"$OWN_QUEUE" >/dev/null || fail "new work order missing from the queue"
QUEUE="$(api GET '/api/v1/work-orders?size=100' "$ATTENDANT")"
jq -e 'all(.content[]; .status as $s | ["COMPLETED","DELIVERED","CANCELLED"] | index($s) == null)' <<<"$QUEUE" >/dev/null || fail "finished orders in the default queue"
jq -e '[.content[].status | {IN_EXECUTION:0,WAITING_PARTS:1,WAITING_APPROVAL:2,IN_DIAGNOSIS:3,RECEIVED:4}[.]] as $r | $r == ($r | sort)' <<<"$QUEUE" >/dev/null || fail "queue not ordered by status priority"
HIDDEN="$(api GET "/api/v1/work-orders?customerId=$SEED_CUSTOMER_WITH_DELIVERED_ORDER" "$ATTENDANT")"
[[ "$(jq -r .totalElements <<<"$HIDDEN")" == 0 ]] || fail "a delivered order is in the default queue"
SHOWN="$(api GET "/api/v1/work-orders?customerId=$SEED_CUSTOMER_WITH_DELIVERED_ORDER&includeFinished=true" "$ATTENDANT")"
jq -e '[.content[].status] | index("DELIVERED") != null' <<<"$SHOWN" >/dev/null || fail "includeFinished did not show the delivered order"

step "Approving through the signed webhook"
SECRET="$(kubectl --context "$CONTEXT" -n "$NAMESPACE" get secret budget-webhook -o jsonpath='{.data.BUDGET_WEBHOOK_SECRET}' | base64 -d)"
EVENT_ID="$(cat /proc/sys/kernel/random/uuid)"
PAYLOAD="$(jq -nc --arg e "$EVENT_ID" --arg n "$NUMBER" --arg b "$BUDGET_ID" '{eventId:$e,workOrderNumber:$n,budgetId:$b,decision:"APPROVED"}')"
sign() { printf '%s.%s' "$1" "$2" | openssl dgst -sha256 -hmac "$SECRET" -hex | sed 's/^.* //'; }
TS="$(date +%s)"
webhook() {
  curl -s -o /tmp/webhook.out -w '%{http_code}' -X POST "$APP/api/v1/webhooks/budget-decisions" -H 'Content-Type: application/json' \
    -H "X-Timestamp: $1" -H "X-Signature: sha256=$2" -d "$PAYLOAD"
}
[[ "$(webhook "$TS" "bad$(sign "$TS" "$PAYLOAD")")" == 401 ]] || fail "forged signature was not refused"
[[ "$(webhook "$TS" "$(sign "$TS" "$PAYLOAD")")" == 200 ]] || fail "signed webhook refused"
[[ "$(jq -r .result /tmp/webhook.out)" == APPLIED ]] || fail "webhook not applied"
[[ "$(webhook "$TS" "$(sign "$TS" "$PAYLOAD")")" == 200 && "$(jq -r .result /tmp/webhook.out)" == DUPLICATE ]] || fail "replay was not recognized"

step "Checking the new status"
DETAIL="$(api GET "/api/v1/work-orders/$WORK_ORDER_ID" "$ATTENDANT")"
[[ "$(jq -r .status <<<"$DETAIL")" == IN_EXECUTION ]] || fail "work order is not IN_EXECUTION after the approval"
[[ "$(jq -r '.budgets[0].decision.channel' <<<"$DETAIL")" == EXTERNAL ]] || fail "decision channel is not EXTERNAL"
CUSTOMER_STATUS="$(api GET "/api/v1/work-orders?status=IN_EXECUTION&customerId=$CUSTOMER_ID" "$ATTENDANT")"
jq -e --arg id "$WORK_ORDER_ID" '[.content[].id] | index($id) != null' <<<"$CUSTOMER_STATUS" >/dev/null || fail "status filter does not find the work order"

step "Checking the status e-mail"
for _ in $(seq 1 30); do
  if curl -fs "$MAILPIT/api/v1/messages?limit=200" | jq -e --arg e "$EMAIL" --arg n "$NUMBER" \
    'any(.messages[]; .To[0].Address == $e and (.Subject | contains($n)))' >/dev/null; then
    printf '\nSMOKE TEST PASSED (%s)\n' "$NUMBER"
    exit 0
  fi
  sleep 2
done
fail "no e-mail for $NUMBER reached Mailpit"
