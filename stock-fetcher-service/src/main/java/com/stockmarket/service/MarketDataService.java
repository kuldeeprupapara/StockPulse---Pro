package com.stockmarket.service;

import com.stockmarket.config.CacheConfig;
import com.stockmarket.dto.SymbolReq;
import com.stockmarket.client.ExternalMarketClient;
import com.stockmarket.enums.MarketMoverType;
import com.stockmarket.model.ChartResponse;
import com.stockmarket.model.CompanySummary;
import com.stockmarket.model.SearchResult;
import com.stockmarket.model.StockQuoteEvent;
import com.stockmarket.util.CommonMarketUtils;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MarketDataService {

    private static final Map<String, String> SUFFIX_BY_EXCHANGE = Map.of(
            "NSE", ".NS",
            "BSE", ".BO"
    );

    private static final Map<String, String> COUNTRY_BY_EXCHANGE = Map.of(
            "NSE", "IN",
            "BSE", "IN",
            "NYQ", "US",
            "NMS", "US",
            "SHH", "CN",
            "SHZ", "CN"
    );

    private static final Map<String, String> DEFAULT_EXCHANGE_BY_COUNTRY = Map.of(
            "IN", "NSE",
            "US", "NYQ",
            "CN", "SHH"
    );

    private final ExternalMarketClient marketClient;
    private final CacheConfig config;

    public List<StockQuoteEvent> getQuotes(final SymbolReq symbolReq) {
        // load from cache first
        final List<String> symbols = symbolReq.quotes();
       return config.loadSymbolData(symbols);
    }

    public ChartResponse getChart(String symbol, String interval, String range, String countryCode, String exchange) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        String resolvedSymbol = symbol.startsWith("^") ? symbol : resolveEquitySymbol(symbol, exchange, countryCode);
        return marketClient.fetchChart(resolvedSymbol, interval, range);
    }

    public List<SearchResult> search(String query, String countryCode, String exchange) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query parameter is required");
        }
        List<SearchResult> results = marketClient.search(query);
        String normalizedExchange = CommonMarketUtils.normalizeExchange(exchange);
        String normalizedCountry = CommonMarketUtils.normalizeCountryCode(countryCode);

        return results.stream()
                .filter(item -> matchesExchange(item.getExchange(), normalizedExchange))
                .filter(item -> matchesCountry(item.getExchange(), normalizedCountry))
                .toList();
    }

    public CompanySummary getCompanySummary(String symbol, String countryCode, String exchange) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        String resolvedSymbol = resolveEquitySymbol(symbol, exchange, countryCode);
        return marketClient.fetchCompanySummary(resolvedSymbol);
    }

    public List<StockQuoteEvent> getMarketMovers(MarketMoverType type, String countryCode, String exchange) {
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        List<StockQuoteEvent> movers = marketClient.fetchMarketMovers(type);
        String normalizedExchange = CommonMarketUtils.normalizeExchange(exchange);
        String normalizedCountry = CommonMarketUtils.normalizeCountryCode(countryCode);

        return movers.stream()
                .filter(item -> matchesExchange(item.getExchange(), normalizedExchange))
                .filter(item -> matchesCountry(item.getExchange(), normalizedCountry))
                .toList();
    }

    public ChartResponse getIndexChart(String symbol, String interval, String range) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("index symbol is required");
        }
        String normalizedSymbol = normalizeSymbol(symbol);
        return marketClient.fetchChart(normalizedSymbol, interval, range);
    }

    private String normalizeSymbol(String symbol) {
        return symbol.startsWith("^") ? symbol : "^" + symbol;
    }

    private String resolveEquitySymbol(String symbol, String exchange, String countryCode) {
        if (symbol.contains(".")) {
            return symbol;
        }

        String normalizedExchange = CommonMarketUtils.normalizeExchange(exchange);
        if (normalizedExchange == null) {
            normalizedExchange = CommonMarketUtils.defaultExchangeByCountry(countryCode, DEFAULT_EXCHANGE_BY_COUNTRY);
        }
        return CommonMarketUtils.buildDisplaySymbol(symbol, normalizedExchange, SUFFIX_BY_EXCHANGE);
    }

    private boolean matchesExchange(String actualExchange, String requestedExchange) {
        return CommonMarketUtils.matchesExchange(actualExchange, requestedExchange);
    }

    private boolean matchesCountry(String actualExchange, String requestedCountryCode) {
        if (requestedCountryCode == null) {
            return true;
        }
        return requestedCountryCode.equals(resolveCountryCodeByExchange(actualExchange));
    }

    private String resolveCountryCodeByExchange(String exchange) {
        return CommonMarketUtils.resolveCountryCodeByExchange(exchange, COUNTRY_BY_EXCHANGE);
    }



}
