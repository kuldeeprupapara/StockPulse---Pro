package com.stockmarket.query.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request object for fetching symbol quotes.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SymbolFetchRequest {
    private Long eventId;
    private Integer symbolId;
    private String symbolName;       // e.g., "GOENKA"
    private String fullSymbol;       // e.g., "GOENKA.NS"
    private String exchange;         // "NSE", "NSI", or "BSE"
}
