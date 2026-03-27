package com.stockmarket.query.service;

import com.stockmarket.enums.MarketMoverType;
import com.stockmarket.model.ChartResponse;
import com.stockmarket.model.CompanySummary;
import com.stockmarket.model.StockQuoteEvent;
import com.stockmarket.query.client.FetcherClient;
import com.stockmarket.query.entity.StockQuoteEvents;
import com.stockmarket.query.repository.StockQuoteEventRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockQueryService {

    private final FetcherClient fetcherClient;
    private final StockQuoteEventRepository stockQuoteEventRepository;

    public CompanySummary getStockSummary(String symbol, String countryCode, String exchange) {
        return fetcherClient.getSummary(symbol, countryCode, exchange);
    }

    public ChartResponse getStockChart(String symbol, String interval, String range, String countryCode, String exchange) {
        return fetcherClient.getChart(symbol, interval, range, countryCode, exchange);
    }

    public List<StockQuoteEvent> getMarketMovers(MarketMoverType type, String countryCode, String exchange) {
        return fetcherClient.getMovers(type, countryCode, exchange);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterStocks(
            String exchange,
            String symbol,
            Double minPrice,
            Double maxPrice,
            Double minChangePercent,
            Double maxChangePercent,
            Integer page,
            Integer size,
            String sortBy,
            String sortDir
    ) {
        int safePage = page == null ? 0 : Math.max(page, 0);
        int safeSize = size == null ? 25 : Math.max(size, 1);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(parseDirection(sortDir), mapSortField(sortBy))
        );

        Specification<StockQuoteEvents> spec = (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();

            if (exchange != null && !exchange.isBlank()) {
                predicates.add(cb.equal(root.get("exchange"), exchange));
            }

            if (symbol != null && !symbol.isBlank()) {
                Join<Object, Object> symbolJoin = root.join("symbol");
                predicates.add(cb.like(
                        cb.upper(symbolJoin.get("symbolname")),
                        "%" + symbol.trim().toUpperCase() + "%"
                ));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("regularMarketPrice"), BigDecimal.valueOf(minPrice)));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("regularMarketPrice"), BigDecimal.valueOf(maxPrice)));
            }

            if (minChangePercent != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("regularMarketChangePercent"), BigDecimal.valueOf(minChangePercent)));
            }

            if (maxChangePercent != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("regularMarketChangePercent"), BigDecimal.valueOf(maxChangePercent)));
            }

            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<StockQuoteEvents> result = stockQuoteEventRepository.findAll(spec, pageable);

        List<Map<String, Object>> rows = result.getContent().stream()
                .map(this::mapQuoteRow)
                .toList();

        Map<String, Object> payload = new HashMap<>();
        payload.put("content", rows);
        payload.put("page", result.getNumber());
        payload.put("size", result.getSize());
        payload.put("totalElements", result.getTotalElements());
        payload.put("totalPages", result.getTotalPages());
        payload.put("hasNext", result.hasNext());
        return payload;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMarketBreadth(String exchange) {
        return stockQuoteEventRepository.getMarketBreadth(exchange);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopMovers(String exchange, MarketMoverType type, Integer limit) {
        int safeLimit = limit == null ? 20 : Math.max(1, Math.min(limit, 100));
        if (type == MarketMoverType.LOSERS) {
            return stockQuoteEventRepository.getTopLosers(exchange, safeLimit);
        }
        return stockQuoteEventRepository.getTopGainers(exchange, safeLimit);
    }

    private Sort.Direction parseDirection(String sortDir) {
        return "ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    private String mapSortField(String sortBy) {
        if ("price".equalsIgnoreCase(sortBy)) {
            return "regularMarketPrice";
        }
        if ("changePercent".equalsIgnoreCase(sortBy)) {
            return "regularMarketChangePercent";
        }
        if ("updatedAt".equalsIgnoreCase(sortBy)) {
            return "updatedAt";
        }
        return "updatedAt";
    }

    private Map<String, Object> mapQuoteRow(StockQuoteEvents entity) {
        Map<String, Object> row = new HashMap<>();
        row.put("symbolId", entity.getSymbol() != null ? entity.getSymbol().getSymbolId() : null);
        row.put("symbol", entity.getSymbol() != null ? entity.getSymbol().getSymbolname() : null);
        row.put("exchange", entity.getExchange());
        row.put("price", entity.getRegularMarketPrice());
        row.put("change", entity.getRegularMarketChange());
        row.put("changePercent", entity.getRegularMarketChangePercent());
        row.put("marketState", entity.getMarketState());
        row.put("updatedAt", entity.getUpdatedAt());
        return row;
    }
}
