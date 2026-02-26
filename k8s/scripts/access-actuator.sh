#!/usr/bin/env bash
set -euo pipefail

NS="${NS:-demo}"
SERVICE="${SERVICE:-demo-kafka-app}"
PORT="${PORT:-8080}"
PATH_SUFFIX="${PATH_SUFFIX:-/app/actuator}"
IMAGE="${IMAGE:-curlimages/curl:8.6.0}"

usage() {
  cat <<USAGE
Usage: $(basename "$0") [-n namespace] [-s service] [-p port] [-e path]

Options:
  -n  Kubernetes namespace (default: demo)
  -s  Service name (default: demo-kafka-app)
  -p  Service port (default: 8080)
  -e  Endpoint path (default: /app/actuator)

Examples:
  $(basename "$0")
  $(basename "$0") -n default -e /app/actuator/health
  NS=prod SERVICE=demo-kafka-app $(basename "$0") -e /app/actuator/info
USAGE
}

while getopts ":n:s:p:e:h" opt; do
  case "$opt" in
    n) NS="$OPTARG" ;;
    s) SERVICE="$OPTARG" ;;
    p) PORT="$OPTARG" ;;
    e) PATH_SUFFIX="$OPTARG" ;;
    h)
      usage
      exit 0
      ;;
    :)
      echo "Error: option -$OPTARG requires an argument." >&2
      usage
      exit 1
      ;;
    \?)
      echo "Error: invalid option -$OPTARG" >&2
      usage
      exit 1
      ;;
  esac
done

if ! command -v kubectl >/dev/null 2>&1; then
  echo "Error: kubectl not found in PATH." >&2
  exit 1
fi

if [[ "$PATH_SUFFIX" != /* ]]; then
  PATH_SUFFIX="/$PATH_SUFFIX"
fi

URL="http://${SERVICE}:${PORT}${PATH_SUFFIX}"
TMP_NAME="curl-actuator-$(date +%s)"
ENDPOINT_IP="$(kubectl -n "$NS" get endpoints "$SERVICE" -o jsonpath='{.subsets[0].addresses[0].ip}' 2>/dev/null || true)"

echo "Namespace: $NS"
echo "URL: $URL"

if ! kubectl -n "$NS" get svc "$SERVICE" >/dev/null 2>&1; then
  echo "Error: service '$SERVICE' not found in namespace '$NS'." >&2
  exit 1
fi

if [[ -z "$ENDPOINT_IP" ]]; then
  echo "Error: service '$SERVICE' has no endpoints in namespace '$NS'." >&2
  echo "Hint: check labels/selectors with:" >&2
  echo "  kubectl -n $NS get svc $SERVICE -o yaml" >&2
  echo "  kubectl -n $NS get pods --show-labels" >&2
  exit 1
fi

echo "Running in-cluster curl pod (service URL)..."
if kubectl -n "$NS" run "$TMP_NAME" \
  --rm -i --restart=Never \
  --image "$IMAGE" \
  --command -- curl -fsS "$URL"; then
  echo
  exit 0
fi

echo "Service access failed. Retrying via pod endpoint IP: ${ENDPOINT_IP}:${PORT}${PATH_SUFFIX}"
TMP_NAME_FALLBACK="curl-actuator-fallback-$(date +%s)"
kubectl -n "$NS" run "$TMP_NAME_FALLBACK" \
  --rm -i --restart=Never \
  --image "$IMAGE" \
  --command -- curl -fsS "http://${ENDPOINT_IP}:${PORT}${PATH_SUFFIX}"
echo
