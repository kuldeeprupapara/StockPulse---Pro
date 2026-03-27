package com.stockmarket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmarket.config.StreamScreenType;
import com.stockmarket.config.StreamSubscriptionProperties;
import com.stockmarket.kafka.dto.IndexQuoteKafkaEvent;
import com.stockmarket.util.CommonMarketUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE Emitter Service — manages all connected Angular clients.
 *
 * WHAT IS SSE?
 * ─────────────────────────────────────────────────────────────────────────
 * Server-Sent Events = one-way HTTP streaming (server → client).
 * Angular opens one connection → server pushes data anytime.
 * No polling needed. No WebSocket complexity needed.
 *
 * HOW MULTIPLE CLIENTS ARE HANDLED:
 * ─────────────────────────────────────────────────────────────────────────
 * Each Angular tab that opens EventSource → calls register() → new SseEmitter
 * All emitters stored in CopyOnWriteArrayList (thread-safe).
 * broadcast() iterates ALL emitters → sends same data to every client.
 *
 * Example: 5 users watching → 5 emitters → all get same price update ✅
 */
@Slf4j
@Service
public class SseEmitterService {

    private static final Map<String, String> COUNTRY_BY_EXCHANGE = Map.of(
            "NSE", "IN",
            "BSE", "IN",
            "NYQ", "US",
            "NMS", "US",
            "SHH", "CN",
            "SHZ", "CN"
    );

    /*
     * Thread-safe list of all active Angular SSE connections.
     * CopyOnWriteArrayList chosen because:
     * - Kafka thread reads/iterates during broadcast()
     * - HTTP thread writes during register()
     * - Regular ArrayList = ConcurrentModificationException
     * - CopyOnWriteArrayList = safe concurrent read + write
     */
    private final List<Subscription> subscriptions = new CopyOnWriteArrayList<>();

    private final ObjectMapper objectMapper;
    private final StreamSubscriptionProperties subscriptionProperties;

    public SseEmitterService(ObjectMapper objectMapper, StreamSubscriptionProperties subscriptionProperties) {
        this.objectMapper = objectMapper;
        this.subscriptionProperties = subscriptionProperties;
    }

    /**
     * Register new Angular client when EventSource connects.
     *
     * Long.MAX_VALUE timeout:
     *   Keeps connection open indefinitely.
     *   Default is 30s which causes auto-disconnect.
     *   Angular EventSource auto-reconnects anyway but
     *   Long.MAX_VALUE avoids unnecessary reconnects.
     */
    public SseEmitter register() {
        return registerWithFilter(null, null, null, null);
    }

    public SseEmitter registerWithFilter(String countryCode, String exchange) {
        return registerWithFilter(countryCode, exchange, null, null);
    }

    public SseEmitter registerWithFilter(String countryCode, String exchange, String screenType, String symbols) {
        StreamScreenType resolvedScreenType = StreamScreenType.from(screenType);
        Set<String> normalizedSymbols = normalizeAndCapSymbols(symbols, resolvedScreenType);

        SseEmitter emitter = new SseEmitter(subscriptionProperties.getEmitterTimeoutMs());
        Subscription subscription = new Subscription(
                emitter,
                CommonMarketUtils.normalizeCountryCode(countryCode),
                CommonMarketUtils.normalizeExchange(exchange),
                resolvedScreenType,
                normalizedSymbols
        );

        // Remove from list when Angular closes tab
        emitter.onCompletion(() -> {
            subscriptions.remove(subscription);
            log.debug("SSE client disconnected. Active: {}", subscriptions.size());
        });

        // Remove on timeout (rare with Long.MAX_VALUE)
        emitter.onTimeout(() -> {
            subscriptions.remove(subscription);
            log.debug("SSE client timed out. Active: {}", subscriptions.size());
        });

        // Remove on network error
        emitter.onError(e -> {
            subscriptions.remove(subscription);
            log.debug("SSE client error. Active: {}", subscriptions.size());
        });

        subscriptions.add(subscription);
        log.info(
                "SSE client registered. active={}, screenType={}, symbolCount={}, countryCode={}, exchange={}",
                subscriptions.size(),
                resolvedScreenType,
                normalizedSymbols.size(),
                subscription.countryCode(),
                subscription.exchange()
        );

        sendSubscriptionAck(subscription, symbols, normalizedSymbols.size());
        return emitter;
    }

