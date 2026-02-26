#!/usr/bin/env bash
set -euo pipefail

NS="${NS:-demo}"
REMOTE="${REMOTE:-origin}"
BRANCH="${BRANCH:-prod}"
FORCE_REDEPLOY=false

usage() {
  cat <<USAGE
Usage: $(basename "$0") [--force|-f]

Options:
  -f, --force   Force redeploy even when no git commit change is detected.
  -h, --help    Show this help message.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -f|--force)
      FORCE_REDEPLOY=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Error: unknown option '$1'" >&2
      usage
      exit 1
      ;;
  esac
done

require_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "Error: required command '$cmd' is not installed." >&2
    exit 1
  fi
}

require_cmd git
require_cmd helm
require_cmd kubectl

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

CURRENT_BRANCH="$(git rev-parse --abbrev-ref HEAD)"
if [[ "$CURRENT_BRANCH" == "HEAD" ]]; then
  echo "Error: detached HEAD detected. Checkout a local branch first." >&2
  exit 1
fi

echo "Namespace: $NS"
echo "Remote: $REMOTE"
echo "Current branch: $CURRENT_BRANCH"
echo "Pull branch: $BRANCH"

# Prevent accidental overwrite of local k8s edits.
if ! git diff --quiet -- k8s || ! git diff --cached --quiet -- k8s; then
  echo "Error: uncommitted changes detected under k8s/. Commit or stash them first." >&2
  exit 1
fi

HEAD_BEFORE="$(git rev-parse HEAD)"
echo "Current local HEAD: $HEAD_BEFORE"

echo "Checking out current branch '$CURRENT_BRANCH'..."
git checkout "$CURRENT_BRANCH"

echo "Pulling latest from '$REMOTE/$BRANCH'..."
git pull --ff-only "$REMOTE" "$BRANCH"

HEAD_AFTER="$(git rev-parse HEAD)"
echo "Local HEAD after pull: $HEAD_AFTER"

DEPLOY_REQUIRED=false
if [[ "$HEAD_BEFORE" != "$HEAD_AFTER" ]]; then
  echo "Commit changed after pull. Updating k8s folder from latest HEAD..."
  git checkout HEAD -- k8s
  DEPLOY_REQUIRED=true
else
  if [[ "$FORCE_REDEPLOY" == "true" ]]; then
    echo "No commit change detected. --force enabled, proceeding with redeploy."
    DEPLOY_REQUIRED=true
  else
    echo "No commit change detected. Skipping Helm redeploy."
  fi
fi

if [[ "$DEPLOY_REQUIRED" == "true" ]]; then
  if ! kubectl get namespace "$NS" >/dev/null 2>&1; then
    echo "Namespace '$NS' not found. Creating it..."
    kubectl create namespace "$NS"
  fi

  echo "Deploying dedicated Traefik ingress controller for actuator on port 8080..."
  helm repo add traefik https://traefik.github.io/charts >/dev/null 2>&1 || true
  helm repo update >/dev/null 2>&1
  echo "Removing existing traefik-actuator pods (if any)..."
  kubectl delete pod -n "$NS" -l app.kubernetes.io/name=traefik,app.kubernetes.io/instance=traefik-actuator-demo --ignore-not-found=true >/dev/null 2>&1 || true
  helm upgrade --install traefik-actuator traefik/traefik -n "$NS" -f ./k8s/traefik-actuator/traefik-actuator-values.yaml --skip-crds

  echo "Deploying Helm releases in namespace '$NS'..."
  helm upgrade --install kafka ./k8s/charts/kafka -n "$NS"
  helm upgrade --install kafka-ui ./k8s/charts/kafka-ui -n "$NS"
  helm upgrade --install otel-lgtm ./k8s/charts/otel-lgtm -n "$NS"
  helm upgrade --install demo-kafka-app ./k8s/charts/demo-kafka-app -f ./k8s/charts/demo-kafka-app/values-prod.yaml -n "$NS"
fi

echo
echo "Helm releases in namespace '$NS':"
helm list -n "$NS"

echo
echo "Pods in namespace '$NS':"
kubectl get pods -n "$NS" -o wide
