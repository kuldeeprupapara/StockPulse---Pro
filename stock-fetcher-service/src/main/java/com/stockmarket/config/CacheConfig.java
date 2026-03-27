package com.stockmarket.config;

import com.stockmarket.model.StockQuoteEvent;
import java.util.List;

public interface CacheConfig {

    List<StockQuoteEvent> loadSymbolData(List<String> symbols);
}
