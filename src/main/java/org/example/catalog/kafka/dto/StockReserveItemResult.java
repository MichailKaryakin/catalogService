package org.example.catalog.kafka.dto;

import java.util.UUID;

public record StockReserveItemResult(
        UUID productId,
        String sku,
        int requested,
        int reserved,
        boolean success,
        String reason
) {
    public static StockReserveItemResult success(UUID productId, String sku, int quantity) {
        return new StockReserveItemResult(productId, sku, quantity, quantity, true, null);
    }

    public static StockReserveItemResult failure(UUID productId, String sku, int quantity, String reason) {
        return new StockReserveItemResult(productId, sku, quantity, 0, false, reason);
    }
}
