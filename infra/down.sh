#!/usr/bin/env bash
# Destroys everything created by up.sh, in reverse order.
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

for stage in 03-platform 02-database 01-foundation; do
  printf '\n==> terraform destroy %s\n' "$stage"
  terraform -chdir="$ROOT/infra/$stage" init -input=false >/dev/null
  terraform -chdir="$ROOT/infra/$stage" destroy -input=false -auto-approve || true
done

docker ps -aq --filter name=ministack-rds | xargs -r docker rm -f >/dev/null
