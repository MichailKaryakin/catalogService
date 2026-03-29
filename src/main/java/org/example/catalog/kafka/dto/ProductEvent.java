package org.example.catalog.kafka.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductEvent(
        String eventType,
        UUID productId,
        String sku,
        BigDecimal price,
        boolean available,

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant timestamp
) {
    public static ProductEvent created(UUID productId, String sku, BigDecimal price, boolean available) {
        return new ProductEvent("PRODUCT_CREATED", productId, sku, price, available, Instant.now());
    }

    public static ProductEvent updated(UUID productId, String sku, BigDecimal price, boolean available) {
        return new ProductEvent("PRODUCT_UPDATED", productId, sku, price, available, Instant.now());
    }

    public static ProductEvent deleted(UUID productId, String sku, BigDecimal price, boolean available) {
        return new ProductEvent("PRODUCT_DELETED", productId, sku, price, available, Instant.now());
    }
}
