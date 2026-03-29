package org.example.catalog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catalog.cache.ProductCacheService;
import org.example.catalog.dto.StockReleaseRequest;
import org.example.catalog.dto.StockReserveRequest;
import org.example.catalog.dto.StockResponse;
import org.example.catalog.entity.Stock;
import org.example.catalog.exception.ProductNotFoundException;
import org.example.catalog.exception.StockNotFoundException;
import org.example.catalog.exception.StockOperationException;
import org.example.catalog.repository.ProductRepository;
import org.example.catalog.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    private final ProductCacheService cacheService;

    public StockResponse findByProductId(UUID productId) {
        log.debug("Fetching stock for productId={}", productId);
        return stockRepository.findByProductId(productId)
                .map(StockResponse::fromEntity)
                .orElseThrow(() -> {
                    log.warn("Stock not found for productId={}", productId);
                    return new StockNotFoundException(productId);
                });
    }

    @Transactional
    public StockResponse create(UUID productId, int quantity, String warehouseLocation) {
        log.info("Creating stock for productId={}, quantity={}", productId, quantity);

        if (stockRepository.existsByProductId(productId)) {
            throw new StockOperationException("Stock already exists for productId: " + productId);
        }

        var product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        Stock stock = Stock.builder()
                .product(product)
                .quantity(quantity)
                .reserved(0)
                .warehouseLocation(warehouseLocation)
                .build();

        StockResponse response = StockResponse.fromEntity(stockRepository.save(stock));
        log.info("Stock created for productId={}", productId);
        return response;
    }

    @Transactional
    public StockResponse reserve(StockReserveRequest request) {
        log.info("Reserving stock for productId={}, orderId={}, qty={}",
                request.productId(), request.orderId(), request.quantity());

        int updated = stockRepository.reserveStock(request.productId(), request.quantity());

        if (updated == 0) {
            log.warn("Not enough stock for productId={}, requested={}", request.productId(), request.quantity());
            throw new StockOperationException(
                    "Not enough stock for productId: " + request.productId());
        }

        StockResponse response = findByProductId(request.productId());
        cacheService.evictProduct(request.productId());
        log.info("Stock reserved for productId={}, orderId={}", request.productId(), request.orderId());
        return response;
    }

    @Transactional
    public StockResponse release(StockReleaseRequest request) {
        log.info("Releasing stock for productId={}, orderId={}, qty={}",
                request.productId(), request.orderId(), request.quantity());

        int updated = stockRepository.releaseStock(request.productId(), request.quantity());

        if (updated == 0) {
            log.warn("Failed to release stock for productId={}, requested={}", request.productId(), request.quantity());
            throw new StockOperationException(
                    "Failed to release stock for productId: " + request.productId());
        }

        StockResponse response = findByProductId(request.productId());
        cacheService.evictProduct(request.productId());
        log.info("Stock released for productId={}, orderId={}", request.productId(), request.orderId());
        return response;
    }
}
