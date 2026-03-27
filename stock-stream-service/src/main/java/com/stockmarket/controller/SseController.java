package com.stockmarket.controller;

import com.stockmarket.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE REST Controller.
 *
 * ENDPOINTS:
 * ─────────────────────────────────────────────────────────────────────────
 * GET /api/stream/indices
 *   → Angular connects here via EventSource
 *   → HTTP connection stays open (SSE stream)
 *   → Receives "index-quote" events with live price data
 *
 * GET /api/stream/indices/clients
 *   → Returns count of currently connected Angular clients
 *   → Useful for monitoring dashboard
 *
 * ANGULAR USAGE:
 * ─────────────────────────────────────────────────────────────────────────
 * const source = new EventSource(
 *   'http://localhost:8081/api/stream/indices'
 * );
 * source.addEventListener('index-quote', (e) => {
 *   const data = JSON.parse(e.data);
 *   // update price, change, sparkline
 * });
 */
@Slf4j
@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterService sseEmitterService;

    /*
     * MediaType.TEXT_EVENT_STREAM_VALUE = "text/event-stream"
     * Required Content-Type for SSE protocol.
     * Tells browser: keep this connection open and fire events.
     */
    @GetMapping(
            value    = "/indices",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter streamIndices(
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String exchange,
            @RequestParam(required = false) String screenType,
            @RequestParam(required = false) String symbols
    ) {
        log.info(
                "Angular SSE connection established. countryCode={}, exchange={}, screenType={}, symbols={}",
                countryCode,
                exchange,
                screenType,
                symbols
        );
        return registerStream(countryCode, exchange, screenType, symbols);
    }

    @GetMapping(
            value = "/stocks",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter streamStocks(
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String exchange,
            @RequestParam(required = false) String screenType,
            @RequestParam(required = false) String symbols
    ) {
        log.info(
                "Stock SSE connection established. countryCode={}, exchange={}, screenType={}, symbols={}",
                countryCode,
                exchange,
                screenType,
                symbols
        );
        return registerStream(countryCode, exchange, screenType, symbols);
    }

    @GetMapping("/indices/clients")
    public int connectedClients() {
        return sseEmitterService.getConnectedClients();
    }

    private SseEmitter registerStream(String countryCode, String exchange, String screenType, String symbols) {
        return sseEmitterService.registerWithFilter(countryCode, exchange, screenType, symbols);
    }
}
