package org.example.catalog.service;

import org.example.catalog.cache.ProductCacheService;
import org.example.catalog.dto.*;
import org.example.catalog.entity.Category;
import org.example.catalog.entity.Product;
import org.example.catalog.exception.DuplicateSkuException;
import org.example.catalog.exception.ProductNotFoundException;
import org.example.catalog.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService unit tests")
class ProductServiceTest {

    @Mock
    ProductRepository productRepository;
    @Mock
    ApplicationEventPublisher eventPublisher;
    @Mock
    ProductCacheService cacheService;

    @InjectMocks
    ProductService productService;

    private UUID productId;
    private Product product;
    private Category category;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        category = Category.builder()
                .id(UUID.randomUUID())
                .name("Electronics")
                .build();
        product = Product.builder()
                .id(productId)
                .sku("ABC-123")
                .name("Test Product")
                .description("A test product")
                .price(new BigDecimal("99.99"))
                .currency("EUR")
                .available(true)
                .category(category)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("findById: cache hit — returns cached value, no DB call")
    void findById_cacheHit_returnsCached() {
        ProductResponse cached = ProductResponse.fromEntity(product);
        when(cacheService.getProduct(productId)).thenReturn(Optional.of(cached));

        ProductResponse result = productService.findById(productId);

        assertThat(result.id()).isEqualTo(productId);
        verifyNoInteractions(productRepository);
    }

    @Test
    @DisplayName("findById: cache miss — loads from DB and puts to cache")
    void findById_cacheMiss_loadsFromDb() {
        when(cacheService.getProduct(productId)).thenReturn(Optional.empty());
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductResponse result = productService.findById(productId);

        assertThat(result.id()).isEqualTo(productId);
        assertThat(result.sku()).isEqualTo("ABC-123");
        verify(cacheService).putProduct(any(ProductResponse.class));
    }

