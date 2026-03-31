package org.example.catalog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.catalog.dto.ProductFilterRequest;
import org.example.catalog.dto.ProductPatchRequest;
import org.example.catalog.dto.ProductRequest;
import org.example.catalog.dto.ProductResponse;
import org.example.catalog.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Products", description = "Managing catalog products")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Get all products", description = "Returns paginated and filtered list of products")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of products")
    })
    @GetMapping
    public Page<ProductResponse> getAll(
            @Parameter(description = "Filter parameters: q, minPrice, maxPrice, categoryId, available")
            ProductFilterRequest filter,
            @Parameter(description = "Pagination: page, size, sort")
            Pageable pageable
    ) {
        return productService.findAll(filter, pageable);
    }

    @Operation(summary = "Get product by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}")
    public ProductResponse getById(
            @Parameter(description = "Product UUID") @PathVariable UUID id
    ) {
        return productService.findById(id);
    }

    @Operation(summary = "Get product by SKU")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/sku/{sku}")
    public ProductResponse getBySku(
            @Parameter(description = "Product SKU, pattern: A-Z0-9-") @PathVariable String sku
    ) {
        return productService.findBySku(sku);
    }

    @Operation(summary = "Create product", description = "Admin only. Creates a new product in the catalog")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "SKU already exists")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @Operation(summary = "Update product", description = "Admin only. Full update of product fields")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "409", description = "SKU already exists")
    })
    @PutMapping("/{id}")
    public ProductResponse update(
            @Parameter(description = "Product UUID") @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request
    ) {
        return productService.update(id, request);
    }

    @Operation(summary = "Patch product", description = "Admin only. Partial update of product fields")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product patched"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "409", description = "SKU already exists")
    })
    @PatchMapping("/{id}")
    public ProductResponse patch(
            @Parameter(description = "Product UUID") @PathVariable UUID id,
            @Valid @RequestBody ProductPatchRequest request
    ) {
        return productService.patch(id, request);
    }

    @Operation(summary = "Delete product", description = "Admin only. Removes product from catalog")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product deleted"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "Product UUID") @PathVariable UUID id
    ) {
        productService.delete(id);
    }
}
