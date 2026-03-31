package org.example.catalog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.catalog.dto.StockReleaseRequest;
import org.example.catalog.dto.StockReserveRequest;
import org.example.catalog.dto.StockResponse;
import org.example.catalog.service.StockService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Stock", description = "Managing product stock and reservations")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @Operation(summary = "Get stock by product ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock info"),
            @ApiResponse(responseCode = "404", description = "Stock not found")
    })
    @GetMapping("/products/{productId}/stock")
    public StockResponse getStock(
            @Parameter(description = "Product UUID") @PathVariable UUID productId
    ) {
        return stockService.findByProductId(productId);
    }

    @Operation(summary = "Create stock for product", description = "Initializes stock entry for a product")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Stock created"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "422", description = "Stock already exists for product")
    })
    @PostMapping("/products/{productId}/stock")
    @ResponseStatus(HttpStatus.CREATED)
    public StockResponse createStock(
            @Parameter(description = "Product UUID") @PathVariable UUID productId,
            @Parameter(description = "Initial quantity") @RequestParam int quantity,
            @Parameter(description = "Warehouse location (optional)") @RequestParam(required = false) String warehouseLocation
    ) {
        return stockService.create(productId, quantity, warehouseLocation);
    }

    @Operation(summary = "Reserve stock", description = "Reserves stock for an order. Used by Order Service")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock reserved"),
            @ApiResponse(responseCode = "404", description = "Stock not found"),
            @ApiResponse(responseCode = "422", description = "Not enough stock")
    })
    @PostMapping("/stock/reserve")
    public StockResponse reserve(@Valid @RequestBody StockReserveRequest request) {
        return stockService.reserve(request);
    }

    @Operation(summary = "Release stock", description = "Releases reserved stock. Used on order cancellation")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock released"),
            @ApiResponse(responseCode = "404", description = "Stock not found"),
            @ApiResponse(responseCode = "422", description = "Failed to release stock")
    })
    @PostMapping("/stock/release")
    public StockResponse release(@Valid @RequestBody StockReleaseRequest request) {
        return stockService.release(request);
    }
}
