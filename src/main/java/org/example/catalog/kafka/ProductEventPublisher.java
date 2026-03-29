package org.example.catalog.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catalog.kafka.dto.ProductEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventPublisher {

    private final KafkaTemplate<String, ProductEvent> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductEvent(ProductEvent event) {
        String topic = switch (event.eventType()) {
            case "PRODUCT_CREATED" -> "catalog.product.created";
            case "PRODUCT_UPDATED" -> "catalog.product.updated";
            case "PRODUCT_DELETED" -> "catalog.product.deleted";
            default -> throw new IllegalArgumentException("Unknown event type: " + event.eventType());
        };

        kafkaTemplate.send(topic, event.productId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error(
                                "Failed to send event {} for productId={}: {}",
                                event.eventType(), event.productId(), ex.getMessage());
                    } else {
                        log.info(
                                "Event {} sent to topic={}, productId={}",
                                event.eventType(), topic, event.productId()
                        );
                    }
                });
    }
}
