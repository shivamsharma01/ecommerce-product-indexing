package com.mcart.product_indexer.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.mcart.product_indexer.dto.CloudEventEnvelope;
import com.mcart.product_indexer.service.ProductIndexerService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Pub/Sub pull subscriber for product Firestore events.
 * Eventarc publishes Firestore document events to the configured Pub/Sub topic;
 * this subscriber pulls messages and indexes products in Elasticsearch.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "product-indexer.pubsub.enabled", havingValue = "true", matchIfMissing = true)
public class ProductEventSubscriber {

    private static final String DEFAULT_SUBSCRIPTION = "product-events-sub";

    private final ProductIndexerService productIndexerService;
    private final ObjectMapper objectMapper;
    private final PubSubTemplate pubSubTemplate;

    @Value("${product-indexer.pubsub.subscription:" + DEFAULT_SUBSCRIPTION + "}")
    private String subscriptionName;

    private com.google.cloud.pubsub.v1.Subscriber subscriber;

    @PostConstruct
    public void subscribe() {
        subscriber = pubSubTemplate.subscribe(subscriptionName, this::handleMessage);
        log.info("Subscribed to Pub/Sub subscription: {}", subscriptionName);
    }

    @PreDestroy
    public void shutdown() {
        if (subscriber != null) {
            subscriber.stopAsync();
            log.info("Stopped Pub/Sub subscription: {}", subscriptionName);
        }
    }

    private void handleMessage(BasicAcknowledgeablePubsubMessage message) {
        try {
            String payload = message.getPubsubMessage().getData().toStringUtf8();
            CloudEventEnvelope envelope = objectMapper.readValue(payload, CloudEventEnvelope.class);
            productIndexerService.processEvent(envelope);
            message.ack();
        } catch (Exception ex) {
            log.error("Failed to process product event", ex);
            message.nack();
        }
    }
}
