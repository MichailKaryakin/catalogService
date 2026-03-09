package org.example.catalog.service;

import lombok.RequiredArgsConstructor;
import org.example.catalog.dto.CategoryRequest;
import org.example.catalog.dto.CategoryResponse;
import org.example.catalog.entity.Category;
import org.example.catalog.exception.CategoryNotFoundException;
import org.example.catalog.exception.DuplicateCategoryNameException;
import org.example.catalog.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::fromEntity)
                .toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new DuplicateCategoryNameException(request.name());
        }

        Category category = Category.builder()
                .name(request.name())
                .parent(resolveParent(request.parentId()))
                .build();

        return CategoryResponse.fromEntity(categoryRepository.save(category));
    }

    private Category resolveParent(java.util.UUID parentId) {
        if (parentId == null) return null;
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> new CategoryNotFoundException(parentId));
    }
}
