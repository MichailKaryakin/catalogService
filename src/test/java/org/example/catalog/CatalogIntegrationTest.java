package org.example.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.catalog.dto.ProductRequest;
import org.example.catalog.dto.ProductResponse;
import org.example.catalog.entity.Product;
import org.example.catalog.repository.ProductRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Catalog integration tests")
class CatalogIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("catalog_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
    );

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    ProductRepository productRepository;

    private static UUID createdProductId;

    @BeforeEach
    void cleanUp() {
        productRepository.deleteAll();
    }

    @Test
    @Order(1)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /products — creates product in DB")
    void createProduct_persistsToDb() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .sku("INT-001")
                .name("Integration Product")
                .description("Desc")
                .price(new BigDecimal("99.99"))
                .currency("EUR")
                .available(true)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("INT-001"))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andReturn();

        ProductResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), ProductResponse.class);
        createdProductId = response.id();

        assertThat(productRepository.findById(createdProductId)).isPresent();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /products — duplicate SKU — returns 409")
    void createProduct_duplicateSku_returns409() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .sku("DUP-001").name("First").price(BigDecimal.TEN).available(true).build();

        mockMvc.perform(post("/api/v1/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /products/{id} — existing — returns full response")
    void getById_existing_returnsProduct() throws Exception {
        UUID id = createProductInDb("GET-001", "Get Test", "29.99");

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.sku").value("GET-001"))
                .andExpect(jsonPath("$.price").value(29.99));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /products/{id} — missing — returns 404")
    void getById_missing_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /products/sku/{sku} — existing — returns product")
    void getBySku_existing_returnsProduct() throws Exception {
        createProductInDb("SKU-TEST", "Sku Product", "59.99");

        mockMvc.perform(get("/api/v1/products/sku/{sku}", "SKU-TEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-TEST"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /products — pagination — returns correct page size")
    void getAll_pagination_worksCorrectly() throws Exception {
        createProductInDb("PROD-A", "Product A", "10.00");
        createProductInDb("PROD-B", "Product B", "20.00");
        createProductInDb("PROD-C", "Product C", "30.00");

        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /products — minPrice filter — returns only matching products")
    void getAll_minPriceFilter_returnsFiltered() throws Exception {
        createProductInDb("CHEAP-1", "Cheap Product", "5.00");
        createProductInDb("EXP-1", "Expensive Product", "500.00");

        mockMvc.perform(get("/api/v1/products")
                        .param("minPrice", "100.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].sku").value("EXP-1"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /products — text search — matches name and description")
    void getAll_textSearch_matchesNameAndDescription() throws Exception {
        createProductInDb("SRCH-1", "Laptop Pro", "999.00");
        createProductInDb("SRCH-2", "Wireless Mouse", "49.00");

        mockMvc.perform(get("/api/v1/products")
                        .param("query", "laptop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].sku").value("SRCH-1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /products/{id} — updates and persists")
    void update_valid_persistsChanges() throws Exception {
        UUID id = createProductInDb("UPD-001", "Original", "10.00");

        ProductRequest updated = ProductRequest.builder()
                .sku("UPD-001")
                .name("Updated Name")
                .price(new BigDecimal("99.00"))
                .available(false)
                .build();

        mockMvc.perform(put("/api/v1/products/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.price").value(99.00))
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /products/{id} — removes from DB")
    void delete_existing_removesFromDb() throws Exception {
        UUID id = createProductInDb("DEL-001", "Delete Me", "1.00");

        mockMvc.perform(delete("/api/v1/products/{id}", id).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(productRepository.findById(id)).isEmpty();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /products/{id} — already deleted — returns 404")
    void delete_alreadyDeleted_returns404() throws Exception {
        UUID id = createProductInDb("DEL-002", "Delete Me 2", "1.00");

        mockMvc.perform(delete("/api/v1/products/{id}", id).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/products/{id}", id).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /products — missing name — returns 400")
    void create_missingName_returns400() throws Exception {
        String body = """
                {"sku":"VALID-001","price":10.00,"available":true}
                """;

        mockMvc.perform(post("/api/v1/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    private UUID createProductInDb(String sku, String name, String price) {
        Product product = Product.builder()
                .sku(sku)
                .name(name)
                .price(new BigDecimal(price))
                .available(true)
                .build();
        return productRepository.save(product).getId();
    }
}
