#!/usr/bin/env bash
# End-to-end check of a deployed environment: real tokens, work order opening with
# customer and vehicle data, prioritized listing, signed webhook and status e-mail.
set -Eeuo pipefail

CONTEXT="${KUBE_CONTEXT:-workshop}"
NAMESPACE="${NAMESPACE:-workshop}"
APP="${APP_URL:-http://localhost:8080}"
KEYCLOAK="${KEYCLOAK_URL:-http://localhost:8081}"
MAILPIT="${MAILPIT_URL:-http://localhost:8025}"
SUMMARY_JSON="${SMOKE_SUMMARY_JSON:-./smoke-summary.json}"
SERVICE_ID="0198a000-0000-7000-8000-000000000201"
SEED_CUSTOMER_WITH_DELIVERED_ORDER="0198a000-0000-7000-8000-000000000002"

STEPS=(
  "Opening port-forwards"
  "Waiting for the services"
  "Getting tokens from Keycloak"
  "Opening a work order with the customer and vehicle data in the body"
  "Issuing the budget"
  "Checking the prioritized listing"
  "Approving through the signed webhook"
  "Checking the new status"
  "Checking the status e-mail"
)
declare -A STEP_STATUS=() STEP_SECS=()
CURRENT_STEP=""
CURRENT_START=0
FAIL_MSG=""
NUMBER=""
SECONDS=0

forwards=()
cleanup() { for pid in "${forwards[@]:-}"; do kill "$pid" 2>/dev/null || true; done; }

close_step() {
  [[ -n "$CURRENT_STEP" ]] || return 0
  STEP_STATUS[$CURRENT_STEP]="$1"
  STEP_SECS[$CURRENT_STEP]=$((SECONDS - CURRENT_START))
  CURRENT_STEP=""
}

step() {
  close_step PASS
  CURRENT_STEP="$1"
  CURRENT_START=$SECONDS
  printf '\n==> %s\n' "$1"
}

fail() { FAIL_MSG="$1"; printf 'FAILED: %s\n' "$1" >&2; exit 1; }

write_summary() {
  local code="$1" overall=PASSED name status secs rows="" icon
  [[ $code -eq 0 ]] || overall=FAILED

  printf '\n== Smoke test summary: %s (%ss)%s ==\n' "$overall" "$SECONDS" "${NUMBER:+ work order $NUMBER}"
  for name in "${STEPS[@]}"; do
    printf '  %-8s %4ss  %s\n' "${STEP_STATUS[$name]}" "${STEP_SECS[$name]}" "$name"
    rows+="$name"$'\t'"${STEP_STATUS[$name]}"$'\t'"${STEP_SECS[$name]}"$'\n'
  done
  [[ -z "$FAIL_MSG" ]] || printf '  failure: %s\n' "$FAIL_MSG"

  if command -v jq >/dev/null; then
    printf '%s' "$rows" | jq -Rn --arg status "$overall" --arg wo "$NUMBER" --arg failure "$FAIL_MSG" --argjson secs "$SECONDS" \
      '{status:$status, workOrder:($wo | if . == "" then null else . end), durationSeconds:$secs, failure:($failure | if . == "" then null else . end),
        steps:[inputs | split("\t") | {name:.[0], status:.[1], seconds:(.[2] | tonumber)}]}' >"$SUMMARY_JSON" || true
  fi

  if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
    {
      [[ $code -eq 0 ]] && echo "## ✅ Smoke test PASSED" || echo "## ❌ Smoke test FAILED"
      echo
      echo "Total time: **${SECONDS}s**${NUMBER:+ · Work order: **$NUMBER**}"
      [[ -z "$FAIL_MSG" ]] || { echo; echo "> **Failure:** $FAIL_MSG"; }
      echo
      echo "| Step | Result | Duration |"
      echo "|---|---|---|"
      for name in "${STEPS[@]}"; do
        case "${STEP_STATUS[$name]}" in PASS) icon="✅ PASS" ;; FAIL) icon="❌ FAIL" ;; *) icon="⏭️ SKIPPED" ;; esac
        echo "| $name | $icon | ${STEP_SECS[$name]}s |"
      done
    } >>"$GITHUB_STEP_SUMMARY"
  fi
}

finish() {
  local code=$?
  trap - EXIT ERR
  set +e
  cleanup
  close_step "$([[ $code -eq 0 ]] && echo PASS || echo FAIL)"
  for name in "${STEPS[@]}"; do
    [[ -n "${STEP_STATUS[$name]:-}" ]] || { STEP_STATUS[$name]=SKIPPED; STEP_SECS[$name]=0; }
  done
  write_summary "$code"
  exit "$code"
}
on_error() { [[ -n "$FAIL_MSG" ]] || FAIL_MSG="unexpected error at line $1"; }
trap finish EXIT
trap 'FAIL_MSG="${FAIL_MSG:-interrupted}"; exit 143' TERM INT
trap 'on_error $LINENO' ERR

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
    exit 0
  fi
  sleep 2
done
fail "no e-mail for $NUMBER reached Mailpit"
