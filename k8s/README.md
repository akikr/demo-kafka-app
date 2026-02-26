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
- Helm access to install a dedicated Traefik ingress controller for actuator on port `8080`

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
- Ingress is enabled by default
  - `ingress.className: traefik-actuator`
  - Host: `demo-kafka-app.local`
  - Path: `/actuator` (rewritten to `/app/actuator` via Traefik middleware)
  - Access port: `8080` via dedicated Traefik service
- Environment includes multiline `JAVA_OPTS`, multiline `APP_ARGS`, Kafka producer/consumer settings, and app name
- Liveness endpoint: `/app/actuator/health/liveness`
- Readiness endpoint: `/app/actuator/health/readiness`
- Mounts dumps path to `/var/dumps` (hostPath `/tmp/app/dumps` by default)
- Includes `values-prod.yaml` for production image override (`ghcr.io/akikr/demo-kafka-app:v1`)

### Key values

- `image.repository`, `image.tag`, `image.pullPolicy`
- `service.type`, `service.port`, `service.targetPort`
- `ingress.enabled`, `ingress.className`, `ingress.host`, `ingress.path`, `ingress.annotations`, `ingress.tls`
- `ingress.rewrite.enabled`, `ingress.rewrite.from`, `ingress.rewrite.to`, `ingress.rewrite.traefikApiVersion`
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

OR use environment variable for namespace

```bash
export NS=demo
```

Install/upgrade Kafka:

```bash
helm upgrade --install kafka ./k8s/charts/kafka --namespace $NS
```

Install/upgrade Kafka UI:

```bash
helm upgrade --install kafka-ui ./k8s/charts/kafka-ui --namespace $NS
```

Install/upgrade OTEL LGTM:

```bash
helm upgrade --install otel-lgtm ./k8s/charts/otel-lgtm --namespace $NS
```

Install/upgrade Demo Kafka App:

```bash
helm upgrade --install demo-kafka-app ./k8s/charts/demo-kafka-app -f ./k8s/charts/demo-kafka-app/values-prod.yaml --namespace $NS
```

Install/upgrade dedicated Traefik ingress controller for actuator on port `8080`:

```bash
helm repo add traefik https://traefik.github.io/charts
helm repo update
helm upgrade --install traefik-actuator traefik/traefik -n $NS -f ./k8s/traefik-actuator/traefik-actuator-values.yaml
```

Install all in one go:

```bash
helm upgrade --install kafka ./k8s/charts/kafka -n $NS
helm upgrade --install kafka-ui ./k8s/charts/kafka-ui -n $NS
helm upgrade --install otel-lgtm ./k8s/charts/otel-lgtm -n $NS
helm repo add traefik https://traefik.github.io/charts
helm repo update
helm upgrade --install traefik-actuator traefik/traefik -n $NS -f ./k8s/traefik-actuator/traefik-actuator-values.yaml
helm upgrade --install demo-kafka-app ./k8s/charts/demo-kafka-app -f ./k8s/charts/demo-kafka-app/values-prod.yaml -n $NS
```

## Verify deployment

```bash
kubectl get pods,deploy,svc,configmap,ingress -n $NS
kubectl describe ingress kafka-ui -n $NS
kubectl describe ingress otel-lgtm -n $NS
kubectl describe ingress demo-kafka-app -n $NS
kubectl get svc traefik-actuator -n $NS
```

Check Kafka broker readiness logs:

```bash
kubectl logs deploy/kafka -n $NS
```

Check Kafka UI logs:

```bash
kubectl logs deploy/kafka-ui -n $NS
```

Check OTEL LGTM logs:

```bash
kubectl logs deploy/otel-lgtm -n $NS
```

Check Demo Kafka App logs:

```bash
kubectl logs deploy/demo-kafka-app -n $NS
```

## Access demo-kafka-app actuator from host VM (via Ingress)

Deploy/upgrade demo-kafka-app chart:

```bash
helm repo add traefik https://traefik.github.io/charts
helm repo update
helm upgrade --install traefik-actuator traefik/traefik -n $NS -f ./k8s/traefik-actuator/traefik-actuator-values.yaml
helm upgrade --install demo-kafka-app ./k8s/charts/demo-kafka-app -f ./k8s/charts/demo-kafka-app/values-prod.yaml -n $NS
```

Verify ingress:

```bash
kubectl get ingress demo-kafka-app -n $NS
kubectl describe ingress demo-kafka-app -n $NS
kubectl get svc traefik-actuator -n $NS
```

Get cluster node IP (use one reachable from host VM):

```bash
kubectl get nodes -o wide
```

Add host mapping in `/etc/hosts` on host VM:

```text
<INTERNAL-IP> demo-kafka-app.local
```

Then access:

```bash
curl http://demo-kafka-app.local:8080/actuator
curl http://demo-kafka-app.local:8080/actuator/health
curl http://demo-kafka-app.local:8080/actuator/info
```

Or without modifying `/etc/hosts`:

```bash
curl -H "Host: demo-kafka-app.local" http://<INTERNAL-IP>:8080/actuator/health
curl -H "Host: demo-kafka-app.local" http://<INTERNAL-IP>:8080/actuator/info
```

## Rollback

Check revision history:

```bash
helm history kafka -n $NS
helm history kafka-ui -n $NS
helm history otel-lgtm -n $NS
helm history demo-kafka-app -n $NS
```

Rollback to a previous revision (example: revision `1`):

```bash
helm rollback kafka 1 -n $NS
helm rollback kafka-ui 1 -n $NS
helm rollback otel-lgtm 1 -n $NS
helm rollback demo-kafka-app 1 -n $NS
```

Rollback and wait until resources are ready:

```bash
helm rollback kafka 1 -n $NS --wait --timeout 5m
helm rollback kafka-ui 1 -n $NS --wait --timeout 5m
helm rollback otel-lgtm 1 -n $NS --wait --timeout 5m
helm rollback demo-kafka-app 1 -n $NS --wait --timeout 5m
```

## Uninstall

```bash
helm uninstall kafka-ui -n $NS
helm uninstall kafka -n $NS
helm uninstall otel-lgtm -n $NS
helm uninstall demo-kafka-app -n $NS
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
