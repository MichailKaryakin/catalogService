package org.example.catalog.exception;

public class StockOperationException extends RuntimeException {
    public StockOperationException(String message) {
        super(message);
    }
}
