package org.example.catalog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catalog.dto.CategoryRequest;
import org.example.catalog.dto.CategoryResponse;
import org.example.catalog.entity.Category;
import org.example.catalog.exception.CategoryNotFoundException;
import org.example.catalog.exception.DuplicateCategoryNameException;
import org.example.catalog.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> findAll() {
        log.debug("Fetching all categories");
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::fromEntity)
                .toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        log.info("Creating category: {}", request.name());
        if (categoryRepository.existsByName(request.name())) {
            log.warn("Duplicate category name: {}", request.name());
            throw new DuplicateCategoryNameException(request.name());
        }

        Category category = Category.builder()
                .name(request.name())
                .parent(resolveParent(request.parentId()))
                .build();

        CategoryResponse response = CategoryResponse.fromEntity(categoryRepository.save(category));
        log.info("Category created: id={}, name={}", response.id(), response.name());
        return response;
    }

    private Category resolveParent(UUID parentId) {
        if (parentId == null) return null;
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> {
                    log.warn("Parent category not found: {}", parentId);
                    return new CategoryNotFoundException(parentId);
                });
    }
}
