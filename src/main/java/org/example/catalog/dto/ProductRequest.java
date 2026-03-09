package org.example.catalog.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record ProductRequest(

        @NotBlank
        @Pattern(regexp = "^[A-Z0-9-]+$")
        String sku,

        @NotBlank
        @Size(max = 255)
        String name,

        String description,

        @NotNull
        @Positive
        BigDecimal price,

        @Size(min = 3, max = 3)
        String currency,

        UUID categoryId,

        boolean available
) {
}
