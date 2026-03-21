# Product Indexer Service

Subscribes to Pub/Sub product events (from product service outbox), and indexes/updates/deletes products in Elasticsearch. The search service reads from the same Elasticsearch index.

## Architecture

```
Product Service (writes Firestore + outbox)
   ↓
Pub/Sub topic (product-events)
   ↓
Product Indexer (this service, pull subscription)
   ↓
Elasticsearch
   ↓
Search Service (reads)
```

## Setup

### 1. Pub/Sub

Product service publishes events to the `product-events` topic. Create a pull subscription (e.g. `product-events-sub`).

### 2. Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `spring.elasticsearch.uris` | `http://localhost:9200` | Elasticsearch connection |
| `product-indexer.pubsub.enabled` | `true` | Enable Pub/Sub subscriber |
| `product-indexer.pubsub.subscription` | `product-events-sub` | Pub/Sub subscription name |

### 3. GCP Credentials & IAM

When running on GKE, use a workload identity or service account with:
- `roles/pubsub.subscriber` on the subscription
- `roles/datastore.user` (Cloud Datastore User) for Firestore read
- Network access to Elasticsearch

For local development, use `gcloud auth application-default login` or `GOOGLE_APPLICATION_CREDENTIALS`.

**If you get PERMISSION_DENIED:** Ensure Cloud Firestore API is enabled. Try `roles/datastore.owner` temporarily to verify; then narrow to `roles/datastore.user` for production.

### 4. Elasticsearch

Use the same Elasticsearch instance as the search service. The index `products` is created automatically on first write.

### Elasticsearch document contract (shared with search)

The indexer’s `com.mcart.product_indexer.model.Product` must stay aligned with `com.mcart.search.model.Product` in the **search** service: same index name (`products`), Java property names (camelCase JSON), and field mapping — including `name` as **text + `name.sort` keyword** for sorting and **`attributes` as a queryable object** (not `enabled: false`).

If an index already existed with an older mapping, run **reindex** or delete the index so it is recreated with the current mapping.

**Outbox payload (`ProductEventPayload`)** supports the same logical fields as the search API document, including optional `categories` (list), `brand`, `imageUrl`, `rating`, `inStock`, and `attributes` (object). Legacy single `category` and `stockQuantity` still work.

## Event Types (ProductEventEnvelope)

- `PRODUCT_CREATED` / `PRODUCT_UPDATED` → index or update in Elasticsearch
- `PRODUCT_DELETED` → delete from Elasticsearch

## Reindex

To bulk reindex all products from Firestore into Elasticsearch (deletes index first, so users won't see old/deleted products):

```bash
curl -X POST http://localhost:8085/admin/reindex
```

Response: `{"success":true,"indexedCount":42}`
