package com.stockmarket.query.service;

import com.stockmarket.dto.APIRespDTO;
import com.stockmarket.dto.SymbolReq;
import com.stockmarket.model.StockQuoteEvent;
import com.stockmarket.query.client.FetcherClient;
import com.stockmarket.query.dto.SymbolFetchRequest;
import com.stockmarket.query.entity.StockQuoteEvents;
import com.stockmarket.query.entity.Symbol;
import com.stockmarket.query.interfaces.StockQuoteProvider;
import com.stockmarket.query.repository.StockQuoteEventRepository;
import com.stockmarket.query.repository.SymbolRepository;
import com.stockmarket.response.MarketOverviewDTO;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketOverviewService implements StockQuoteProvider {

    private final SymbolRepository symbolRepository;
    private final StockQuoteEventRepository stockQuoteEventRepository;
    private final FetcherClient client;
    @Resource(name = "marketOverviewExecutor")
    private ExecutorService executor;

//    private final List<String> featuredSymbols = List.of(
//            "RELIANCE", "INFY", "TCS", "SBIN", "BHARTIARTL", "HINDUNILVR",
//            "AXISBANK", "KOTAKBANK", "BAJFINANCE", "MARUTI", "LT", "ITC"
//    );

    private final List<String> featuredSymbols = List.of(
            "NESTLEIND","TRENT","ONGC","CIPLA","TATASTEEL","EICHERMOT","M&M","ADANIPORTS","SBILIFE","INDIGO","ADANIENT",
            "JIOFIN",
            "HDFCBANK",
            "JSWSTEEL",
            "TATACONSUM",
            "BEL",
            "SHRIRAMFIN",
            "TECHM",
            "POWERGRID",
            "ETERNAL",
            "MAXHEALTH",
            "COALINDIA",
            "APOLLOHOSP",
            "HINDALCO",
            "NTPC",
            "HCLTECH",
            "ULTRACEMCO",
            "SUNPHARMA",
            "TMPV",
            "HDFCLIFE",
            "BAJAJ-AUTO",
            "ASIANPAINT",
            "BAJAJFINSV",
            "ICICIBANK",
            "TITAN",
            "WIPRO",
            "DRREDDY"
    );

    // ========== Controller Method (Existing) ==========

    @Transactional
    public APIRespDTO<Object> getMarketOverview(Integer stockExchange) {
        // stockExchange -> 1 = NSE, 2 = BSE
        List<String> exchanges = (stockExchange == 1)
                ? List.of("NSE", "NSI")
                : List.of("BSE");

        // Step 1: Get symbol metadata from DB
        List<Map<String, Object>> symbolData = symbolRepository.getSymbol(featuredSymbols, exchanges);
        List<SymbolExchangePair> symbolPairs = flattenSymbols(symbolData);

        if (symbolPairs.isEmpty()) {
            log.warn("No symbols found for exchanges: {}", exchanges);
            return APIRespDTO.builder()
                    .status(0)
                    .message("No symbols found")
                    .data(Collections.emptyList())
                    .build();
        }

        // Step 2: Extract symbol IDs
        List<Integer> symbolIds = symbolPairs.stream()
                .map(SymbolExchangePair::getSymbolId)
                .distinct()
                .toList();

        // Step 3: Check DB for existing quotes
        Map<Integer, Map<String, Object>> cachedQuotes = getCachedQuotes(symbolIds, exchanges.get(0));

        // Step 4: Determine which symbols need fresh data
        List<SymbolExchangePair> symbolsToFetch = symbolPairs.stream()
                .filter(pair -> !cachedQuotes.containsKey(pair.getSymbolId()))
                .toList();

        // Step 5: Fetch missing data from external API
        Map<String, StockQuoteEvent> freshQuotes = new HashMap<>();
        if (!symbolsToFetch.isEmpty()) {
            log.info("Fetching {} symbols from external API", symbolsToFetch.size());
            List<String> symbolsToFetchList = symbolsToFetch.stream()
                    .map(SymbolExchangePair::getSymbol)
                    .toList();
            freshQuotes = fetchQuotesFromApi(symbolsToFetchList);

            // Step 6: Save fresh quotes to DB
            saveFreshQuotesToDb(symbolPairs, freshQuotes);
        } else {
            log.info("All {} symbols found in database cache", symbolIds.size());
        }

        // Step 7: Build response from cached + fresh data
        List<MarketOverviewDTO> results = buildMarketOverview(
                symbolPairs,
                cachedQuotes,
                freshQuotes
        );

        return APIRespDTO.builder()
                .status(1)
                .data(results)
                .build();
    }

    // ========== StockQuoteProvider Interface Implementation ==========

    @Override
    public StockQuoteEvent fetchQuoteFromApi(String symbol) {
        try {
            log.debug("Fetching quote for symbol: {}", symbol);
            SymbolReq req = new SymbolReq(List.of(symbol));
            List<StockQuoteEvent> quotes = client.getQuotes(req);
            return quotes.isEmpty() ? null : quotes.get(0);
        } catch (Exception e) {
            log.error("Failed to fetch quote for symbol: {}", symbol, e);
            return null;
        }
    }

    @Override
    public Map<String, StockQuoteEvent> fetchQuotesFromApi(List<String> symbols) {
        log.info("Fetching {} symbols from external API in parallel", symbols.size());

        List<CompletableFuture<QuoteFetchResult>> futures = symbols.stream()
                .map(symbol -> CompletableFuture
                        .supplyAsync(() -> {
                            try {
                                StockQuoteEvent quote = fetchQuoteFromApi(symbol);
                                return new QuoteFetchResult(symbol, quote, null);
                            } catch (Exception e) {
                                log.error("Failed to fetch quote for {}", symbol, e);
                                return new QuoteFetchResult(symbol, null, e.getMessage());
                            }
                        }, executor)
                        .orTimeout(8, TimeUnit.SECONDS)
                        .exceptionally(ex -> {
                            log.error("Timeout fetching quote for {}", symbol, ex);
                            return new QuoteFetchResult(symbol, null, "Timeout");
                        }))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .filter(result -> result.quote() != null)
                        .collect(Collectors.toMap(
                                QuoteFetchResult::symbol,
                                QuoteFetchResult::quote
                        )))
                .orTimeout(15, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    log.error("Overall timeout fetching quotes", ex);
                    return Collections.emptyMap();
                })
                .join();
    }

    @Override
    @Transactional
    public int fetchAndSaveQuotesBatch(List<SymbolFetchRequest> symbolRequests) {
        if (symbolRequests.isEmpty()) {
            return 0;
        }

        log.info("Batch processing {} symbol quote requests", symbolRequests.size());

        // Extract full symbols for parallel fetch
        List<String> symbols = symbolRequests.stream()
                .map(SymbolFetchRequest::getFullSymbol)
                .distinct()
                .toList();

        // Fetch all quotes in parallel from external API
        Map<String, StockQuoteEvent> quotes = fetchQuotesFromApi(symbols);

        if (quotes.isEmpty()) {
            log.warn("No quotes fetched from API for batch request");
            return 0;
        }

        // 🔥 UPDATE EXISTING ENTITIES USING EVENT ID
        AtomicInteger successCount = new AtomicInteger(0);
        List<StockQuoteEvents> entities = new ArrayList<>();

        for (SymbolFetchRequest request : symbolRequests) {
            StockQuoteEvent quote = quotes.get(request.getFullSymbol());

            if (quote != null) {
                try {
                    // 🔥 FIND EXISTING ENTITY BY EVENT ID
                    StockQuoteEvents entity = stockQuoteEventRepository
                            .findById(request.getEventId())
                            .orElseGet(() -> {
                                log.warn("EventId {} not found, creating new entity for {}",
                                        request.getEventId(), request.getFullSymbol());
                                return new StockQuoteEvents();
                            });

                    // 🔥 UPDATE THE ENTITY (not create new)
                    updateEntityFromQuote(entity, request.getSymbolId(),
                            request.getExchange(), quote);

                    entities.add(entity);
                    successCount.incrementAndGet();

                    log.debug("Updated entity for symbol: {} (eventId: {})",
                            request.getFullSymbol(), request.getEventId());
                } catch (Exception e) {
                    log.error("Failed to update entity for symbol: {} (eventId: {})",
                            request.getFullSymbol(), request.getEventId(), e);
                }
            } else {
                log.warn("No quote data received for symbol: {}", request.getFullSymbol());
            }
        }

        if (!entities.isEmpty()) {
            stockQuoteEventRepository.saveAll(entities);
            log.info("Successfully updated {} quotes in database", entities.size());
        }

        return successCount.get();
    }


    // ========== Private Helper Methods ==========

    private Map<Integer, Map<String, Object>> getCachedQuotes(List<Integer> symbolIds, String exchange) {
        try {
            List<Map<String, Object>> quotes = stockQuoteEventRepository.getStockQuotes(symbolIds, exchange);
            log.info("Found {} cached quotes in database", quotes.size());
            return quotes.stream()
                    .collect(Collectors.toMap(
                            row -> {
                                Object symbolIdObj = row.get("symbolid");
                                if (symbolIdObj instanceof Integer) {
                                    return (Integer) symbolIdObj;
                                } else if (symbolIdObj instanceof Number) {
                                    return ((Number) symbolIdObj).intValue();
                                }
                                throw new IllegalStateException("Unexpected symbolId type: " + symbolIdObj.getClass());
                            },
                            row -> row,
                            (existing, replacement) -> existing
                    ));
        } catch (Exception e) {
            log.error("Error fetching cached quotes", e);
            return Collections.emptyMap();
        }
    }

    @Transactional
    protected void saveFreshQuotesToDb(
            List<SymbolExchangePair> symbolPairs,
            Map<String, StockQuoteEvent> freshQuotes
    ) {
        if (freshQuotes.isEmpty()) {
            return;
        }

        try {
            List<StockQuoteEvents> entities = new ArrayList<>();

            for (SymbolExchangePair pair : symbolPairs) {
                StockQuoteEvent apiQuote = freshQuotes.get(pair.getSymbol());
                if (apiQuote == null) {
                    continue;
                }

                // 🔥 FIND EXISTING ENTITY BY SYMBOL ID AND EXCHANGE
                // Note: You may need to add this method to repository
                StockQuoteEvents entity = stockQuoteEventRepository
                        .findBySymbol_SymbolIdAndExchange(pair.getSymbolId(), pair.getExchange())
                        .orElseGet(() -> {
                            log.info("Creating new quote entry for symbol: {} ({})",
                                    pair.getSymbol(), pair.getExchange());
                            return new StockQuoteEvents();
                        });

                updateEntityFromQuote(entity, pair.getSymbolId(), pair.getExchange(), apiQuote);
                entities.add(entity);
            }

            if (!entities.isEmpty()) {
                stockQuoteEventRepository.saveAll(entities);
                log.info("Saved/Updated {} quotes to database", entities.size());
            }
        } catch (Exception e) {
            log.error("Error saving quotes to database", e);
        }
    }


    private StockQuoteEvents mapToEntity(Integer symbolId, String exchange, StockQuoteEvent apiQuote) {
        StockQuoteEvents entity = new StockQuoteEvents();

        Symbol symbol = new Symbol();
        symbol.setSymbolId(symbolId);
        entity.setSymbol(symbol);

        entity.setRegularMarketPrice(toBigDecimal(apiQuote.getRegularMarketPrice()));
        entity.setRegularMarketDayOpen(toBigDecimal(apiQuote.getRegularMarketDayOpen()));
        entity.setRegularMarketChange(toBigDecimal(apiQuote.getRegularMarketChange()));
        entity.setRegularMarketDayHigh(toBigDecimal(apiQuote.getRegularMarketDayHigh()));
        entity.setRegularMarketDayLow(toBigDecimal(apiQuote.getRegularMarketDayLow()));
        entity.setRegularMarketChangePercent(toBigDecimal(apiQuote.getRegularMarketChangePercent()));
        entity.setRegularMarketTime(apiQuote.getRegularMarketTime());
        entity.setMarketState(apiQuote.getMarketState());
        entity.setFinancialCurrency(apiQuote.getFinancialCurrency());
        entity.setCurrency(apiQuote.getCurrency());
        entity.setTrailingPe(toBigDecimal(apiQuote.getTrailingPE()));
        entity.setMarketCap(apiQuote.getMarketCap());
        entity.setForwardPe(toBigDecimal(apiQuote.getForwardPE()));
        entity.setExchangeTimezoneName(apiQuote.getExchangeTimezoneName());
        entity.setExchangeTimezoneShortName(apiQuote.getExchangeTimezoneShortName());
        entity.setFiftyTwoWeekLow(toBigDecimal(apiQuote.getFiftyTwoWeekLow()));
        entity.setFiftyTwoWeekHigh(toBigDecimal(apiQuote.getFiftyTwoWeekHigh()));
        entity.setExchange(exchange);

        return entity;
    }

    private BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    private List<MarketOverviewDTO> buildMarketOverview(
            List<SymbolExchangePair> symbolPairs,
            Map<Integer, Map<String, Object>> cachedQuotes,
            Map<String, StockQuoteEvent> freshQuotes
    ) {
        return symbolPairs.stream()
                .map(pair -> {
                    StockQuoteEvent quote = freshQuotes.get(pair.getSymbol());

                    if (quote == null) {
                        Map<String, Object> cached = cachedQuotes.get(pair.getSymbolId());
                        if (cached != null) {
                            quote = mapCachedToQuote(cached);
                        }
                    }

                    return MarketOverviewDTO.builder()
                            .symbolId(pair.getSymbolId())
                            .detailId(pair.getDetailId())
                            .symbolName(pair.getSymbolName())
                            .symbol(pair.getSymbol())
                            .exchange(pair.getExchange())
                            .quote(quote)
                            .error(quote == null ? "No data available" : null)
                            .build();
                })
                .filter(dto -> dto.getQuote() != null)
                .toList();
    }

    private StockQuoteEvent mapCachedToQuote(Map<String, Object> cached) {
        StockQuoteEvent quote = new StockQuoteEvent();

        quote.setRegularMarketPrice(getDoubleValue(cached, "ltp"));
        quote.setRegularMarketDayHigh(getDoubleValue(cached, "marketdayhigh"));
        quote.setRegularMarketDayLow(getDoubleValue(cached, "marketdaylow"));
        quote.setRegularMarketDayOpen(getDoubleValue(cached, "marketdayopen"));
        quote.setRegularMarketChange(getDoubleValue(cached, "marketchange"));
        quote.setRegularMarketChangePercent(getDoubleValue(cached, "marketchangepercent"));
        quote.setTrailingPE(getDoubleValue(cached, "trailingpe"));
        quote.setForwardPE(getDoubleValue(cached, "forwardpe"));
        quote.setFiftyTwoWeekLow(getDoubleValue(cached, "fiftytwoweeklow"));
        quote.setFiftyTwoWeekHigh(getDoubleValue(cached, "fiftytwoweekhigh"));

        return quote;
    }

    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key.toLowerCase());
        if (value == null) return null;

        if (value instanceof Double) return (Double) value;
        if (value instanceof BigDecimal) return ((BigDecimal) value).doubleValue();
        if (value instanceof Number) return ((Number) value).doubleValue();

        return null;
    }

    private List<SymbolExchangePair> flattenSymbols(List<Map<String, Object>> symbolData) {
        List<SymbolExchangePair> pairs = new ArrayList<>();

        for (Map<String, Object> row : symbolData) {
            Integer symbolId = (Integer) row.get("symbolid");
            Long detailId = ((Number) row.get("detailid")).longValue();
            String symbolName = (String) row.get("symbolname");
            String nseSymbol = (String) row.get("nse_symbol");
            String bseSymbol = (String) row.get("bse_symbol");

            if (nseSymbol != null && !nseSymbol.isEmpty()) {
                pairs.add(new SymbolExchangePair(symbolId, detailId, symbolName, nseSymbol, "NSE"));
            }

            if (bseSymbol != null && !bseSymbol.isEmpty()) {
                pairs.add(new SymbolExchangePair(symbolId, detailId, symbolName, bseSymbol, "BSE"));
            }
        }

        return pairs;
    }

    private void updateEntityFromQuote(StockQuoteEvents entity, Integer symbolId,
                                       String exchange, StockQuoteEvent apiQuote) {

        // 🔥 ONLY SET SYMBOL IF NEW ENTITY (no event ID yet)
        if (entity.getEventId() == null) {
            Symbol symbol = new Symbol();
            symbol.setSymbolId(symbolId);
            entity.setSymbol(symbol);
            entity.setExchange(exchange);
            log.debug("Creating new entity for symbolId: {}, exchange: {}", symbolId, exchange);
        } else {
            log.debug("Updating existing entity with eventId: {}", entity.getEventId());
        }

        // 🔥 UPDATE ALL QUOTE FIELDS (works for both new and existing entities)
        entity.setRegularMarketPrice(toBigDecimal(apiQuote.getRegularMarketPrice()));
        entity.setRegularMarketDayOpen(toBigDecimal(apiQuote.getRegularMarketDayOpen()));
        entity.setRegularMarketChange(toBigDecimal(apiQuote.getRegularMarketChange()));
        entity.setRegularMarketDayHigh(toBigDecimal(apiQuote.getRegularMarketDayHigh()));
        entity.setRegularMarketDayLow(toBigDecimal(apiQuote.getRegularMarketDayLow()));
        entity.setRegularMarketChangePercent(toBigDecimal(apiQuote.getRegularMarketChangePercent()));
        entity.setRegularMarketTime(apiQuote.getRegularMarketTime());
        entity.setMarketState(apiQuote.getMarketState());
        entity.setFinancialCurrency(apiQuote.getFinancialCurrency());
        entity.setCurrency(apiQuote.getCurrency());
        entity.setTrailingPe(toBigDecimal(apiQuote.getTrailingPE()));
        entity.setMarketCap(apiQuote.getMarketCap());
        entity.setForwardPe(toBigDecimal(apiQuote.getForwardPE()));
        entity.setExchangeTimezoneName(apiQuote.getExchangeTimezoneName());
        entity.setExchangeTimezoneShortName(apiQuote.getExchangeTimezoneShortName());
        entity.setFiftyTwoWeekLow(toBigDecimal(apiQuote.getFiftyTwoWeekLow()));
        entity.setFiftyTwoWeekHigh(toBigDecimal(apiQuote.getFiftyTwoWeekHigh()));
    }

    @Data
    @AllArgsConstructor
    private static class SymbolExchangePair {
        private Integer symbolId;
        private Long detailId;
        private String symbolName;
        private String symbol;
        private String exchange;
    }

    private record QuoteFetchResult(String symbol, StockQuoteEvent quote, String error) {}
}
