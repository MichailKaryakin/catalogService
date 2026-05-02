package org.example.catalog.service;

import org.example.catalog.dto.CategoryRequest;
import org.example.catalog.dto.CategoryResponse;
import org.example.catalog.entity.Category;
import org.example.catalog.exception.CategoryNotFoundException;
import org.example.catalog.exception.DuplicateCategoryNameException;
import org.example.catalog.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService unit tests")
class CategoryServiceTest {

    @Mock CategoryRepository categoryRepository;

    @InjectMocks CategoryService categoryService;

    private UUID categoryId;
    private Category category;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        category = Category.builder()
                .id(categoryId)
                .name("Electronics")
                .build();
    }

    @Test
    @DisplayName("findAll: returns all categories as responses")
    void findAll_returnsAll() {
        Category child = Category.builder()
                .id(UUID.randomUUID())
                .name("Phones")
                .parent(category)
                .build();

        when(categoryRepository.findAll()).thenReturn(List.of(category, child));

        List<CategoryResponse> result = categoryService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(CategoryResponse::name)
                .containsExactlyInAnyOrder("Electronics", "Phones");
    }

    @Test
    @DisplayName("findAll: empty DB — returns empty list")
    void findAll_empty_returnsEmptyList() {
        when(categoryRepository.findAll()).thenReturn(List.of());

        List<CategoryResponse> result = categoryService.findAll();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("create: valid, no parent — saves and returns response")
    void create_noParent_saves() {
        CategoryRequest request = new CategoryRequest("Electronics", null);

        when(categoryRepository.existsByName("Electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryResponse result = categoryService.create(request);

        assertThat(result.name()).isEqualTo("Electronics");
        assertThat(result.parentId()).isNull();
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("create: with valid parent — saves with parent reference")
    void create_withParent_savesWithParent() {
        UUID parentId = categoryId;
        Category child = Category.builder()
                .id(UUID.randomUUID())
                .name("Phones")
                .parent(category)
                .build();

        CategoryRequest request = new CategoryRequest("Phones", parentId);

        when(categoryRepository.existsByName("Phones")).thenReturn(false);
        when(categoryRepository.findById(parentId)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(child);

        CategoryResponse result = categoryService.create(request);

        assertThat(result.name()).isEqualTo("Phones");
        assertThat(result.parentId()).isEqualTo(parentId);
    }

    @Test
    @DisplayName("create: duplicate name — throws DuplicateCategoryNameException")
    void create_duplicateName_throws() {
        CategoryRequest request = new CategoryRequest("Electronics", null);

        when(categoryRepository.existsByName("Electronics")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(DuplicateCategoryNameException.class);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("create: parent not found — throws CategoryNotFoundException")
    void create_parentNotFound_throws() {
        UUID missingParentId = UUID.randomUUID();
        CategoryRequest request = new CategoryRequest("Phones", missingParentId);

        when(categoryRepository.existsByName("Phones")).thenReturn(false);
        when(categoryRepository.findById(missingParentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(CategoryNotFoundException.class);

        verify(categoryRepository, never()).save(any());
    }
}
