package org.example.catalog.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic productCreatedTopic() {
        return TopicBuilder.name("catalog.product.created")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic productUpdatedTopic() {
        return TopicBuilder.name("catalog.product.updated")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic productDeletedTopic() {
        return TopicBuilder.name("catalog.product.deleted")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderStockReserveTopic() {
        return TopicBuilder.name("order.stock.reserve")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic stockReserveResultTopic() {
        return TopicBuilder.name("stock.reserve.result")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
