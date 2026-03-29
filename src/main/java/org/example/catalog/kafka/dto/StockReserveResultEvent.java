package org.example.catalog.kafka.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StockReserveResultEvent(
        String eventType,
        UUID orderId,
        List<StockReserveItemResult> results,
        Instant timestamp
) {
    public static StockReserveResultEvent of(UUID orderId, List<StockReserveItemResult> results) {
        return new StockReserveResultEvent("STOCK_RESERVE_RESULT", orderId, results, Instant.now());
    }
}
