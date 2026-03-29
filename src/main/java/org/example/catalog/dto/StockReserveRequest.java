package org.example.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StockReserveRequest(
        @NotNull UUID orderId,
        @NotNull UUID productId,
        @NotNull String sku,
        @Min(1) int quantity
) {
}
