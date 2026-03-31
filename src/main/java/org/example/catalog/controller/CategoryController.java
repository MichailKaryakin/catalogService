package org.example.catalog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.catalog.dto.CategoryRequest;
import org.example.catalog.dto.CategoryResponse;
import org.example.catalog.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Categories", description = "Managing product categories")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Get all categories", description = "Returns flat list of all categories including parent references")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of categories")
    })
    @GetMapping
    public List<CategoryResponse> getAll() {
        return categoryService.findAll();
    }

    @Operation(summary = "Create category", description = "Creates a new category, optionally nested under a parent")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Category created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Category name already exists")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        return categoryService.create(request);
    }
}
