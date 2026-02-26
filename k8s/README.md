# Kafka, Kafka UI, OTEL LGTM, and Demo App Helm Charts

This folder contains four Helm charts:

- `k8s/charts/kafka`: single-node Kafka (KRaft mode), internal-only service.
- `k8s/charts/kafka-ui`: Kafka UI, exposed through Kubernetes Ingress.
- `k8s/charts/otel-lgtm`: Grafana OTEL LGTM stack, exposed through Kubernetes Ingress.
- `k8s/charts/demo-kafka-app`: Spring demo Kafka app (internal service).

## Prerequisites

- Kubernetes cluster
- Helm 3
- Ingress controller installed in the cluster (for example traefik/nginx Ingress)

---

## Chart: kafka

Path: `k8s/charts/kafka`

### Current behavior

- Deploys `confluentinc/cp-kafka:7.8.0`
- Single replica (`replicaCount: 1`)
- KRaft mode configuration is set through environment variables
- Service type is `ClusterIP` on port `9092`
- No ingress is created for Kafka (not externally exposed)

### Key values

- `image.repository`, `image.tag`, `image.pullPolicy`
- `service.type`, `service.port`, `service.targetPort`
- `containerPorts.plaintext`, `containerPorts.internal`, `containerPorts.controller`
- `env.*` for Kafka broker settings
- `resources`, `nodeSelector`, `tolerations`, `affinity`

## Chart: kafka-ui

Path: `k8s/charts/kafka-ui`

### Current behavior

- Deploys `provectuslabs/kafka-ui:latest`
- Connects to Kafka at `kafka:9092`
- Service type is `ClusterIP`
- Ingress is enabled by default
  - `ingress.className: traefik`
  - Host: `kafka-ui.local`
  - Path: `/`
- Creates secret `kafka-ui-credentials` with:
  - `auth-username` = `admin` (base64 encoded)
  - `auth-password` = `password` (base64 encoded)

### Key values

- `image.repository`, `image.tag`, `image.pullPolicy`
- `service.type`, `service.port`, `service.targetPort`
- `env.KAFKA_CLUSTERS_0_NAME`, `env.KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS`
- `ingress.enabled`, `ingress.className`, `ingress.hosts`, `ingress.annotations`, `ingress.tls`
- `resources`, `nodeSelector`, `tolerations`, `affinity`

---

## Chart: otel-lgtm

Path: `k8s/charts/otel-lgtm`

### Current behavior

- Deploys `grafana/otel-lgtm:0.13.0`
- Service type is `ClusterIP`
- Exposes service ports:
  - `3000` (Grafana UI)
  - `4317` (OTLP gRPC)
  - `4318` (OTLP HTTP)
- Ingress is enabled by default
  - `ingress.className: traefik`
  - Host: `otel-lgtm.local`
  - Path: `/`
- Environment variables:
  - `GF_PATHS_DATA=/data/grafana`
  - `ENABLE_LOGS_ALL=true`

### Key values

- `image.repository`, `image.tag`, `image.pullPolicy`
- `service.type`, `service.port`, `service.targetPort`
- `service.otlpGrpcPort`, `service.otlpHttpPort`
- `env.GF_PATHS_DATA`, `env.ENABLE_LOGS_ALL`
- `ingress.enabled`, `ingress.className`, `ingress.hosts`, `ingress.annotations`, `ingress.tls`
- `resources`, `nodeSelector`, `tolerations`, `affinity`

---

## Chart: demo-kafka-application

- Build and push the image to a remote repository (e.g., GitHub Container registry) for prod deployment

```shell
echo $GH_TOKEN | docker login ghcr.io -u $GH_USER --password-stdin;\
docker buildx build -f Dockerfile \
  --platform linux/amd64,linux/arm64 \
  --label "org.opencontainers.image.source https://github.com/akikr/demo-kafka-app.git" \
  --label "org.opencontainers.image.description='A Demo Spring-Kafka Application'" \
  -t ghcr.io/akikr/demo-kafka-app:v1 --push . ;\
docker logout
```

Path: `k8s/charts/demo-kafka-app`

### Current behavior

- Deploys `demo-app:v1` by default (override to `ghcr.io/akikr/demo-kafka-app:v1` for remote registry image pull)
- Single replica (`replicaCount: 1`)
- Service type is `ClusterIP` on port `8080`
- Installs a `NetworkPolicy` (`demo-kafka-app-internal-only`) to allow ingress on `8080` only from pods in the same namespace
- Environment includes multiline `JAVA_OPTS`, multiline `APP_ARGS`, Kafka producer/consumer settings, and app name
- Liveness endpoint: `/app/actuator/health/liveness`
- Readiness endpoint: `/app/actuator/health/readiness`
- Mounts dumps path to `/var/dumps` (hostPath `/tmp/app/dumps` by default)
- Includes `values-prod.yaml` for production image override (`ghcr.io/akikr/demo-kafka-app:v1`)

### Key values

- `image.repository`, `image.tag`, `image.pullPolicy`
- `service.type`, `service.port`, `service.targetPort`
- `networkPolicy.enabled`, `networkPolicy.ingressPort`
- `kafka.serviceName`, `kafka.servicePort` (used to compose Kafka bootstrap server URL)
- `env.*` (JVM/app args, topics, app name)
- `volume.enabled`, `volume.type`, `volume.hostPath`, `volume.mountPath`
- `liveness.path`, `readiness.path`
- `resources`, `nodeSelector`, `tolerations`, `affinity`

