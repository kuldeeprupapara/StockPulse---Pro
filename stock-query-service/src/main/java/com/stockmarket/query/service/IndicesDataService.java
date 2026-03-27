package com.stockmarket.query.service;

import com.stockmarket.dto.APIRespDTO;
import com.stockmarket.kafka.dto.IndexQuoteKafkaEvent;
import com.stockmarket.model.StockQuoteEvent;
import com.stockmarket.query.config.IndexQuoteProducer;
import com.stockmarket.query.entity.IndexConstituent;
import com.stockmarket.query.entity.IndexQuote;
import com.stockmarket.query.entity.IndexQuoteHistory;
import com.stockmarket.query.entity.StockQuoteEvents;
import com.stockmarket.query.entity.Symbol;
import com.stockmarket.query.entity.SymbolDetail;
import com.stockmarket.query.interfaces.StockQuoteProvider;
import com.stockmarket.query.repository.IndexConstituentRepository;
import com.stockmarket.query.repository.IndexQuoteHistoryRepository;
import com.stockmarket.query.repository.IndexQuoteRepository;
import com.stockmarket.query.repository.MasterIndexRepository;
import com.stockmarket.query.repository.StockQuoteEventRepository;
import com.stockmarket.query.repository.SymbolDetailRepository;
import com.stockmarket.query.repository.SymbolRepository;
import com.stockmarket.util.NumberFormatUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndicesDataService {

    private final StockQuoteProvider stockQuoteProvider;
    private final SymbolRepository symbolRepository;
    private final SymbolDetailRepository symbolDetailRepository;
    private final IndexConstituentRepository indexConstituentRepository;
    private final IndexQuoteRepository indexQuoteRepository;
    private final IndexQuoteHistoryRepository indexQuoteHistoryRepository;
    private final StockQuoteEventRepository stockQuoteEventRepository;

    /*
     * IndexQuoteProducer — our Kafka publisher.
     * Injected here so after every DB save we fire a Kafka event.
     * stock-stream-service will pick it up and push to Angular via SSE.
     */
    private final IndexQuoteProducer indexQuoteProducer;

    private final MasterIndexRepository masterIndexRepository;

    public void preFetchIndexData(){
        List<Map<String, Object>> activeIndexes = masterIndexRepository.getAllActiveIndexes();
        for (Map<String, Object> entry : activeIndexes) {
            String rawSymbol = entry.get("index_symbol") == null ? null : entry.get("index_symbol").toString();
            if (rawSymbol == null || rawSymbol.isBlank()) {
                continue;
            }

            String symbol = rawSymbol.startsWith("^") ? rawSymbol : "^" + rawSymbol;
            StockQuoteEvent event = this.stockQuoteProvider.fetchQuoteFromApi(symbol);
            if (event == null) {
                continue;
            }

            Boolean isExist = symbolRepository.existsBySymbolname(symbol);
            if(Boolean.FALSE.equals(isExist)){
                final Symbol newSymbol = new Symbol();
                newSymbol.setSymbolname(symbol);
                Symbol inserted =  symbolRepository.save(newSymbol);
                log.info("Inserted symbol with ID : {}",inserted.getSymbolId());

                SymbolDetail symbolDetail = new SymbolDetail();
                symbolDetail.setSymbolId(inserted);
                symbolDetail.setExchange(event.getExchangeTimezoneShortName());
                symbolDetail.setType(event.getQuoteType());
                symbolDetail.setName(event.getLongName());
                symbolDetailRepository.save(symbolDetail);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void storeIndexConstituents(Map<String, Integer> indexMap) {
        Integer indexId = indexMap.get("indexId");

        final List<Map<String, Object>> symbolData = symbolRepository.getSymbolListForIndex(indexId);
        final Map<Boolean,List<Map<String,Object>>> filteredResult =  symbolData.stream().collect(Collectors.partitioningBy(row-> {
            Object exist = row.get("symbolid");
            if(exist instanceof Integer value){
                return value != 0;
            }else{
                return false;
            }
        }));

        List<Map<String,Object>> getPresentSymbol = filteredResult.get(Boolean.TRUE);
        List<Map<String,Object>> getAbsentSymbol = filteredResult.get(Boolean.FALSE);

        log.info("Present Index Constituents : {}",getPresentSymbol.size());
        log.info("Absent Index Constituents : {}",getAbsentSymbol.size());

        if(getPresentSymbol.size() != 50){
            throw new RuntimeException("Index Constituents not found");
        }

        for(Map<String,Object> row : getPresentSymbol){
            final IndexConstituent indexConstituent = new IndexConstituent();
            indexConstituent.setSymbolId((Integer)row.get("symbolid"));
            indexConstituent.setIndexId(Long.valueOf(indexId));
            indexConstituent.setCreatedAt(Instant.now());
            indexConstituent.setIsActive(true);

            indexConstituentRepository.save(indexConstituent);
        }
    }

    public APIRespDTO<Object> storeIndexData() {
        return storeIndexData(1);
    }

    public APIRespDTO<Object> storeIndexData(Integer countryId) {

        int safeCountryId = countryId == null ? 1 : countryId;

        // for more information about country id -> please refer to master_country table
        List<Map<String, Object>> symbolData = masterIndexRepository.getIndexDetails(true, safeCountryId);

        if (symbolData.isEmpty()) {
            log.warn("No active index data found for countryId {}, skipping.", safeCountryId);
            return APIRespDTO.builder().status(2).message("No active index data found").build();
        }

        log.info("Index Data Found : {}", symbolData.size());

        final List<String> indexSymbols = symbolData.stream()
                .map(row -> (String) row.get("index_symbol"))
                .toList();

        log.info("Fetching quotes for symbols : {}", indexSymbols);

        // Step 2 — batch fetch all quotes in one call (parallel internally)
        Map<String, StockQuoteEvent> quoteMap = this.stockQuoteProvider.fetchQuotesFromApi(indexSymbols);

        if (quoteMap == null || quoteMap.isEmpty()) {
            log.warn("No quotes returned from API, skipping.");
            return APIRespDTO.builder().status(2).message("No quotes returned from API").build();
        }

        int success = 0;
        int failed  = 0;

        for (Map<String, Object> row : symbolData) {
            try {
                final String  exchange = (String) row.get("exchange");
                final String  symbol   = row.get("index_symbol").toString();
                final String  name     = (String) row.get("indexname");
                final Integer indexId  = (Integer) row.get("indexid");
                final Integer rowCountryId = row.get("countryid") == null
                        ? null
                        : ((Number) row.get("countryid")).intValue();

                StockQuoteEvent event = quoteMap.get(symbol);

                if (event == null) {
                    log.warn("No quote found for symbol: {} exchange: {}", symbol, exchange);
                    failed++;
                    continue;
                }

                OffsetDateTime marketTime = OffsetDateTime.ofInstant(
                        Instant.ofEpochSecond(event.getRegularMarketTime()),
                        ZoneOffset.UTC
                );

                // ── Step 3 : Upsert → index_quote (latest snapshot) ──────────────
                IndexQuote indexQuote = indexQuoteRepository.findById(indexId)
                        .orElse(new IndexQuote());   // update if exists, insert if not

                indexQuote.setIndexId(indexId);
                indexQuote.setOpen(toBD(event.getRegularMarketDayOpen()));
                indexQuote.setHigh(toBD(event.getRegularMarketDayHigh()));
                indexQuote.setLow(toBD(event.getRegularMarketDayLow()));
                indexQuote.setClose(toBD(event.getRegularMarketPrice()));
                indexQuote.setPreviousClose(toBD(event.getRegularMarketPreviousClose()));
                indexQuote.setChange(toBD(event.getRegularMarketChange()));
                indexQuote.setChangePercent(toBD(event.getRegularMarketChangePercent()));
                indexQuote.setAvgVolume3Month(event.getAverageDailyVolume3Month());
                indexQuote.setAvgVolume10Day(event.getAverageDailyVolume10Day());
                indexQuote.setFiftyTwoWeekHigh(toBD(event.getFiftyTwoWeekHigh()));
                indexQuote.setFiftyTwoWeekLow(toBD(event.getFiftyTwoWeekLow()));
                indexQuote.setFiftyTwoWeekChangePct(toBD(event.getFiftyTwoWeekChangePercent()));
                indexQuote.setFiftyTwoWeekHighChange(toBD(event.getFiftyTwoWeekHighChange()));
                indexQuote.setFiftyTwoWeekHighChangePct(toBD(event.getFiftyTwoWeekHighChangePercent()));
                indexQuote.setFiftyTwoWeekLowChange(toBD(event.getFiftyTwoWeekLowChange()));
                indexQuote.setFiftyTwoWeekLowChangePct(toBD(event.getFiftyTwoWeekLowChangePercent()));
                indexQuote.setFiftyTwoWeekRange(event.getFiftyTwoWeekRange());
                indexQuote.setFiftyDayAverage(toBD(event.getFiftyDayAverage()));
                indexQuote.setTwoHundredDayAverage(toBD(event.getTwoHundredDayAverage()));
                indexQuote.setMarketState(convertMarketState(event.getMarketState()));
                indexQuote.setUpdatedAt(marketTime);

                indexQuoteRepository.save(indexQuote);

                // ── Step 4 : Insert → index_quote_history (time-series) ──────────
                // Skip history insert if market is CLOSED — avoids storing stale data repeatedly
                if (!"CLOSED".equalsIgnoreCase(event.getMarketState())) {
                    IndexQuoteHistory history = IndexQuoteHistory.builder()
                            .indexId(indexId)
                            .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                            .price(toBD(event.getRegularMarketPrice()))
                            .change(toBD(event.getRegularMarketChange()))
                            .changePercent(toBD(event.getRegularMarketChangePercent()))
                            .high(toBD(event.getRegularMarketDayHigh()))
                            .low(toBD(event.getRegularMarketDayLow()))
                            .volume(event.getAverageDailyVolume10Day())
                            .marketState(convertMarketState(event.getMarketState()))
                            .build();

                    indexQuoteHistoryRepository.save(history);
                } else {
                    log.debug("Market CLOSED for {} — skipping history insert.", symbol);
                }

                // ── STEP 5: Build and publish Kafka event ─────────────────
                //
                // WHY BUILD A SEPARATE KAFKA DTO?
                // We don't send the JPA entity or API response directly to Kafka.
                // We build a clean DTO with only what the frontend needs.
                // This decouples the DB model from the streaming contract.
                //
                // WHAT HAPPENS AFTER publish()?
                //   1. IndexQuoteProducer serializes event → JSON String
                //   2. KafkaTemplate.send() → Kafka broker stores message
                //   3. stock-stream-service @KafkaListener receives message
                //   4. Deserializes JSON → IndexQuoteEvent object
                //   5. SseEmitterService.broadcast() → all Angular clients
                //   6. Angular EventSource receives "index-quote" event
                //   7. Angular updates price, change, sparkline in-place ✅


                IndexQuoteKafkaEvent kafkaEvent = IndexQuoteKafkaEvent.builder()
                        .indexId(indexId)
                        .indexSymbol(symbol)        // used as Kafka message KEY
                        .indexName(name)
                        .countryId(rowCountryId)
                        .countryCode(resolveCountryCode(exchange))
                        .exchange(exchange)
                        .timezone(event.getExchangeTimezoneName())
                        .currency(event.getCurrency())
                        .price(toBD(event.getRegularMarketPrice()))
                        .change(toBD(event.getRegularMarketChange()))
                        .changePercent(toBD(event.getRegularMarketChangePercent()))
                        .open(toBD(event.getRegularMarketDayOpen()))
                        .high(toBD(event.getRegularMarketDayHigh()))
                        .low(toBD(event.getRegularMarketDayLow()))
                        .previousClose(toBD(event.getRegularMarketPreviousClose()))
                        .marketState(event.getMarketState())
                        .updatedAt(marketTime)
                        .build();

                // Fire and forget — DB save already done above
                // Even if Kafka is down, DB save is safe
                indexQuoteProducer.publish(kafkaEvent);

                log.info("Stored → Symbol: {} | Price: {} | Change: {}% | State: {}",
                        symbol,
                        event.getRegularMarketPrice(),
                        event.getRegularMarketChangePercent(),
                        event.getMarketState());

                success++;

            } catch (Exception e) {
                log.error("Error processing index symbol: {}", row.get("index_symbol"), e);
                failed++;
            }
        }

        log.info("storeIndexData complete — Success: {}, Failed: {}", success, failed);

        return APIRespDTO.builder()
                .status(1)
                .message("Stored " + success + " indices, failed: " + failed)
                .data(quoteMap)
                .build();
    }

    @Transactional
    public APIRespDTO<Object> storeIndexConstituentData(Integer indexId, String exchange) {
        try {
            if (indexId == null) {
                return APIRespDTO.builder().status(2).message("indexId is required").build();
            }
            if (exchange == null || exchange.isBlank()) {
                return APIRespDTO.builder().status(2).message("exchange is required").build();
            }

            List<String> normalizedExchanges = normalizeRequestedExchanges(exchange);
            if (normalizedExchanges.isEmpty()) {
                return APIRespDTO.builder().status(2).message("Valid exchange is required").build();
            }

            List<Map<String, Object>> constituents = indexConstituentRepository
                    .getConstituentSymbolsForIndex(indexId, normalizedExchanges);

            if (constituents.isEmpty()) {
                return APIRespDTO.builder().status(2).message("No constituents found for given index/exchange").build();
            }

            List<String> fullSymbols = constituents.stream()
                    .map(row -> (String) row.get("complete_symbol"))
                    .filter(s -> s != null && !s.isBlank())
                    .distinct()
                    .toList();

            Map<String, StockQuoteEvent> fetchedQuotes = stockQuoteProvider.fetchQuotesFromApi(fullSymbols);
            if (fetchedQuotes.isEmpty()) {
                return APIRespDTO.builder().status(2).message("No quote data received from provider").build();
            }

            List<Integer> symbolIds = constituents.stream()
                    .map(row -> ((Number) row.get("symbolid")).intValue())
                    .distinct()
                    .toList();

            List<StockQuoteEvents> existingRows = stockQuoteEventRepository
                    .findBySymbol_SymbolIdInAndExchangeIn(symbolIds, normalizedExchanges);

            Map<String, StockQuoteEvents> existingBySymbolExchange = existingRows.stream()
                    .filter(e -> e.getSymbol() != null)
                    .collect(Collectors.toMap(
                            e -> e.getSymbol().getSymbolId() + "|" + e.getExchange(),
                            e -> e,
                            (a, b) -> a
                    ));

            List<StockQuoteEvents> toSave = new ArrayList<>();
            int upserted = 0;

            for (Map<String, Object> row : constituents) {
                Integer symbolId = ((Number) row.get("symbolid")).intValue();
                String fullSymbol = (String) row.get("complete_symbol");
                String rowExchange = row.get("exchange") == null
                        ? normalizedExchanges.get(0)
                        : row.get("exchange").toString().trim().toUpperCase(Locale.ROOT);

                StockQuoteEvent quote = fetchedQuotes.get(fullSymbol);
                if (quote == null) {
                    continue;
                }

                String entityKey = symbolId + "|" + rowExchange;
                StockQuoteEvents entity = existingBySymbolExchange.get(entityKey);
                if (entity == null) {
                    entity = new StockQuoteEvents();
                    Symbol symbol = new Symbol();
                    symbol.setSymbolId(symbolId);
                    entity.setSymbol(symbol);
                }

                mapQuoteToStockQuoteEntity(entity, quote, rowExchange);
                toSave.add(entity);
                upserted++;
            }

            if (!toSave.isEmpty()) {
                stockQuoteEventRepository.saveAll(toSave);
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("indexId", indexId);
            payload.put("exchange", normalizedExchanges.size() == 1 ? normalizedExchanges.get(0) : normalizedExchanges);
            payload.put("requested", constituents.size());
            payload.put("receivedQuotes", fetchedQuotes.size());
            payload.put("upserted", upserted);

            return APIRespDTO.builder().status(1).message("Constituent quote upsert completed").data(payload).build();
        } catch (Exception e) {
            log.error("Error storing constituent data for indexId {} and exchange {}", indexId, exchange, e);
            return APIRespDTO.builder().status(2).message("Error storing constituent data").build();
        }
    }

    private List<String> normalizeRequestedExchanges(String exchangeParam) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String raw : exchangeParam.split(",")) {
            String normalized = raw.trim().toUpperCase(Locale.ROOT);
            if (!normalized.isBlank()) {
                tokens.add(normalized);
            }
        }

        boolean hasNseGroup = tokens.remove("NSE") || tokens.remove("NSI");
        if (hasNseGroup) {
            tokens.add("NSE");
            tokens.add("NSI");
        }

        return new ArrayList<>(tokens);
    }


    public APIRespDTO<Object> getIndexData(Integer countryId){
        try{
            List<Map<String,Object>> indexQuote = indexQuoteRepository.getIndexQuoteDetails(countryId);
            List<Map<String, Object>> roundedItems = roundIndexRows(indexQuote);

            Map<String, Object> payload = new HashMap<>();
            payload.put("items", roundedItems);
            payload.put("summary", buildSummary(roundedItems));

            log.info("Index rows found for countryId {}: {}", countryId, roundedItems.size());
            return APIRespDTO.builder().data(payload).status(1).build();
        }
        catch (Exception e){
            log.error("Error fetching index data : ", e);
            return APIRespDTO.builder().status(2).message("Error fetching index data").build();
        }
    }

    public APIRespDTO<Object> getConstituents(Integer indexId, String exchange) {
        try {
            if (indexId == null) {
                return APIRespDTO.builder().status(2).message("indexId is required").build();
            }
            if (exchange == null || exchange.isBlank()) {
                return APIRespDTO.builder().status(2).message("exchange is required").build();
            }

            String normalizedExchange = exchange.trim().toUpperCase();
            List<Map<String, Object>> rows = indexConstituentRepository
                    .getTopConstituentsByMarketCap(indexId, normalizedExchange);

            List<Map<String, Object>> response = rows.stream().map(row -> {
                Map<String, Object> item = new HashMap<>();
                BigDecimal marketCap = toBD(row.get("marketcap"));

                item.put("name", row.get("name"));
                item.put("marketCap", NumberFormatUtil.formatMarketCap(marketCap));
                item.put("marketCapValue", to2(marketCap));
                item.put("fiftyTwoWeek", build52WeekRange(row.get("fiftytwoweeklow"), row.get("fiftytwoweekhigh")));
                item.put("fiftyTwoWeekChangePercent", to2(row.get("fiftytwoweekchangepercent")));
                item.put("ltp", to2(row.get("ltp")));
                item.put("regularMarketChangePercent", to2(row.get("regularmarketchangepercent")));
                return item;
            }).toList();

            return APIRespDTO.builder().status(1).data(response).build();
        } catch (Exception e) {
            log.error("Error fetching constituents for indexId: {}", indexId, e);
            return APIRespDTO.builder().status(2).message("Error fetching constituents").build();
        }
    }

    public APIRespDTO<Object> getIndexOhlc(
            Integer indexId,
            OffsetDateTime fromTs,
            OffsetDateTime toTs,
            Integer bucketMinutes
    ) {
        try {
            if (indexId == null) {
                return APIRespDTO.builder().status(2).message("indexId is required").build();
            }

            OffsetDateTime safeTo = toTs == null ? OffsetDateTime.now(ZoneOffset.UTC) : toTs;
            OffsetDateTime safeFrom = fromTs == null ? safeTo.minusHours(6) : fromTs;
            if (safeFrom.isAfter(safeTo)) {
                return APIRespDTO.builder().status(2).message("fromTs must be before toTs").build();
            }

            int safeBucket = bucketMinutes == null ? 5 : Math.max(1, Math.min(bucketMinutes, 60));

            List<Map<String, Object>> rows = indexQuoteHistoryRepository.getIndexOhlc(
                    indexId,
                    safeFrom,
                    safeTo,
                    safeBucket
            );

            return APIRespDTO.builder().status(1).data(rows).build();
        } catch (Exception e) {
            log.error("Error fetching index OHLC for indexId: {}", indexId, e);
            return APIRespDTO.builder().status(2).message("Error fetching index OHLC").build();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private BigDecimal toBD(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    private short convertMarketState(String state) {
        if (state == null) return -1;
        return switch (state) {
            case "REGULAR"   -> 1;
            case "PRE"       -> 2;
            case "POST"      -> 3;
            case "CLOSED"    -> 0;
            case "PREPRE"    -> 4;
            case "POSTPOST"  -> 5;
            default          -> -1;
        };
    }

    private List<Map<String, Object>> roundIndexRows(List<Map<String, Object>> rows) {
        return rows.stream()
                .map(row -> {
                    Map<String, Object> mapped = new HashMap<>(row);
                    mapped.put("open", to2(row.get("open")));
                    mapped.put("high", to2(row.get("high")));
                    mapped.put("low", to2(row.get("low")));
                    mapped.put("close", to2(row.get("close")));
                    mapped.put("change", to2(row.get("change")));
                    mapped.put("change_percent", to2(row.get("change_percent")));
                    return mapped;
                })
                .toList();
    }

    private Map<String, Object> buildSummary(List<Map<String, Object>> rows) {
        int total = rows.size();
        int featured = 0;
        int gainers = 0;
        int losers = 0;
        int unchanged = 0;

        BigDecimal netChange = BigDecimal.ZERO;
        BigDecimal avgChangePercent = BigDecimal.ZERO;

        for (Map<String, Object> row : rows) {
            Object featuredVal = row.get("isfeatured");
            if (featuredVal instanceof Boolean b && b) {
                featured++;
            }

            BigDecimal change = toBD(row.get("change"));
            BigDecimal changePercent = toBD(row.get("change_percent"));

            if (change != null) {
                netChange = netChange.add(change);
                int cmp = change.compareTo(BigDecimal.ZERO);
                if (cmp > 0) {
                    gainers++;
                } else if (cmp < 0) {
                    losers++;
                } else {
                    unchanged++;
                }
            }

            if (changePercent != null) {
                avgChangePercent = avgChangePercent.add(changePercent);
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalIndexes", total);
        summary.put("featuredIndexes", featured);
        summary.put("gainers", gainers);
        summary.put("losers", losers);
        summary.put("unchanged", unchanged);
        summary.put("netChange", to2(netChange));
        summary.put("averageChangePercent", total == 0 ? BigDecimal.ZERO : to2(avgChangePercent.divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)));
        return summary;
    }

    private BigDecimal to2(Object value) {
        BigDecimal bd = toBD(value);
        return bd == null ? null : bd.setScale(2, RoundingMode.HALF_UP);
    }


    private String build52WeekRange(Object low, Object high) {
        BigDecimal lowBd = to2(low);
        BigDecimal highBd = to2(high);

        if (lowBd == null && highBd == null) {
            return null;
        }
        if (lowBd == null) {
            return highBd.toPlainString();
        }
        if (highBd == null) {
            return lowBd.toPlainString();
        }

        return lowBd.toPlainString() + " - " + highBd.toPlainString();
    }

    private BigDecimal toBD(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    private void mapQuoteToStockQuoteEntity(StockQuoteEvents entity, StockQuoteEvent quote, String exchange) {
        entity.setExchange(exchange);
        entity.setRegularMarketPrice(toBD(quote.getRegularMarketPrice()));
        entity.setRegularMarketDayOpen(toBD(quote.getRegularMarketDayOpen()));
        entity.setRegularMarketChange(toBD(quote.getRegularMarketChange()));
        entity.setRegularMarketDayHigh(toBD(quote.getRegularMarketDayHigh()));
        entity.setRegularMarketDayLow(toBD(quote.getRegularMarketDayLow()));
        entity.setRegularMarketChangePercent(toBD(quote.getRegularMarketChangePercent()));
        entity.setRegularMarketTime(quote.getRegularMarketTime());
        entity.setMarketState(quote.getMarketState());
        entity.setFinancialCurrency(quote.getFinancialCurrency());
        entity.setCurrency(quote.getCurrency());
        entity.setTrailingPe(toBD(quote.getTrailingPE()));
        entity.setMarketCap(quote.getMarketCap());
        entity.setForwardPe(toBD(quote.getForwardPE()));
        entity.setExchangeTimezoneName(quote.getExchangeTimezoneName());
        entity.setExchangeTimezoneShortName(quote.getExchangeTimezoneShortName());
        entity.setFiftyTwoWeekLow(toBD(quote.getFiftyTwoWeekLow()));
        entity.setFiftyTwoWeekHigh(toBD(quote.getFiftyTwoWeekHigh()));
    }

    private String resolveCountryCode(String exchange) {
        if (exchange == null || exchange.isBlank()) {
            return null;
        }
        String normalized = exchange.trim().toUpperCase(Locale.ROOT);
        if ("NSI".equals(normalized)) {
            normalized = "NSE";
        }
        return switch (normalized) {
            case "NSE", "BSE" -> "IN";
            case "NYQ", "NMS" -> "US";
            case "SHH", "SHZ" -> "CN";
            default -> null;
        };
    }
}
