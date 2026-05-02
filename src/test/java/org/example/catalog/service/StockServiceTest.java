package org.example.catalog.service;

import org.example.catalog.cache.ProductCacheService;
import org.example.catalog.dto.StockReleaseRequest;
import org.example.catalog.dto.StockReserveRequest;
import org.example.catalog.dto.StockResponse;
import org.example.catalog.entity.Product;
import org.example.catalog.entity.Stock;
import org.example.catalog.exception.ProductNotFoundException;
import org.example.catalog.exception.StockNotFoundException;
import org.example.catalog.exception.StockOperationException;
import org.example.catalog.repository.ProductRepository;
import org.example.catalog.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockService unit tests")
class StockServiceTest {

    @Mock
    StockRepository stockRepository;
    @Mock
    ProductRepository productRepository;
    @Mock
    ProductCacheService cacheService;

    @InjectMocks
    StockService stockService;

    private UUID productId;
    private Product product;
    private Stock stock;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        product = Product.builder()
                .id(productId)
                .sku("ABC-123")
                .name("Test Product")
                .price(new BigDecimal("99.99"))
                .currency("EUR")
                .available(true)
                .build();
        stock = Stock.builder()
                .id(UUID.randomUUID())
                .product(product)
                .quantity(100)
                .reserved(10)
                .warehouseLocation("A1")
                .build();
    }

    @Test
    @DisplayName("findByProductId: found — returns StockResponse")
    void findByProductId_found_returnsResponse() {
        when(stockRepository.findByProductId(productId)).thenReturn(Optional.of(stock));

        StockResponse result = stockService.findByProductId(productId);

        assertThat(result.productId()).isEqualTo(productId);
        assertThat(result.quantity()).isEqualTo(100);
        assertThat(result.reserved()).isEqualTo(10);
    }

    @Test
    @DisplayName("findByProductId: not found — throws StockNotFoundException")
    void findByProductId_notFound_throws() {
        when(stockRepository.findByProductId(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.findByProductId(productId))
                .isInstanceOf(StockNotFoundException.class);
    }

    @Test
    @DisplayName("create: valid — saves stock and returns response")
    void create_valid_savesStock() {
        when(stockRepository.existsByProductId(productId)).thenReturn(false);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(stockRepository.save(any(Stock.class))).thenReturn(stock);

        StockResponse result = stockService.create(productId, 100, "A1");

        assertThat(result.productId()).isEqualTo(productId);
        verify(stockRepository).save(any(Stock.class));
    }

    @Test
    @DisplayName("create: stock already exists — throws StockOperationException")
    void create_alreadyExists_throws() {
        when(stockRepository.existsByProductId(productId)).thenReturn(true);

        assertThatThrownBy(() -> stockService.create(productId, 50, null))
                .isInstanceOf(StockOperationException.class)
                .hasMessageContaining("Stock already exists");

        verify(stockRepository, never()).save(any());
    }

    @Test
    @DisplayName("create: product not found — throws ProductNotFoundException")
    void create_productNotFound_throws() {
        when(stockRepository.existsByProductId(productId)).thenReturn(false);
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.create(productId, 10, null))
                .isInstanceOf(ProductNotFoundException.class);

        verify(stockRepository, never()).save(any());
    }

    @Test
    @DisplayName("reserve: enough stock — updates reserved, evicts cache")
    void reserve_enoughStock_succeeds() {
        StockReserveRequest request = new StockReserveRequest(UUID.randomUUID(), productId, "ABC-123", 5);

        when(stockRepository.reserveStock(productId, 5)).thenReturn(1);
        when(stockRepository.findByProductId(productId)).thenReturn(Optional.of(stock));

        StockResponse result = stockService.reserve(request);

        assertThat(result).isNotNull();
        verify(cacheService).evictProduct(productId);
    }

    @Test
    @DisplayName("reserve: not enough stock — throws StockOperationException, no cache evict")
    void reserve_notEnoughStock_throws() {
        StockReserveRequest request = new StockReserveRequest(UUID.randomUUID(), productId, "ABC-123", 999);

        when(stockRepository.reserveStock(productId, 999)).thenReturn(0);

        assertThatThrownBy(() -> stockService.reserve(request))
                .isInstanceOf(StockOperationException.class)
                .hasMessageContaining("Not enough stock");

        verifyNoInteractions(cacheService);
    }

    @Test
    @DisplayName("release: valid — releases stock, evicts cache")
    void release_valid_succeeds() {
        StockReleaseRequest request = new StockReleaseRequest(UUID.randomUUID(), productId, 5);

        when(stockRepository.releaseStock(productId, 5)).thenReturn(1);
        when(stockRepository.findByProductId(productId)).thenReturn(Optional.of(stock));

        StockResponse result = stockService.release(request);

        assertThat(result).isNotNull();
        verify(cacheService).evictProduct(productId);
    }

    @Test
    @DisplayName("release: DB returns 0 — throws StockOperationException")
    void release_dbReturnsZero_throws() {
        StockReleaseRequest request = new StockReleaseRequest(UUID.randomUUID(), productId, 5);

        when(stockRepository.releaseStock(productId, 5)).thenReturn(0);

        assertThatThrownBy(() -> stockService.release(request))
                .isInstanceOf(StockOperationException.class)
                .hasMessageContaining("Failed to release stock");

        verifyNoInteractions(cacheService);
    }
}
