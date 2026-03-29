package org.example.catalog.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catalog.dto.StockReserveRequest;
import org.example.catalog.exception.StockOperationException;
import org.example.catalog.kafka.dto.OrderStockItem;
import org.example.catalog.kafka.dto.OrderStockReserveEvent;
import org.example.catalog.kafka.dto.StockReserveItemResult;
import org.example.catalog.kafka.dto.StockReserveResultEvent;
import org.example.catalog.service.StockService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockReserveConsumer {

    private static final String RESULT_TOPIC = "stock.reserve.result";

    private final StockService stockService;
    private final KafkaTemplate<String, StockReserveResultEvent> resultTemplate;

    @KafkaListener(topics = "order.stock.reserve", groupId = "catalog-stock-reserve")
    public void handle(OrderStockReserveEvent event) {
        log.info("Received order.stock.reserve event, orderId={}", event.orderId());

        List<StockReserveItemResult> results = new ArrayList<>();

        for (OrderStockItem item : event.items()) {
            try {
                stockService.reserve(new StockReserveRequest(
                        event.orderId(),
                        item.productId(),
                        item.sku(),
                        item.quantity()
                ));
                results.add(StockReserveItemResult.success(
                        item.productId(), item.sku(), item.quantity()));
                log.info("Reserved productId={}, qty={}, orderId={}",
                        item.productId(), item.quantity(), event.orderId());
            } catch (StockOperationException e) {
                results.add(StockReserveItemResult.failure(
                        item.productId(), item.sku(), item.quantity(), e.getMessage()));
                log.warn("Failed to reserve productId={}, orderId={}: {}",
                        item.productId(), event.orderId(), e.getMessage());
            }
        }

        StockReserveResultEvent result = StockReserveResultEvent.of(event.orderId(), results);
        resultTemplate.send(RESULT_TOPIC, event.orderId().toString(), result);
        log.info("Sent stock.reserve.result for orderId={}", event.orderId());
    }
}
