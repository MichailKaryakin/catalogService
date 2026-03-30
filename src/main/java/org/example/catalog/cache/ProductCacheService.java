package org.example.catalog.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catalog.dto.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCacheService {

    private static final String PRODUCT_KEY_PREFIX = "catalog:product:";
    private static final String PRODUCTS_LIST_PREFIX = "catalog:products:";
    private static final Duration PRODUCT_TTL = Duration.ofMinutes(30);
    private static final Duration PRODUCTS_LIST_TTL = Duration.ofMinutes(5);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<ProductResponse> getProduct(UUID id) {
        String key = PRODUCT_KEY_PREFIX + id;
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) return Optional.empty();
            return Optional.of(objectMapper.readValue(json, ProductResponse.class));
        } catch (Exception e) {
            logCacheError("read", key, e);
            return Optional.empty();
        }
    }

    public void putProduct(ProductResponse product) {
        String key = PRODUCT_KEY_PREFIX + product.id();
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(product), PRODUCT_TTL);
        } catch (Exception e) {
            logCacheError("write", key, e);
        }
    }

    public void evictProduct(UUID id) {
        redisTemplate.delete(PRODUCT_KEY_PREFIX + id);
    }

    public Optional<String> getProductsList(String queryHash) {
        String key = PRODUCTS_LIST_PREFIX + queryHash;
        try {
            String json = redisTemplate.opsForValue().get(key);
            return Optional.ofNullable(json);
        } catch (Exception e) {
            logCacheError("read", key, e);
            return Optional.empty();
        }
    }

    public void putProductsList(String queryHash, Object page) {
        String key = PRODUCTS_LIST_PREFIX + queryHash;
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(page), PRODUCTS_LIST_TTL);
        } catch (Exception e) {
            logCacheError("write", key, e);
        }
    }

    public void evictAllProductsLists() {
        try {
            var keys = redisTemplate.keys(PRODUCTS_LIST_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys);
        } catch (Exception e) {
            logCacheError("evict all", PRODUCTS_LIST_PREFIX, e);
        }
    }

    public Page<ProductResponse> deserializePage(String json) throws Exception {
        return objectMapper.readValue(json,
                objectMapper.getTypeFactory().constructParametricType(
                        org.springframework.data.domain.PageImpl.class,
                        ProductResponse.class
                )
        );
    }

    private void logCacheError(String operation, String key, Exception e) {
        log.warn("Cache {} failed for key={}: {}", operation, key, e.getMessage());
    }
}
