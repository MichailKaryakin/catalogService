package org.example.catalog.kafka.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderStockReserveEvent(
        String eventType,
        UUID orderId,
        UUID userId,
        List<OrderStockItem> items,
        Instant timestamp
) {
}
