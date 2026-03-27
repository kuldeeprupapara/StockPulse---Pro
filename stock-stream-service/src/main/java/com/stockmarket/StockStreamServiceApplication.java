package com.stockmarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for stock-stream-service.
 *
 * RESPONSIBILITIES:
 * ─────────────────────────────────────────────────────────────────────────
 * ✅ Kafka Consumer  — listens to "index-quote-updates" topic
 * ✅ SSE Emitter     — pushes live data to Angular clients
 * ✅ REST endpoint   — GET /api/stream/indices
 *
 * NOT RESPONSIBLE FOR:
 * ─────────────────────────────────────────────────────────────────────────
 * ❌ No DB connection
 * ❌ No Yahoo Finance API calls
 * ❌ No Kafka producing
 * ❌ No scheduling
 *
 * PORTS:
 *   stock-query-service  → 8080
 *   stock-stream-service → 8081  (this service)
 *   Angular              → 4200
 *   Kafka broker         → 9092
 */
@SpringBootApplication
public class StockStreamServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(StockStreamServiceApplication.class, args);
	}

}
