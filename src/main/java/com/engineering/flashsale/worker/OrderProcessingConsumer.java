package com.engineering.flashsale.worker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderProcessingConsumer {

    @KafkaListener(topics = "flashsale-order-events", groupId = "flash-sale-consumer-group")
    public void processOrderEvent(String message) {
        log.info("Kafka consumer received event for downstream fulfillment: {}", message);
    }
}