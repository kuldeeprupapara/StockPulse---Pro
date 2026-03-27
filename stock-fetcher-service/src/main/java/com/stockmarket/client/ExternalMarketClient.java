package com.stockmarket.client;

import com.stockmarket.enums.MarketMoverType;
import com.stockmarket.model.ChartResponse;
import com.stockmarket.model.CompanySummary;
import com.stockmarket.model.SearchResult;
import com.stockmarket.model.StockQuoteEvent;
import java.util.List;

public interface ExternalMarketClient {

    StockQuoteEvent fetchQuote(String symbol);
    List<StockQuoteEvent> fetchQuotes(List<String> symbols);

    ChartResponse fetchChart(String symbol, String interval, String range);

    List<SearchResult> search(String query);

    CompanySummary fetchCompanySummary(String symbol);

    List<StockQuoteEvent> fetchMarketMovers(MarketMoverType type);
}
