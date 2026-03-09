package org.example.catalog.exception;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(UUID id) {
        super("Product not found with id: " + id);
    }

    public ProductNotFoundException(String field, String value) {
        super("Product not found with " + field + ": " + value);
    }
}
