package org.example.catalog.dto;

import org.example.catalog.entity.Stock;

import java.util.UUID;

public record StockResponse(
        UUID id,
        UUID productId,
        String sku,
        int quantity,
        int reserved,
        int available,
        String warehouseLocation
) {
    public static StockResponse fromEntity(Stock stock) {
        return new StockResponse(
                stock.getId(),
                stock.getProduct().getId(),
                stock.getProduct().getSku(),
                stock.getQuantity(),
                stock.getReserved(),
                stock.getQuantity() - stock.getReserved(),
                stock.getWarehouseLocation()
        );
    }
}
