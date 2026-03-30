package org.example.catalog.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.micrometer.core.instrument.MeterRegistry;
import org.example.catalog.cache.ProductCacheService;
import org.example.catalog.repository.StockRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockSyncScheduler {

    private final StockRepository stockRepository;
    private final ProductCacheService cacheService;
    private final MeterRegistry meterRegistry;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void syncStocks() {
        log.info("Starting scheduled stock sync");

        try {
            long total = stockRepository.count();
            long outOfStock = stockRepository.findAll().stream()
                    .filter(s -> s.getQuantity() - s.getReserved() <= 0)
                    .count();

            meterRegistry.gauge("catalog.stock.total", total);
            meterRegistry.gauge("catalog.stock.out_of_stock", outOfStock);

            log.info("Stock sync completed: total={}, outOfStock={}", total, outOfStock);
        } catch (Exception e) {
            log.error("Stock sync failed: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 300000)
    public void evictStaleCache() {
        log.info("Starting scheduled cache eviction");

        try {
            cacheService.evictAllProductsLists();
            log.info("Stale product list caches evicted");
        } catch (Exception e) {
            log.error("Cache eviction failed: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void recordStockMetrics() {
        log.info("Recording stock metrics");

        try {
            long total = stockRepository.count();
            long lowStock = stockRepository.findAll().stream()
                    .filter(s -> s.getQuantity() - s.getReserved() <= 5)
                    .count();

            meterRegistry.gauge("catalog.stock.low_stock", lowStock);
            log.info("Stock metrics recorded: total={}, lowStock={}", total, lowStock);
        } catch (Exception e) {
            log.error("Stock metrics recording failed: {}", e.getMessage());
        }
    }
}
