#!/usr/bin/env bash
# Creates the whole environment: MiniStack + minikube, the RDS database, the platform secrets,
# the application image and the Kubernetes manifests.
#
# Environment: CLUSTER_CPUS (4), CLUSTER_MEMORY (8192mb), IMAGE_TAR (use a saved image instead of building),
#              SKIP_BUILD=1 (reuse workshop:local).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLUSTER="${CLUSTER_NAME:-workshop}"
IMAGE="workshop:local"

stage() {
  local dir="$ROOT/infra/$1"
  shift
  printf '\n==> terraform apply %s\n' "$(basename "$dir")"
  terraform -chdir="$dir" init -input=false >/dev/null
  terraform -chdir="$dir" apply -input=false -auto-approve "$@"
}

stage 01-foundation -var "cluster_name=$CLUSTER" -var "cluster_cpus=${CLUSTER_CPUS:-4}" -var "cluster_memory=${CLUSTER_MEMORY:-8192mb}"
stage 02-database
stage 03-platform -var "kube_context=$CLUSTER"

printf '\n==> application image\n'
if [[ -n "${IMAGE_TAR:-}" ]]; then
  docker load -i "$IMAGE_TAR"
elif [[ "${SKIP_BUILD:-0}" != "1" ]]; then
  docker build -t "$IMAGE" "$ROOT"
fi
minikube -p "$CLUSTER" image load "$IMAGE"

printf '\n==> kubernetes manifests\n'
kubectl --context "$CLUSTER" apply -k "$ROOT/k8s/overlays/minikube"
for deployment in mailpit keycloak workshop; do
  kubectl --context "$CLUSTER" -n workshop rollout status "deployment/$deployment" --timeout=600s
done

cat <<MSG

Environment ready. Reach it with:
  kubectl --context $CLUSTER -n workshop port-forward svc/workshop 8080:8080
  kubectl --context $CLUSTER -n workshop port-forward svc/keycloak 8081:8080
  kubectl --context $CLUSTER -n workshop port-forward svc/mailpit 8025:8025
Check it with scripts/smoke-test.sh and load it with: kubectl --context $CLUSTER apply -k k8s/load
MSG
