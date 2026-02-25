package com.mcart.product_indexer.config;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ReindexGate {

    private final AtomicBoolean reindexInProgress = new AtomicBoolean(false);

    public boolean isReindexInProgress() {
        return reindexInProgress.get();
    }

    public void setReindexInProgress(boolean inProgress) {
        reindexInProgress.set(inProgress);
    }
}
