# Product indexer

Consumes **product-events** from Pub/Sub, upserts/deletes documents in **Elasticsearch**, and can run a full **reindex** from **Firestore**. The search service reads the same `products` index.

## Requirements

- Java 17
- Running Elasticsearch, reachable Firestore/Pub/Sub in deployed environments

## Run locally

```bash
./gradlew bootRun
```

| Variable | Purpose |
|----------|---------|
| `SPRING_ELASTICSEARCH_URIS` | ES cluster URL |
| `SPRING_CLOUD_GCP_PROJECT_ID` | GCP project |
| `SPRING_CLOUD_GCP_FIRESTORE_ENABLED` | Firestore client |
| `PRODUCT_INDEXER_PUBSUB_ENABLED` | Start subscriber (`true` in prod) |
| `PRODUCT_INDEXER_PUBSUB_SUBSCRIPTION` | e.g. `product-events-sub` |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` | Same `iss` as auth JWT (same as product service) |
| `SERVER_PORT` | Default `8085` |

Admin API: `POST /product-indexer/admin/reindex` requires a JWT with scope **`product.admin`** (bootstrap admin from Flyway — see `auth` README).

## Build and test

```bash
./gradlew test
```

Smoke test loads the Spring context with Firestore/Pub/Sub disabled and mocked index/repository beans — **no Docker**.

## GCP

Typical IAM: Pub/Sub subscriber on the subscription, Firestore reader, network access to Elasticsearch. See Terraform in `ecomm-infra/terraform` for topics/subscriptions.

## Kubernetes

Manifests under **`ecomm-infra/deploy/k8s/apps/product-indexer/`** (ServiceAccount, Deployment, Service, ConfigMap, optional ExternalSecret for ES credentials).