    @Test
    @DisplayName("findById: product not in cache or DB — throws ProductNotFoundException")
    void findById_notFound_throws() {
        when(cacheService.getProduct(productId)).thenReturn(Optional.empty());
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(productId))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining(productId.toString());
    }

    @Test
    @DisplayName("findBySku: found — returns response")
    void findBySku_found_returnsResponse() {
        when(productRepository.findBySku("ABC-123")).thenReturn(Optional.of(product));

        ProductResponse result = productService.findBySku("ABC-123");

        assertThat(result.sku()).isEqualTo("ABC-123");
    }

    @Test
    @DisplayName("findBySku: not found — throws ProductNotFoundException")
    void findBySku_notFound_throws() {
        when(productRepository.findBySku("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findBySku("MISSING"))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("sku");
    }

    @Test
    @DisplayName("findAll: cache miss — queries DB, caches result")
    void findAll_cacheMiss_queriesDb() {
        ProductFilterRequest filter = new ProductFilterRequest(null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> dbPage = new PageImpl<>(List.of(product));

        when(cacheService.getProductsList(any())).thenReturn(Optional.empty());
        when(productRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(dbPage);

        Page<ProductResponse> result = productService.findAll(filter, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).sku()).isEqualTo("ABC-123");
        verify(cacheService).putProductsList(any(), any());
    }

    @Test
    @DisplayName("findAll: cache hit — returns cached page, no DB call")
    void findAll_cacheHit_returnsCached() throws Exception {
        ProductFilterRequest filter = new ProductFilterRequest(null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);
        String cachedJson = "cached-json";
        Page<ProductResponse> cachedPage = new PageImpl<>(List.of(ProductResponse.fromEntity(product)));

        when(cacheService.getProductsList(any())).thenReturn(Optional.of(cachedJson));
        when(cacheService.deserializePage(cachedJson)).thenReturn(cachedPage);

        Page<ProductResponse> result = productService.findAll(filter, pageable);

        assertThat(result.getContent()).hasSize(1);
        verifyNoInteractions(productRepository);
    }

    @Test
    @DisplayName("create: valid request — saves product, publishes event, updates cache")
    void create_valid_savesAndPublishesEvent() {
        ProductRequest request = ProductRequest.builder()
                .sku("NEW-001")
                .name("New Product")
                .price(new BigDecimal("49.99"))
                .available(true)
                .build();

        when(productRepository.existsBySku("NEW-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p = Product.builder()
                    .id(UUID.randomUUID())
                    .sku(p.getSku())
                    .name(p.getName())
                    .price(p.getPrice())
                    .currency("EUR")
                    .available(p.isAvailable())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            return p;
        });

        ProductResponse result = productService.create(request);

        assertThat(result.sku()).isEqualTo("NEW-001");
        assertThat(result.currency()).isEqualTo("EUR"); // default currency
        verify(productRepository).save(any(Product.class));
        verify(eventPublisher).publishEvent(any(Object.class));
        verify(cacheService).putProduct(any());
        verify(cacheService).evictAllProductsLists();
    }

    @Test
    @DisplayName("create: duplicate SKU — throws DuplicateSkuException, no save")
    void create_duplicateSku_throws() {
        ProductRequest request = ProductRequest.builder()
                .sku("ABC-123")
                .name("Dup")
                .price(BigDecimal.TEN)
                .available(true)
                .build();

        when(productRepository.existsBySku("ABC-123")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(DuplicateSkuException.class);

        verify(productRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("create: no currency in request — defaults to EUR")
    void create_noCurrency_defaultsToEur() {
        ProductRequest request = ProductRequest.builder()
                .sku("CUR-001")
                .name("Currency Test")
                .price(BigDecimal.TEN)
                .currency(null)
                .available(true)
                .build();

        when(productRepository.existsBySku(any())).thenReturn(false);
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            return Product.builder()
                    .id(UUID.randomUUID()).sku(p.getSku()).name(p.getName())
                    .price(p.getPrice()).currency(p.getCurrency())
                    .available(true).createdAt(Instant.now()).updatedAt(Instant.now())
                    .build();
        });

        ProductResponse result = productService.create(request);
        assertThat(result.currency()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("update: valid — updates fields, evicts old cache, publishes event")
    void update_valid_updatesAndPublishes() {
        ProductRequest request = ProductRequest.builder()
                .sku("ABC-123")
                .name("Updated Name")
                .price(new BigDecimal("199.99"))
                .available(false)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductResponse result = productService.update(productId, request);

        assertThat(result.name()).isEqualTo("Updated Name");
        verify(cacheService).evictProduct(productId);
        verify(cacheService).putProduct(any());
        verify(cacheService).evictAllProductsLists();
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("update: product not found — throws ProductNotFoundException")
    void update_notFound_throws() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        ProductRequest request = ProductRequest.builder()
                .sku("X").name("Y").price(BigDecimal.ONE).available(true).build();

        assertThatThrownBy(() -> productService.update(productId, request))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("update: SKU changed to existing one — throws DuplicateSkuException")
    void update_skuChangedToDuplicate_throws() {
        ProductRequest request = ProductRequest.builder()
                .sku("OTHER-SKU")
                .name("Name")
                .price(BigDecimal.TEN)
                .available(true)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.existsBySku("OTHER-SKU")).thenReturn(true);

        assertThatThrownBy(() -> productService.update(productId, request))
                .isInstanceOf(DuplicateSkuException.class);
    }

    @Test
    @DisplayName("patch: only name changed — other fields unchanged")
    void patch_nameOnly_otherFieldsUnchanged() {
        ProductPatchRequest request = new ProductPatchRequest(null, "New Name", null, null, null, null, null);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductResponse result = productService.patch(productId, request);

        assertThat(result.name()).isEqualTo("New Name");
        assertThat(result.sku()).isEqualTo("ABC-123");
        assertThat(result.price()).isEqualByComparingTo("99.99");
    }

    @Test
    @DisplayName("patch: null fields — no changes applied")
    void patch_allNull_noChanges() {
        ProductPatchRequest request = new ProductPatchRequest(null, null, null, null, null, null, null);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductResponse result = productService.patch(productId, request);

        assertThat(result.sku()).isEqualTo("ABC-123");
        assertThat(result.name()).isEqualTo("Test Product");
    }

    @Test
    @DisplayName("delete: existing product — deletes, evicts cache, publishes event")
    void delete_existing_deletesAndPublishes() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        productService.delete(productId);

        verify(productRepository).delete((Product) product);
        verify(cacheService).evictProduct(productId);
        verify(cacheService).evictAllProductsLists();
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("delete: not found — throws, no delete called")
    void delete_notFound_throws() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(productId))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productRepository, never()).delete(any(Product.class));
    }
}
