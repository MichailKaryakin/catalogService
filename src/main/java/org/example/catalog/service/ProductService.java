package org.example.catalog.service;

import lombok.RequiredArgsConstructor;
import org.example.catalog.dto.*;
import org.example.catalog.entity.Category;
import org.example.catalog.entity.Product;
import org.example.catalog.exception.DuplicateSkuException;
import org.example.catalog.exception.CategoryNotFoundException;
import org.example.catalog.exception.ProductNotFoundException;
import org.example.catalog.repository.CategoryRepository;
import org.example.catalog.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public Page<ProductResponse> findAll(ProductFilterRequest request, Pageable pageable) {
        return productRepository.findAll(buildSpec(request), pageable)
                .map(ProductResponse::fromEntity);
    }

    public ProductResponse findById(UUID id) {
        return productRepository.findById(id)
                .map(ProductResponse::fromEntity)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public ProductResponse findBySku(String sku) {
        return productRepository.findBySku(sku)
                .map(ProductResponse::fromEntity)
                .orElseThrow(() -> new ProductNotFoundException("sku", sku));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
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

        return ProductResponse.fromEntity(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = getProductOrThrow(id);

        if (!product.getSku().equals(request.sku()) && productRepository.existsBySku(request.sku())) {
            throw new DuplicateSkuException(request.sku());
        }

        product.setSku(request.sku());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setCurrency(request.currency() != null ? request.currency() : "EUR");
        product.setAvailable(request.available());
        product.setCategory(resolveCategory(request.categoryId()));

        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public ProductResponse patch(UUID id, ProductPatchRequest request) {
        Product product = getProductOrThrow(id);

        if (request.sku() != null) {
            if (!product.getSku().equals(request.sku()) && productRepository.existsBySku(request.sku())) {
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

        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public void delete(UUID id) {
        Product product = getProductOrThrow(id);
        productRepository.delete(product);
    }

    private Product getProductOrThrow(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    private Category resolveCategory(UUID categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
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
