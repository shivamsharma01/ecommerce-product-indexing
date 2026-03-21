package com.mcart.product_indexer.service;

public record ReindexResult(long indexedCount, long failedCount) {

    public boolean isCompleteSuccess() {
        return failedCount == 0;
    }
}
