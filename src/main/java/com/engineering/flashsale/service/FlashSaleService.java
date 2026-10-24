package com.engineering.flashsale.service;

import com.engineering.flashsale.entity.Order;
import com.engineering.flashsale.entity.OutboxEvent;
import com.engineering.flashsale.entity.Product;
import com.engineering.flashsale.repository.OrderRepository;
import com.engineering.flashsale.repository.OutboxEventRepository;
import com.engineering.flashsale.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class FlashSaleService {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> decrementStockScript;
    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ProductRepository productRepository;

    /**
     * Pre-warms the Redis cache by loading inventory from PostgreSQL.
     * Call this before starting the flash sale.
     */
    @Transactional(readOnly = true)
    public void initializeFlashSale(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        String stockKey = "product:" + productId + ":stock";
        redisTemplate.opsForValue().set(stockKey, String.valueOf(product.getAvailableStock()));
    }

    /**
     * Atomically reserves product stock via Redis and persists the order
     * along with an outbox event in PostgreSQL.
     */
    @Transactional
    public String reserveProduct(Long userId, Long productId, int quantity) {
        String stockKey = "product:" + productId + ":stock";

        // Step 1: Atomic In-Memory Decrement via Redis Lua
        Long stockResult = redisTemplate.execute(
                decrementStockScript,
                Collections.singletonList(stockKey),
                String.valueOf(quantity)
        );

        if (stockResult == null || stockResult == -1) {
            throw new IllegalArgumentException("Target product not found or sale inactive.");
        }
        if (stockResult == 0) {
            return "OUT_OF_STOCK";
        }

        // Step 2: Atomic ACID Persist of Order and Outbox Event
        Order order = Order.builder()
                .userId(userId)
                .productId(productId)
                .quantity(quantity)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
        orderRepository.save(order);

        String jsonPayload = String.format("{\"orderId\":%d,\"userId\":%d,\"productId\":%d,\"quantity\":%d}",
                order.getId(), userId, productId, quantity);

        OutboxEvent outbox = OutboxEvent.builder()
                .aggregateType("ORDER")
                .aggregateId(order.getId())
                .payload(jsonPayload)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
        outboxEventRepository.save(outbox);

        return "SUCCESS";
    }
}