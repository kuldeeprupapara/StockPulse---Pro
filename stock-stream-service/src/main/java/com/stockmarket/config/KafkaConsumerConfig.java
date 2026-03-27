package com.stockmarket.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.Map;

/**
 * Kafka Consumer Configuration.
 *
 * ── WHY @EnableKafka? ────────────────────────────────────────────────────
 * @EnableKafka activates Spring's @KafkaListener infrastructure.
 * WITHOUT it → @KafkaListener in IndexQuoteConsumer is SILENTLY IGNORED.
 * No error thrown — messages just never arrive. Always add this.
 *
 * ── THREE BEANS NEEDED FOR KAFKA CONSUMER ────────────────────────────────
 *
 *  1. ConsumerFactory
 *     └── Creates raw Kafka consumers with our config
 *         (broker address, group-id, deserializers)
 *
 *  2. ConcurrentKafkaListenerContainerFactory
 *     └── Wraps ConsumerFactory
 *         Creates listener containers for @KafkaListener methods
 *         Manages concurrency (threads per partition)
 *
 *  3. @KafkaListener (in IndexQuoteConsumer)
 *     └── Actual method that processes each Kafka message
 *         Runs in container created by factory above
 *
 * ── CONSUMER GROUP AND PARTITION ASSIGNMENT ──────────────────────────────
 *
 *  Topic "index-quote-updates" has 3 partitions:
 *  ┌─────────────┬─────────────┬─────────────┐
 *  │ Partition 0 │ Partition 1 │ Partition 2 │
 *  │  ^NSEI msgs │  ^BSESN msgs│ ^NSEBANK msg│
 *  └─────────────┴─────────────┴─────────────┘
 *
 *  concurrency = 3 creates 3 threads:
 *  Thread-1 → reads Partition 0 → processes ^NSEI
 *  Thread-2 → reads Partition 1 → processes ^BSESN
 *  Thread-3 → reads Partition 2 → processes ^NSEBANK
 *
 *  All 3 run IndexQuoteConsumer.consume() in parallel ✅
 *
 * ── OFFSET MANAGEMENT ────────────────────────────────────────────────────
 *
 *  Offset = position of last consumed message per partition
 *  Kafka stores offset per (group-id + partition)
 *
 *  Example:
 *    Partition 0: offset 142 → consumer has read 142 messages
 *    Service restarts → resumes from offset 143 (no data loss)
 *
 *  auto-offset-reset = "latest":
 *    First time this group connects → start from newest message
 *    Already consumed → resume from stored offset
 */
@EnableKafka       // ✅ Activates @KafkaListener — MUST have this
@Configuration
public class KafkaConsumerConfig {

    /*
     * Read from application.properties:
     * spring.kafka.bootstrap-servers=localhost:9092
     * Must match same broker that stock-query-service produces to.
     */
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /*
     * Read from application.properties:
     * spring.kafka.consumer.group-id=stockpulse-sse-group
     */
    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    /*
     * Read from application.properties:
     * spring.kafka.consumer.auto-offset-reset=latest
     */
    @Value("${spring.kafka.consumer.auto-offset-reset:latest}")
    private String autoOffsetReset;

    /*
     * Read from application.properties:
     * spring.kafka.consumer.max-poll-records=10
     */
    @Value("${spring.kafka.consumer.max-poll-records:10}")
    private String maxPollRecords;

    /**
     * ConsumerFactory — creates configured Kafka consumer instances.
     *
     * CONFIG EXPLAINED:
     * ─────────────────────────────────────────────────────────────────────
     * BOOTSTRAP_SERVERS_CONFIG
     *   → Kafka broker address. Consumer connects here to fetch messages.
     *
     * GROUP_ID_CONFIG
     *   → Identifies consumer group. Kafka tracks offset per group.
     *   → Multiple instances with same group = partition sharing (scaling).
     *
     * KEY_DESERIALIZER_CLASS_CONFIG
     *   → Producer used StringSerializer for key → we use StringDeserializer
     *   → Converts bytes back to String e.g. "^NSEI"
     *
     * VALUE_DESERIALIZER_CLASS_CONFIG
     *   → Producer used StringSerializer for value → we use StringDeserializer
     *   → Converts bytes back to JSON String
     *   → We then manually parse JSON → IndexQuoteEvent in consumer
     *
     * AUTO_OFFSET_RESET_CONFIG
     *   → "latest"   = only new messages (production setting)
     *   → "earliest" = all messages from start (debugging setting)
     *
     * MAX_POLL_RECORDS_CONFIG
     *   → Max messages per poll cycle
     *   → Low value (10) = low latency = better for real-time SSE
     *   → High value (500) = high throughput = better for batch processing
     *
     * SESSION_TIMEOUT_MS_CONFIG
     *   → How long broker waits for heartbeat before marking consumer dead
     *   → If consumer dies → broker reassigns its partitions to other consumers
     *
     * HEARTBEAT_INTERVAL_MS_CONFIG
     *   → How often consumer sends "I am alive" signal to broker
     *   → Must be less than session timeout (typically 1/3 of session timeout)
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers,           // localhost:9092
                ConsumerConfig.GROUP_ID_CONFIG,
                groupId,                    // stockpulse-sse-group
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class,   // key = "^NSEI" String
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class,   // value = JSON String
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                autoOffsetReset,            // latest
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
                maxPollRecords,             // 10 — low latency for SSE
                ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG,
                "15000",                    // 15s — mark dead after 15s no heartbeat
                ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG,
                "5000"                      // 5s — send heartbeat every 5s
        ));
    }

    /**
     * ConcurrentKafkaListenerContainerFactory
     * ─────────────────────────────────────────────────────────────────────
     * Creates the CONTAINER that manages @KafkaListener method execution.
     *
     * setConcurrency(3):
     *   Creates 3 consumer threads — one per partition.
     *   Each thread independently polls its assigned partition.
     *   All 3 call IndexQuoteConsumer.consume() in parallel.
     *   ⚠️ Never set higher than partition count (3 here).
     *
     * setAutoStartup(true):
     *   Starts consuming as soon as application boots up.
     *   No manual start needed.
     *
     * AckMode.RECORD:
     *   Commit offset after EACH message is processed successfully.
     *   Ensures no message loss if service crashes mid-processing.
     *   For SSE use case this is safe and recommended.
     *
     *   Other modes:
     *   BATCH  → commit after all records in poll batch processed
     *   TIME   → commit every N milliseconds
     *   MANUAL → developer calls Acknowledgment.acknowledge() manually
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
    kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        // Attach our consumer factory
        factory.setConsumerFactory(consumerFactory());

        // 3 threads = 3 partitions in "index-quote-updates" topic
        factory.setConcurrency(3);

        // Commit offset after each record — no message loss on crash
        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.RECORD);

        // Start consuming immediately on app startup
        factory.setAutoStartup(true);

        return factory;
    }
}
