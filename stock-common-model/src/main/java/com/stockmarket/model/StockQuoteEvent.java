package com.stockmarket.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class StockQuoteEvent{
    // ── Core identity ──────────────────────────────────────────

    private String symbol;
    private String shortName;
    private String longName;
    private String quoteType;
    private String currency;
    private String exchange;
    private String fullExchangeName;
    private String exchangeTimezoneName;
    private String exchangeTimezoneShortName;
    private String market;
    private String marketState;
    private String quoteSourceName;
    private String financialCurrency;
    private String regularMarketDayRange;
    private String fiftyTwoWeekRange;       // → fifty_two_week_range

    // ── Regular market prices ──────────────────────────────────
    private double regularMarketPrice;      // → close
    private Double regularMarketDayOpen;    // → open
    private Double regularMarketDayHigh;    // → high
    private Double regularMarketDayLow;     // → low
    private double regularMarketChange;     // → change
    private double regularMarketChangePercent; // → change_percent
    private Double regularMarketPreviousClose; // → previous_close   ADDED
    private long regularMarketTime;         // → updated_at (epoch seconds)



    // ── Volume ─────────────────────────────────────────────────
    private Long averageDailyVolume3Month;  // → avg_volume_3month   ADDED
    private Long averageDailyVolume10Day;   // → avg_volume_10day    ADDED

    // ── 52 Week ────────────────────────────────────────────────
    private Double fiftyTwoWeekHigh;                // → fifty_two_week_high
    private Double fiftyTwoWeekLow;                 // → fifty_two_week_low
    private Double fiftyTwoWeekChangePercent;       // → fifty_two_week_change_pct  ADDED
    private Double fiftyTwoWeekHighChange;          // → fifty_two_week_high_change   ADDED
    private Double fiftyTwoWeekHighChangePercent;   // → fifty_two_week_high_change_pct   ADDED
    private Double fiftyTwoWeekLowChange;           // → fifty_two_week_low_change   ADDED
    private Double fiftyTwoWeekLowChangePercent;    // → fifty_two_week_low_change_pct   ADDED

    // ── Moving averages ────────────────────────────────────────
    private Double fiftyDayAverage;                 // → fifty_day_average   ADDED
    private Double fiftyDayAverageChange;
    private Double fiftyDayAverageChangePercent;
    private Double twoHundredDayAverage;            // → two_hundred_day_average   ADDED
    private Double twoHundredDayAverageChange;
    private Double twoHundredDayAverageChangePercent;

    // ── Fundamentals (for stocks, null for INDEX) ──────────────
    private Double trailingPE;
    private Double forwardPE;
    private Long marketCap;

    // ── Misc ───────────────────────────────────────────────────
    private Long firstTradeDateMilliseconds;
    private Long gmtOffSetMilliseconds;
    private Integer priceHint;
    private Integer sourceInterval;
    private Integer exchangeDataDelayedBy;
    private Boolean triggerable;
    private Boolean tradeable;
    private Boolean cryptoTradeable;
    private Boolean hasPrePostMarketData;
    private Boolean esgPopulated;
    private String messageBoardId;
    private String customPriceAlertConfidence;
    private String region;
    private String language;
    private String typeDisp;

}
