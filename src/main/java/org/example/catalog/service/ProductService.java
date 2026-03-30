package org.example.catalog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catalog.cache.ProductCacheService;
import org.example.catalog.dto.*;
import org.example.catalog.entity.Category;
import org.example.catalog.entity.Product;
import org.example.catalog.exception.DuplicateSkuException;
import org.example.catalog.exception.CategoryNotFoundException;
import org.example.catalog.exception.ProductNotFoundException;
import org.example.catalog.kafka.dto.ProductEvent;
import org.example.catalog.repository.CategoryRepository;
import org.example.catalog.repository.ProductRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ProductCacheService cacheService;

    public Page<ProductResponse> findAll(ProductFilterRequest request, Pageable pageable) {
        String queryHash = Integer.toHexString(request.hashCode())
                + ":" + pageable.getPageNumber()
                + ":" + pageable.getPageSize()
                + ":" + pageable.getSort();

        Optional<String> cached = cacheService.getProductsList(queryHash);
        if (cached.isPresent()) {
            try {
                return cacheService.deserializePage(cached.get());
            } catch (Exception e) {
                log.warn("Failed to deserialize cached products list: {}", e.getMessage());
            }
        }

        Page<ProductResponse> page = productRepository.findAll(buildSpec(request), pageable)
                .map(ProductResponse::fromEntity);

        cacheService.putProductsList(queryHash, page);
        return page;
    }

    public ProductResponse findById(UUID id) {
        Optional<ProductResponse> cached = cacheService.getProduct(id);
        if (cached.isPresent()) {
            return cached.get();
        }

        ProductResponse response = productRepository.findById(id)
                .map(ProductResponse::fromEntity)
                .orElseThrow(() -> {
                    log.warn("Product not found by id: {}", id);
                    return new ProductNotFoundException(id);
                });

        cacheService.putProduct(response);
        return response;
    }

    public ProductResponse findBySku(String sku) {
        return productRepository.findBySku(sku)
                .map(ProductResponse::fromEntity)
                .orElseThrow(() -> {
                    log.warn("Product not found by sku: {}", sku);
                    return new ProductNotFoundException("sku", sku);
                });
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        log.info("Creating product with sku: {}", request.sku());
        if (productRepository.existsBySku(request.sku())) {
            log.warn("Duplicate sku on create: {}", request.sku());
            throw new DuplicateSkuException(request.sku());
        }

        Product product = Product.builder()
                .sku(request.sku())
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .currency(request.currency() != null ? request.currency() : "EUR")
                .available(request.available())
                .category(resolveCategory(request.categoryId()))
                .build();

        ProductResponse response = ProductResponse.fromEntity(productRepository.save(product));

        cacheService.putProduct(response);
        cacheService.evictAllProductsLists();

        eventPublisher.publishEvent(
                ProductEvent.created(response.id(), response.sku(), response.price(), response.available())
        );

        log.info("Product created: id={}, sku={}", response.id(), response.sku());
        return response;
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        log.info("Updating product id: {}", id);
        Product product = getProductOrThrow(id);

        if (!product.getSku().equals(request.sku()) && productRepository.existsBySku(request.sku())) {
            log.warn("Duplicate sku on update: {}", request.sku());
            throw new DuplicateSkuException(request.sku());
        }

        product.setSku(request.sku());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setCurrency(request.currency() != null ? request.currency() : "EUR");
        product.setAvailable(request.available());
        product.setCategory(resolveCategory(request.categoryId()));

        ProductResponse response = ProductResponse.fromEntity(product);

        cacheService.evictProduct(id);
        cacheService.putProduct(response);
        cacheService.evictAllProductsLists();

        eventPublisher.publishEvent(
                ProductEvent.updated(id, request.sku(), request.price(), request.available())
        );

        log.info("Product updated: id={}, sku={}", id, request.sku());
        return response;
    }

    @Transactional
    public ProductResponse patch(UUID id, ProductPatchRequest request) {
        log.info("Patching product id: {}", id);
        Product product = getProductOrThrow(id);

        if (request.sku() != null) {
            if (!product.getSku().equals(request.sku()) && productRepository.existsBySku(request.sku())) {
                log.warn("Duplicate sku on patch: {}", request.sku());
                throw new DuplicateSkuException(request.sku());
            }
            product.setSku(request.sku());
        }
        if (request.name() != null) product.setName(request.name());
        if (request.description() != null) product.setDescription(request.description());
        if (request.price() != null) product.setPrice(request.price());
        if (request.currency() != null) product.setCurrency(request.currency());
        if (request.available() != null) product.setAvailable(request.available());
        if (request.categoryId() != null) product.setCategory(resolveCategory(request.categoryId()));

        ProductResponse response = ProductResponse.fromEntity(product);

        cacheService.evictProduct(id);
        cacheService.putProduct(response);
        cacheService.evictAllProductsLists();

        eventPublisher.publishEvent(
                ProductEvent.updated(id, product.getSku(), product.getPrice(), product.isAvailable())
        );

        log.info("Product patched: id={}", id);
        return response;
    }

    @Transactional
    public void delete(UUID id) {
        log.info("Deleting product id: {}", id);
        Product product = getProductOrThrow(id);

        eventPublisher.publishEvent(
                ProductEvent.deleted(id, product.getSku(), product.getPrice(), product.isAvailable())
        );

        productRepository.delete(product);

        cacheService.evictProduct(id);
        cacheService.evictAllProductsLists();

        log.info("Product deleted: id={}", id);
    }

    private Product getProductOrThrow(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product not found: {}", id);
                    return new ProductNotFoundException(id);
                });
    }

    private Category resolveCategory(UUID categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    log.warn("Category not found: {}", categoryId);
                    return new CategoryNotFoundException(categoryId);
                });
    }

    private Specification<Product> buildSpec(ProductFilterRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.query() != null && !request.query().isBlank()) {
                String pattern = "%" + request.query().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }
            if (request.minPrice() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), request.minPrice()));
            if (request.maxPrice() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), request.maxPrice()));
            if (request.categoryId() != null)
                predicates.add(cb.equal(root.get("category").get("id"), request.categoryId()));
            if (request.available() != null)
                predicates.add(cb.equal(root.get("available"), request.available()));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
