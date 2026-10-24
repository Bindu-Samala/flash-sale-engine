package com.engineering.flashsale.worker;

import com.engineering.flashsale.entity.OutboxEvent;
import com.engineering.flashsale.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxRepo;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String TOPIC = "flashsale-order-events";

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishEvents() {
        List<OutboxEvent> pendingEvents = outboxRepo.findTop100ByStatusOrderByCreatedAtAsc("PENDING");

        for (OutboxEvent event : pendingEvents) {
            kafkaTemplate.send(TOPIC, event.getAggregateId().toString(), event.getPayload())
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        event.setStatus("PUBLISHED");
                        outboxRepo.save(event);
                    } else {
                        log.error("Failed to publish event ID: {}", event.getId(), ex);
                    }
                });
        }
    }
}