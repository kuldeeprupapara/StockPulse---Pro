package com.stockmarket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmarket.kafka.dto.IndexQuoteKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer — bridge between Kafka and SSE.
 *
 * FLOW:
 * ─────────────────────────────────────────────────────────────────────────
 * Kafka delivers message → consume() fires → deserialize → SSE broadcast
 *
 * THREAD MODEL:
 * ─────────────────────────────────────────────────────────────────────────
 * concurrency = 3 (set in KafkaConsumerConfig)
 * Thread-1 → Partition 0 → ^NSEI messages
 * Thread-2 → Partition 1 → ^BSESN messages
 * Thread-3 → Partition 2 → ^NSEBANK messages
 * All 3 call this consume() method independently in parallel.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndexQuoteConsumer {

    private final SseEmitterService sseEmitterService;
    private final ObjectMapper      objectMapper;

    /**
     * Called automatically when Kafka delivers a new message.
     *
     * @param message  raw JSON String from Kafka partition
     *
     * WHY NOT RETHROW EXCEPTIONS?
     * ─────────────────────────────────────────────────────────────────────
     * Rethrowing causes Kafka to retry the same message endlessly
     * (poison pill problem) → consumer gets stuck on bad message.
     * We log and move on → consumer processes next message normally.
     */
    @KafkaListener(
            topics      = "${kafka.topic.index-quote:stock.quotes.index.v1}",
            groupId     = "${spring.kafka.consumer.group-id}",
            concurrency = "${kafka.consumer.concurrency:3}"
    )
    public void consume(String message) {
        try {
            // Step 1: JSON String → IndexQuoteEvent POJO
            IndexQuoteKafkaEvent event =
                    objectMapper.readValue(message, IndexQuoteKafkaEvent.class);

            log.debug("📨 Kafka received → symbol: {} | price: {} | state: {}",
                    event.getIndexSymbol(),
                    event.getPrice(),
                    event.getMarketState());

            // Step 2: Push to all connected Angular SSE clients
            // Angular listens: source.addEventListener("index-quote", ...)
            sseEmitterService.broadcast("index-quote", event);

            log.debug("📡 SSE broadcast done → symbol: {} | clients: {}",
                    event.getIndexSymbol(),
                    sseEmitterService.getConnectedClients());

        } catch (Exception e) {
            // Log and skip — do NOT rethrow (poison pill prevention)
            log.error("❌ Kafka consume failed | message: {} | error: {}",
                    message, e.getMessage());
        }
    }
}
