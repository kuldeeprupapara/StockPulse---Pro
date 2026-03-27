package com.stockmarket.service;

import com.stockmarket.client.FetcherSearchClient;
import com.stockmarket.model.SearchResult;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StockSearchService {

    private final FetcherSearchClient fetcherSearchClient;

    public StockSearchService(FetcherSearchClient fetcherSearchClient) {
        this.fetcherSearchClient = fetcherSearchClient;
    }

    public List<SearchResult> search(String query, String countryCode, String exchange) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query parameter is required");
        }
        return fetcherSearchClient.search(query.trim(), countryCode, exchange);
    }
}

