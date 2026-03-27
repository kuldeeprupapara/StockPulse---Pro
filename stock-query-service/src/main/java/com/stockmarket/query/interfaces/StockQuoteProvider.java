package com.stockmarket.query.interfaces;

import com.stockmarket.model.StockQuoteEvent;
import com.stockmarket.query.dto.SymbolFetchRequest;
import java.util.List;
import java.util.Map;

/**
 * Interface for fetching and managing stock quotes.
 *
 * @author kuldeep rupapara
 * @version 1.0
 * @since 2026-01-29
 */
public interface StockQuoteProvider {

    /**
     * Fetch quote for a single symbol from external API.
     *
     * @param symbol the stock symbol (e.g., "RELIANCE.NS")
     * @return StockQuoteEvent or null if not found
     */
    StockQuoteEvent fetchQuoteFromApi(String symbol);

    /**
     * Fetch quotes for multiple symbols in parallel.
     *
     * @param symbols list of stock symbols
     * @return map of symbol to StockQuoteEvent
     */
    Map<String, StockQuoteEvent> fetchQuotesFromApi(List<String> symbols);

    /**
     * Batch fetch and save quotes for multiple symbols.
     *
     * @param symbolRequests list of symbol fetch requests
     * @return count of successfully saved quotes
     */
    int fetchAndSaveQuotesBatch(List<SymbolFetchRequest> symbolRequests);
}