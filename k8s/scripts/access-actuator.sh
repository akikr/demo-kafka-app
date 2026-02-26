#!/usr/bin/env bash
set -euo pipefail

NS="${NS:-default}"
SERVICE="${SERVICE:-demo-kafka-app}"
PORT="${PORT:-8080}"
PATH_SUFFIX="${PATH_SUFFIX:-/app/actuator}"
IMAGE="${IMAGE:-curlimages/curl:8.6.0}"

usage() {
  cat <<USAGE
Usage: $(basename "$0") [-n namespace] [-s service] [-p port] [-e path]

Options:
  -n  Kubernetes namespace (default: default)
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

echo "Namespace: $NS"
echo "URL: $URL"

echo "Running in-cluster curl pod..."
kubectl -n "$NS" run "$TMP_NAME" \
  --rm -i --restart=Never \
  --image "$IMAGE" \
  --command -- curl -fsS "$URL"
echo
