package com.mcart.product_indexer.controller;

import com.mcart.product_indexer.service.ReindexService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/product-indexer/admin")
@RequiredArgsConstructor
public class ReindexController {

    private final ReindexService reindexService;

    @PostMapping("/reindex")
    public ResponseEntity<Map<String, Object>> reindex() {
        long count = reindexService.reindex();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "indexedCount", count
        ));
    }
}
