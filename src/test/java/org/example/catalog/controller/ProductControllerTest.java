package org.example.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.catalog.common.GlobalExceptionHandler;
import org.example.catalog.dto.*;
import org.example.catalog.exception.DuplicateSkuException;
import org.example.catalog.exception.ProductNotFoundException;
import org.example.catalog.security.Http401UnauthorizedEntryPoint;
import org.example.catalog.security.JwtAuthenticationFilter;
import org.example.catalog.security.JwtService;
import org.example.catalog.security.SecurityConfig;
import org.example.catalog.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class,
        JwtAuthenticationFilter.class, Http401UnauthorizedEntryPoint.class})
@DisplayName("ProductController slice tests")
class ProductControllerTest {

    @Autowired
    MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    ProductService productService;
    @MockitoBean
    JwtService jwtService;
    @MockitoBean
    ObjectMapper objectMapper;

    private UUID productId;
    private ProductResponse sampleResponse;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        sampleResponse = ProductResponse.builder()
                .id(productId)
                .sku("ABC-123")
                .name("Test Product")
                .description("A test product")
                .price(new BigDecimal("99.99"))
                .currency("EUR")
                .available(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @WithMockUser
    @DisplayName("GET /products — returns 200 with page")
    void getAll_returns200() throws Exception {
        var page = new PageImpl<>(List.of(sampleResponse), PageRequest.of(0, 10), 1);
        when(productService.findAll(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("ABC-123"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /products — filter params passed correctly")
    void getAll_withFilters_passes() throws Exception {
        var page = new PageImpl<>(List.of(sampleResponse));
        when(productService.findAll(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/products")
                        .param("minPrice", "10.00")
                        .param("maxPrice", "200.00")
                        .param("available", "true"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /products/{id} — found — returns 200")
    void getById_found_returns200() throws Exception {
        when(productService.findById(productId)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId.toString()))
                .andExpect(jsonPath("$.sku").value("ABC-123"))
                .andExpect(jsonPath("$.price").value(99.99));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /products/{id} — not found — returns 404")
    void getById_notFound_returns404() throws Exception {
        when(productService.findById(productId)).thenThrow(new ProductNotFoundException(productId));

        mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /products/sku/{sku} — found — returns 200")
    void getBySku_found_returns200() throws Exception {
        when(productService.findBySku("ABC-123")).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/products/sku/{sku}", "ABC-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("ABC-123"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /products/sku/{sku} — not found — returns 404")
    void getBySku_notFound_returns404() throws Exception {
        when(productService.findBySku("MISSING")).thenThrow(new ProductNotFoundException("sku", "MISSING"));

        mockMvc.perform(get("/api/v1/products/sku/{sku}", "MISSING"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /products — valid body — returns 201")
    void create_valid_returns201() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .sku("NEW-001")
                .name("New Product")
                .price(new BigDecimal("49.99"))
                .available(true)
                .build();

        when(productService.create(any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("ABC-123"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /products — blank SKU — returns 400")
    void create_blankSku_returns400() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .sku("")
                .name("Name")
                .price(BigDecimal.TEN)
                .available(true)
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /products — invalid SKU pattern (lowercase) — returns 400")
    void create_invalidSkuPattern_returns400() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .sku("invalid-sku")
                .name("Name")
                .price(BigDecimal.TEN)
                .available(true)
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /products — negative price — returns 400")
    void create_negativePrice_returns400() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .sku("VALID-001")
                .name("Name")
                .price(new BigDecimal("-1.00"))
                .available(true)
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /products — duplicate SKU — returns 409")
    void create_duplicateSku_returns409() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .sku("ABC-123")
                .name("Dup")
                .price(BigDecimal.TEN)
                .available(true)
                .build();

        when(productService.create(any())).thenThrow(new DuplicateSkuException("ABC-123"));

        mockMvc.perform(post("/api/v1/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /products/{id} — valid — returns 200")
    void update_valid_returns200() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .sku("ABC-123")
                .name("Updated")
                .price(new BigDecimal("150.00"))
                .available(true)
                .build();

        when(productService.update(eq(productId), any())).thenReturn(sampleResponse);

        mockMvc.perform(put("/api/v1/products/{id}", productId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /products/{id} — not found — returns 404")
    void update_notFound_returns404() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .sku("ABC-123").name("X").price(BigDecimal.TEN).available(true).build();

        when(productService.update(eq(productId), any()))
                .thenThrow(new ProductNotFoundException(productId));

        mockMvc.perform(put("/api/v1/products/{id}", productId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /products/{id} — existing — returns 204")
    void delete_existing_returns204() throws Exception {
        doNothing().when(productService).delete(productId);

        mockMvc.perform(delete("/api/v1/products/{id}", productId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /products/{id} — not found — returns 404")
    void delete_notFound_returns404() throws Exception {
        doThrow(new ProductNotFoundException(productId)).when(productService).delete(productId);

        mockMvc.perform(delete("/api/v1/products/{id}", productId)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /products — unauthenticated — returns 401")
    void create_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
