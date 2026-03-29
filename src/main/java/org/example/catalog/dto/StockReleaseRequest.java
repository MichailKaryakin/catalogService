package org.example.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StockReleaseRequest(
        @NotNull UUID orderId,
        @NotNull UUID productId,
        @Min(1) int quantity
) {
}
