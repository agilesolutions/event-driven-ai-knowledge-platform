# From Events to Intelligence: AI-Powered Notebook Architecture
What if every notebook could become searchable knowledge?

This project demonstrates a **cloud-native, event-driven architecture** built with Spring Boot 4, Redpanda (Kafka), and Spring AI. 
When a notebook is created, independent consumer services handle notifications and auditing, while the **Notebook Intelligence Service** transforms its content into enriched knowledge using AI-generated summaries, topics, keywords, and entities.

Enriched documents are persisted in MongoDB and indexed in Qdrant, laying the foundation for **semantic search and intelligent knowledge discovery**. With Terraform-managed infrastructure and a modular microservices design, the project explores how modern Java applications can evolve from simple CRUD services into AI-enabled platforms.

**One event. Multiple consumers. From notebook creation to intelligent discovery.**

<img title="Architecture" alt="Alt text" src="/docs/use-case-v2.png" width="800">

## Terraform provision LGTM grafana stack, PostgreSQL database, traefik ingress controller

```
cd terraform/environment/grafana

terraform init

terraform validate

terraform plan

terraform apply -auto-approve
```

## Build, bootstrap FluxCD and GitOps deploy notebook application

```
gradlew jibDockerBuild

flux bootstrap github --owner=agilesolutions --repository=AI-agentic-migration-overheid --branch=master --path=./fluxcd --components-extra=image-reflector-controller,image-automation-controller --personal
```

## Run and Test

```
kubectl port-forward svc/notebook 8080:8080 -n services


http://localhost:8080/swagger-ui.html
```

## Observe MongoDB document changes with MongoDB Compass

``` 
kubectl get pods -n database

kubectl port-forward svc/mongodb 27017:27017 -n database

# connect to MongoDB Compass with the following connection string:
mongodb://localhost:27017

```

## Grafana observe

```
kubectl port-forward svc/kube-prometheus-stack-grafana  3000:80 -n monitoring


http://localhost:3000

```

## Grafana alerts
The Loki query for the alert is:

```
sum(
count_over_time(
{namespace="notebook"} |= "ERROR" [5m]
)
) > 0

```
This means that if there are any log entries in the last 5 minutes that contain the string "ERROR" in the "notebook" namespace, the alert will fire.

```
kubectl port-forward svc/grafana-webhook 8080:8080 -n monitoring

kubectl logs -n monitoring deployment/grafana-webhook -f
```
The logs will show the alerts received from Grafana and the actions taken by the webhook.

You will see something like this in the logs:

```
{
  "receiver": "local-webhook",
  "status": "firing",
  "alerts": [
    {
      "status": "firing",
      "labels": {
        "alertname": "NotebookErrorLogs",
        "namespace": "notebook"
      },
      "annotations": {
        "summary": "Notebook application error"
      }
    }
  ]
}
```

## Enabling Grafana Service Graph
Tempo is a distributed tracing backend that can be used to visualize service graphs in Grafana. To enable service graphs, you need to configure Tempo to receive traces from your application and then configure Grafana to display the service graph.

```
                    OTLP
Notebook ─────────────────────► Tempo
   │                              │
   │                              │ service-graphs
   │                              ▼
   │                         Metrics Generator
   │                              │
   │                              │ remote_write
   │                              ▼
   │                         Prometheus
   │                              │
   │                              ▼
   └──────────────────────────► Grafana
                                  │
                                  ▼
                            Service Graph
```

## Observability met SpringBoot 4 OTEL en Grafana Alloy collector

```
Spring Boot 4 application
        │
        │ OTLP/gRPC or OTLP/HTTP
        ▼
┌──────────────────────┐
│    Grafana Alloy     │
│                      │
│ OTLP receiver        │
│   ├── traces         │
│   ├── metrics        │
│   └── logs           │
└──────────┬───────────┘
           │
           ├── traces ──► Tempo
           ├── metrics ─► Prometheus/Mimir
           └── logs ────► Loki
```

### Grafana dashboard drilldown

```
kubectl port-forward svc/kube-prometheus-stack-grafana  3000:80 -n monitoring
```

## GITOPS - manage applicaties met FluxCD.
[Central idea is to show how to use GitOps](docs/gitops-strategie.md) (FluxCD) to manage the deployment of a microservices application on Kubernetes.
1. How to bootstrap FluxCD in your Kubernetes cluster:
```
flux bootstrap github --owner=agilesolutions --repository=AI-agentic-migration-overheid --branch=master --path=./fluxcd --components-extra=image-reflector-controller,image-automation-controller --personal
```
Read full instructions in [docus/fluxcd.md](docs/fluxcd.md)

