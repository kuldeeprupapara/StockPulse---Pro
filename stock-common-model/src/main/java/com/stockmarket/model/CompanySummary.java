package com.stockmarket.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CompanySummary {

    // ── Identity ──────────────────────────────────────────────────────────────
    private String symbol;
    private String companyName;
    private String sector;
    private String industry;
    private String exchange;
    private String marketCapCategory;
    private String businessSummary;
    private String address;
    private String website;
    private int    fullTimeEmployees;
    private String earningsDate;

    // ── Live Price ────────────────────────────────────────────────────────────
    private double regularMarketPrice;
    private double regularMarketChange;
    private double regularMarketChangePercent;
    private double regularMarketDayOpen;
    private double regularMarketDayHigh;
    private double regularMarketDayLow;
    private long   regularMarketVolume;
    private double regularMarketPreviousClose;
    private String marketState;

    // ── 52-Week ───────────────────────────────────────────────────────────────
    private double week52High;
    private double week52Low;
    private double week52Change;

    // ── Market Cap ────────────────────────────────────────────────────────────
    private double marketCap;
    private String marketCapFormatted;

    // ── Valuation ─────────────────────────────────────────────────────────────
    private double peRatio;
    private double forwardPE;
    private double priceToBook;
    private double priceToSales;

    // ── Moving Averages ───────────────────────────────────────────────────────
    private double fiftyDayAverage;
    private double twoHundredDayAverage;

    // ── Dividends ─────────────────────────────────────────────────────────────
    private double dividendRate;
    private double dividendYield;       // stored as % (e.g. 0.43, not 0.0043)
    private String exDividendDate;
    private double payoutRatio;

    // ── EPS ───────────────────────────────────────────────────────────────────
    private double epsTrailingTwelveMonths;
    private double epsForward;

    // ── Volume ────────────────────────────────────────────────────────────────
    private long   averageVolume3Month;
    private long   averageVolume10Day;

    // ── Earnings & Margins ────────────────────────────────────────────────────
    private double profitMargin;
    private double earningsGrowthQoQ;
    private double netIncome;

    // ── Enterprise Value ──────────────────────────────────────────────────────
    private double enterpriseValue;
    private double evToRevenue;
    private double evToEbitda;

    // ── Shares & Ownership ────────────────────────────────────────────────────
    private long   sharesOutstanding;
    private double insiderHolding;
    private double institutionalHolding;
    private double beta;
    private double bookValue;

    // ── Circuit Limits ────────────────────────────────────────────────────────
    private double upperCircuit;
    private double lowerCircuit;

    // ── Analyst ───────────────────────────────────────────────────────────────
    private String analystRating;
    private double targetMeanPrice;
    private int    numberOfAnalystOpinions;
}