package org.example.catalog.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record ProductPatchRequest(

        @Pattern(regexp = "^[A-Z0-9-]+$")
        String sku,

        @Size(max = 255)
        String name,

        String description,

        @Positive
        BigDecimal price,

        @Size(min = 3, max = 3)
        String currency,

        UUID categoryId,

        Boolean available
) {
}