---

## Deploy with Helm

Use a namespace (example: `demo`):

```bash
kubectl create namespace demo
```

Switch to this (example: `demo`) namespace

```bash
kubectl config set-context --current --namespace=demo
```

OR Switch to (`default`) namespace

```bash
kubectl config set-context --current --namespace=default
```

Install/upgrade Kafka:

```bash
helm upgrade --install kafka ./k8s/charts/kafka --namespace demo
```

Install/upgrade Kafka UI:

```bash
helm upgrade --install kafka-ui ./k8s/charts/kafka-ui --namespace demo
```

Install/upgrade OTEL LGTM:

```bash
helm upgrade --install otel-lgtm ./k8s/charts/otel-lgtm --namespace demo
```

Install/upgrade Demo Kafka App:

```bash
helm upgrade --install demo-kafka-app ./k8s/charts/demo-kafka-app -f ./k8s/charts/demo-kafka-app/values-prod.yaml --namespace demo
```

Install all in one go:

```bash
helm upgrade --install kafka ./k8s/charts/kafka -n demo
helm upgrade --install kafka-ui ./k8s/charts/kafka-ui -n demo
helm upgrade --install otel-lgtm ./k8s/charts/otel-lgtm -n demo
helm upgrade --install demo-kafka-app ./k8s/charts/demo-kafka-app -f ./k8s/charts/demo-kafka-app/values-prod.yaml -n demo
```

## Verify deployment

```bash
kubectl get pods,deploy,svc,configmap,ingress -n demo
kubectl get networkpolicy -n demo
kubectl describe ingress kafka-ui -n demo
kubectl describe ingress otel-lgtm -n demo
```

Check Kafka broker readiness logs:

```bash
kubectl logs deploy/kafka -n demo
```

Check Kafka UI logs:

```bash
kubectl logs deploy/kafka-ui -n demo
```

Check OTEL LGTM logs:

```bash
kubectl logs deploy/otel-lgtm -n demo
```

Check Demo Kafka App logs:

```bash
kubectl logs deploy/demo-kafka-app -n demo
```

## Access demo-kafka-app actuator from inside cluster VM

Use the helper script:

```bash
./k8s/scripts/access-actuator.sh
./k8s/scripts/access-actuator.sh -n demo -e /app/actuator/health
./k8s/scripts/access-actuator.sh -n demo -e /app/actuator/health/readiness
```

Equivalent direct `kubectl` command:

```bash
kubectl -n demo run curl --rm -it --restart=Never --image=curlimages/curl:8.6.0 -- curl -sS http://demo-kafka-app:8080/app/actuator/health
```

## Rollback

Check revision history:

```bash
helm history kafka -n demo
helm history kafka-ui -n demo
helm history otel-lgtm -n demo
helm history demo-kafka-app -n demo
```

Rollback to a previous revision (example: revision `1`):

```bash
helm rollback kafka 1 -n demo
helm rollback kafka-ui 1 -n demo
helm rollback otel-lgtm 1 -n demo
helm rollback demo-kafka-app 1 -n demo
```

Rollback and wait until resources are ready:

```bash
helm rollback kafka 1 -n demo --wait --timeout 5m
helm rollback kafka-ui 1 -n demo --wait --timeout 5m
helm rollback otel-lgtm 1 -n demo --wait --timeout 5m
helm rollback demo-kafka-app 1 -n demo --wait --timeout 5m
```

## Uninstall

```bash
helm uninstall kafka-ui -n demo
helm uninstall kafka -n demo
helm uninstall otel-lgtm -n demo
helm uninstall demo-kafka-app -n demo
```

---

### Access Kafka UI in browser

Here, the default ingress host is `kafka-ui.local` [here](charts/kafka-ui/values.yaml)

```yaml
ingress:
  enabled: true
  className: "traefik"
  annotations: {}
  hosts:
    - host: kafka-ui.local
      paths:
        - path: /
          pathType: Prefix
  tls: []
```

Map host to your ingress IP/DNS, then open:

`http://kafka-ui.local`

Add the following entry in `/etc/hosts`:

```text
<INTERNAL-IP> kafka-ui.local
```

where this `<INTERNAL-IP>` is the IP of the one of K8s nodes (usually master node).

To get the nodes details:

```shell
kubectl get nodes -o wide
```

OR (without modifying `/etc/hosts`) access directly:

```shell
curl -H "Host: kafka-ui.local"  http://<INTERNAL-IP>
```

OR via https

```shell
curl --insecure -H "Host: kafka-ui.local" https://<INTERNAL-IP>
```

---

### Access OTEL LGTM (Grafana UI) in browser

Here, the default ingress host is `otel-lgtm.local` [here](charts/otel-lgtm/values.yaml)

```yaml
ingress:
  enabled: true
  className: "traefik"
  annotations: {}
  hosts:
    - host: otel-lgtm.local
      paths:
        - path: /
          pathType: Prefix
  tls: []
```

Map host to your ingress IP/DNS, then open:

`http://otel-lgtm.local`

Add the following entry in `/etc/hosts`:

```text
<INTERNAL-IP> otel-lgtm.local
```

OR (without modifying `/etc/hosts`) access directly:

```shell
curl -H "Host: otel-lgtm.local"  http://<INTERNAL-IP>
```
