package org.example.catalog.exception;

import java.util.UUID;

public class StockNotFoundException extends RuntimeException {
    public StockNotFoundException(UUID productId) {
        super("Stock not found for productId: " + productId);
    }
}