    /**
     * Broadcast event to ALL connected Angular clients.
     * Called by IndexQuoteConsumer on every Kafka message.
     *
     * Dead emitter detection:
     *   send() throws IOException when connection is broken.
     *   We collect dead emitters and remove after loop.
     *   Cannot remove inside forEach → ConcurrentModificationException.
     */
    public void broadcast(String eventName, IndexQuoteKafkaEvent data) {
        if (subscriptions.isEmpty()) {
            log.debug("No SSE clients — skipping broadcast: {}", eventName);
            return;
        }

        String eventExchange = CommonMarketUtils.normalizeExchange(data.getExchange());
        String eventCountryCode = resolveEventCountryCode(data, eventExchange);
        String eventSymbol = normalizeSymbolToken(data.getIndexSymbol());
        List<Subscription> dead = new CopyOnWriteArrayList<>();

        subscriptions.forEach(subscription -> {
            try {
                if (!matches(subscription, eventCountryCode, eventExchange, eventSymbol)) {
                    return;
                }
                subscription.emitter().send(
                        SseEmitter.event()
                                .name(eventName)  // Angular: addEventListener("index-quote")
                                .data(objectMapper.writeValueAsString(data))
                );
            } catch (IOException e) {
                dead.add(subscription); // broken connection
            } catch (Exception e) {
                log.error("SSE send error: {}", e.getMessage());
                dead.add(subscription);
            }
        });

        // Cleanup dead connections
        if (!dead.isEmpty()) {
            subscriptions.removeAll(dead);
            log.debug("Removed {} dead emitters. Active: {}",
                    dead.size(), subscriptions.size());
        }
    }

    public int getConnectedClients() {
        return subscriptions.size();
    }

    private boolean matches(Subscription subscription, String eventCountryCode, String eventExchange, String eventSymbol) {
        if (subscription.exchange() != null && !subscription.exchange().equals(eventExchange)) {
            return false;
        }

        if (subscription.countryCode() != null && !subscription.countryCode().equals(eventCountryCode)) {
            return false;
        }

        if (subscription.symbols().isEmpty()) {
            return true;
        }

        if (eventSymbol == null) {
            return false;
        }

        return subscription.symbols().contains(eventSymbol)
                || subscription.symbols().contains(stripIndexPrefix(eventSymbol));
    }

    private String resolveEventCountryCode(IndexQuoteKafkaEvent event, String eventExchange) {
        String eventCountryCode = CommonMarketUtils.normalizeCountryCode(event.getCountryCode());
        if (eventCountryCode != null) {
            return eventCountryCode;
        }
        return resolveCountryCodeByExchange(eventExchange);
    }

    private String resolveCountryCodeByExchange(String exchange) {
        return CommonMarketUtils.resolveCountryCodeByExchange(exchange, COUNTRY_BY_EXCHANGE);
    }

    private Set<String> normalizeAndCapSymbols(String symbols, StreamScreenType screenType) {
        if (symbols == null || symbols.isBlank()) {
            return Set.of();
        }

        int maxSymbols = Math.max(1, subscriptionProperties.maxSymbolsForScreen(screenType));
        Set<String> normalized = Arrays.stream(symbols.split(","))
                .map(this::normalizeSymbolToken)
                .filter(token -> token != null && !token.isBlank())
                .limit(maxSymbols)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        return Set.copyOf(normalized);
    }

    private String normalizeSymbolToken(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return null;
        }
        return symbol.trim().toUpperCase();
    }

    private String stripIndexPrefix(String symbol) {
        if (symbol == null) {
            return null;
        }
        return symbol.startsWith("^") ? symbol.substring(1) : symbol;
    }

    private void sendSubscriptionAck(Subscription subscription, String requestedSymbols, int acceptedSymbolCount) {
        try {
            int requestedSymbolCount = countRequestedSymbols(requestedSymbols);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("screenType", subscription.screenType().name());
            payload.put("countryCode", subscription.countryCode());
            payload.put("exchange", subscription.exchange());
            payload.put("requestedSymbolCount", requestedSymbolCount);
            payload.put("acceptedSymbolCount", acceptedSymbolCount);
            payload.put("acceptedSymbols", subscription.symbols());

            subscription.emitter().send(
                    SseEmitter.event()
                            .name("subscription-ack")
                            .data(objectMapper.writeValueAsString(payload))
            );
        } catch (Exception ex) {
            subscriptions.remove(subscription);
            subscription.emitter().completeWithError(ex);
            log.warn("Failed to send subscription ack. Closing connection.", ex);
        }
    }

    private int countRequestedSymbols(String requestedSymbols) {
        if (requestedSymbols == null || requestedSymbols.isBlank()) {
            return 0;
        }
        return (int) Arrays.stream(requestedSymbols.split(","))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .count();
    }

    private record Subscription(
            SseEmitter emitter,
            String countryCode,
            String exchange,
            StreamScreenType screenType,
            Set<String> symbols
    ) {
    }
}
