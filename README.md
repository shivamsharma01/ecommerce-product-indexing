# Product Indexer Service

Subscribes to Pub/Sub product events (from Firestore via Eventarc), and indexes/updates/deletes products in Elasticsearch. The search service reads from the same Elasticsearch index.

## Architecture

```
Firestore (product writes)
   ↓
Eventarc (triggers on document changes)
   ↓
Pub/Sub topic
   ↓
GKE Indexer (this service, pull subscription)
   ↓
Elasticsearch
   ↓
Search Service (reads)
```

## Setup

### 1. Eventarc + Pub/Sub

Create an Eventarc trigger for Firestore document events on the `products` collection, with a Pub/Sub topic as the destination. Create a pull subscription on that topic (e.g. `product-events-sub`).

### 2. Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `spring.elasticsearch.uris` | `http://localhost:9200` | Elasticsearch connection |
| `product-indexer.pubsub.enabled` | `true` | Enable Pub/Sub subscriber |
| `product-indexer.pubsub.subscription` | `product-events-sub` | Pub/Sub subscription name |

### 3. GCP Credentials

When running on GKE, use a workload identity or service account with:
- `roles/pubsub.subscriber` on the subscription
- Network access to Elasticsearch

For local development, use `gcloud auth application-default login` or `GOOGLE_APPLICATION_CREDENTIALS`.

### 4. Elasticsearch

Use the same Elasticsearch instance as the search service. The index `products` is created automatically on first write.

## Firestore Document Schema

The indexer maps Firestore document fields to the Elasticsearch product schema:

| Firestore field | Elasticsearch field |
|-----------------|---------------------|
| name | name |
| description | description |
| price | price |
| category | categories (single-item list) |
| categories | categories (list) |
| stockQuantity | inStock (stockQuantity > 0) |
| brand | brand |
| imageUrl | imageUrl |
| rating | rating |
| attributes | attributes |

## Event Types

- `google.cloud.firestore.document.v1.written` → index or update in Elasticsearch
- `google.cloud.firestore.document.v1.deleted` → delete from Elasticsearch
