package com.stockmarket.response;

import com.stockmarket.model.StockQuoteEvent;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MarketOverviewDTO {
    private Integer symbolId;
    private Long detailId;
    private String symbolName;      // e.g., "RELIANCE"
    private String symbol;          // e.g., "RELIANCE.NS"
    private String exchange;        // "NSE" or "BSE"
    private StockQuoteEvent quote;  // Quote data from API
    private String error;           // Error message if failed
}
