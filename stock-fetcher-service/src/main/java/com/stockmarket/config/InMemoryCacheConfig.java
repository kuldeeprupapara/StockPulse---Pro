package com.stockmarket.config;

import com.stockmarket.client.ExternalMarketClient;
import com.stockmarket.model.StockQuoteEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InMemoryCacheConfig implements CacheConfig {

    private final ExternalMarketClient marketClient;

    private final Map<String,StockQuoteEvent> eventMap = new ConcurrentHashMap<>(15);

    public List<StockQuoteEvent> loadSymbolData(List<String> symbol){
        final List<StockQuoteEvent> stockQuoteEvents = new ArrayList<>();
        for(String name : symbol){
            StockQuoteEvent event = eventMap.computeIfAbsent(name, marketClient::fetchQuote);
            stockQuoteEvents.add(event);
        }
        return stockQuoteEvents;
    }
}
