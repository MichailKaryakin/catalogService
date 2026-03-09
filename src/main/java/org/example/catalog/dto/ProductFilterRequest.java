package org.example.catalog.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductFilterRequest(
        String query,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        UUID categoryId,
        Boolean available
) {
}
