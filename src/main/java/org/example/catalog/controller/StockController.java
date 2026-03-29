package org.example.catalog.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.catalog.dto.StockReleaseRequest;
import org.example.catalog.dto.StockReserveRequest;
import org.example.catalog.dto.StockResponse;
import org.example.catalog.service.StockService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping("/products/{productId}/stock")
    public StockResponse getStock(@PathVariable UUID productId) {
        return stockService.findByProductId(productId);
    }

    @PostMapping("/products/{productId}/stock")
    @ResponseStatus(HttpStatus.CREATED)
    public StockResponse createStock(
            @PathVariable UUID productId,
            @RequestParam int quantity,
            @RequestParam(required = false) String warehouseLocation) {
        return stockService.create(productId, quantity, warehouseLocation);
    }

    @PostMapping("/stock/reserve")
    public StockResponse reserve(@Valid @RequestBody StockReserveRequest request) {
        return stockService.reserve(request);
    }

    @PostMapping("/stock/release")
    public StockResponse release(@Valid @RequestBody StockReleaseRequest request) {
        return stockService.release(request);
    }
}
