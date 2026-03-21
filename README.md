# Product Indexer

Spring Boot service that consumes **Google Cloud Pub/Sub** product lifecycle events, keeps **Elasticsearch** in sync with the canonical **Firestore** product data, and exposes an **admin reindex** API for full rebuilds. The search service reads the same `products` index.

## Capabilities

- **Event-driven indexing**: Pull subscriber on a configurable subscription; processes `PRODUCT_CREATED`, `PRODUCT_UPDATED` (upsert with version / `updatedAt` guards), and `PRODUCT_DELETED` (idempotent delete).
- **Elasticsearch**: Ensures the `products` index exists with mapping from `Product` on startup; uses `ElasticsearchOperations` (repository scanning disabled).
- **Firestore**: Reactive repository reads for full reindex; GCP Firestore starter enabled.
- **Reindex safety**: While a full reindex runs, inbound Pub/Sub messages are **not** ack’d (they are nack’d / redelivered) via `ReindexGate` so the index is not mutated concurrently.
- **Operations**: HTTP `POST` to trigger full reindex (drops index, recreates mapping, bulk load from Firestore). Admin routes use the same **JWT resource-server** model as **user-service** and **product-service** (Bearer token, shared OIDC issuer).

## Architecture

```
Product Service (Firestore + outbox)
        ↓
Pub/Sub topic (e.g. product-events)
        ↓
Product Indexer (pull subscription)
        ↓
Elasticsearch (index: products)
        ↓
Search Service (read)
```

## Stack

- Java 17, Spring Boot 3.2
- Spring Security **OAuth2 resource server** (JWT)
- Spring Data Elasticsearch, Testcontainers (tests)
- Spring Cloud GCP: Pub/Sub, Firestore

## Configuration

Defaults live in `src/main/resources/application.yaml`. Override with environment variables, `application-{profile}.yaml`, or external config as needed.

| Key | Purpose |
|-----|---------|
| `server.port` | HTTP port (default `8085`) |
| `spring.elasticsearch.uris` | Elasticsearch cluster URL |
| `spring.data.elasticsearch.auto-index-creation` | Allow index creation |
| `spring.data.elasticsearch.repositories.enabled` | Set `false` when using operations only |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | OIDC issuer (JWT validation). Env: `JWT_ISSUER_URI` — **use the same value as user-service / product-service** |
| `spring.cloud.gcp.project-id` | GCP project |
| `spring.cloud.gcp.credentials.location` | Optional JSON key path (local); prefer workload identity on GKE |
| `spring.cloud.gcp.firestore.enabled` | Firestore client |
| `product-indexer.pubsub.enabled` | When `true`, starts the Pub/Sub subscriber (`false` in tests) |
| `product-indexer.pubsub.subscription` | Subscription name |

Example shape (see repo file for full values):

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${JWT_ISSUER_URI:http://localhost:8180/realms/mcart}
server:
  port: 8085
product-indexer:
  pubsub:
    enabled: true
    subscription: product-events-sub
```

**Secrets**: Do not commit real credential paths or keys. Use `GOOGLE_APPLICATION_CREDENTIALS`, Workload Identity, or a local untracked override file.

## Security (admin API)

- Endpoints under `/product-indexer/admin/**` require a **valid JWT** in the `Authorization: Bearer <token>` header (`spring-boot-starter-oauth2-resource-server`) and the **`ADMIN`** role (from the `roles` claim, `realm_access.roles`, or scopes mapped by Spring).
- Configure **`JWT_ISSUER_URI`** (or `spring.security.oauth2.resourceserver.jwt.issuer-uri`) to match the **same OIDC realm / issuer** used by user-service and product-service so tokens from your IdP validate here.
- Authorities are derived from JWT claims the same way as typical mcart services: top-level **`roles`** (strings, optional `ROLE_` prefix), **`realm_access.roles`** (Keycloak-style), then OAuth2 **`scope`** strings.
- To call reindex from a script, obtain an access token from your IdP (client credentials or user login, depending on policy) and pass it as Bearer.

## GCP setup

- Pub/Sub: product service publishes to a topic; create a **pull** subscription (name must match `product-indexer.pubsub.subscription`).
- IAM (typical): `roles/pubsub.subscriber` on the subscription; `roles/datastore.user` (or broader for troubleshooting) for Firestore reads; network reachability to Elasticsearch.

If you see **PERMISSION_DENIED** on Firestore, ensure the Firestore API is enabled and IAM is correct.

## API

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/product-indexer/admin/reindex` | Deletes `products` index, recreates mapping, indexes all products from Firestore. Requires **Bearer JWT** with **`ADMIN`** role. JSON: `success` is `true` only if every document indexed (`failedCount == 0`); `indexedCount` and `failedCount` are always present. |

Example (replace `<access_token>` with a token from your IdP, same issuer as other services):

```bash
curl -X POST http://localhost:8085/product-indexer/admin/reindex \
  -H "Authorization: Bearer <access_token>"
```

## Events

Payloads are expected as **ProductEventEnvelope** JSON (must include `aggregateId` and `payload`). Supported types:

- `PRODUCT_CREATED` / `PRODUCT_UPDATED` → index or update in Elasticsearch
- `PRODUCT_DELETED` → delete from Elasticsearch (no-op if index missing)

## Contract with search service

The indexed document (`com.mcart.product_indexer.model.Product`) must stay aligned with the search service’s `Product` model: same index name (`products`), field names, and mapping (e.g. sortable `name`, queryable `attributes`). If an old index exists with wrong mapping, reindex or delete the index so it is recreated.

**Outbox / payload** fields should mirror the search API document where applicable: optional `categories`, `brand`, `imageUrl`, `rating`, `inStock`, `attributes`; legacy `category` and `stockQuantity` are still supported.

## Build and test

```bash
./gradlew test
```

Requires Docker for Testcontainers (Elasticsearch). Integration tests register a **test `JwtDecoder`** (`IntegrationTestJwtConfig`) so the suite does not call a real OIDC issuer; production uses the issuer from configuration.
