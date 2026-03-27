package com.stockmarket.query.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@Entity
@Table(name = "stock_quote_events")
@NoArgsConstructor
public class StockQuoteEvents {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "eventid")
    private Long eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "symbol_id", referencedColumnName = "symbolid")
    private Symbol symbol;

    @Column(name = "regular_market_price")
    private BigDecimal regularMarketPrice;

    @Column(name = "regular_market_day_open")
    private BigDecimal regularMarketDayOpen;

    @Column(name = "regular_market_change")
    private BigDecimal regularMarketChange;

    @Column(name = "regular_market_day_high")
    private BigDecimal regularMarketDayHigh;

    @Column(name = "regular_market_day_low")
    private BigDecimal regularMarketDayLow;

    @Column(name = "regular_market_change_percent")
    private BigDecimal regularMarketChangePercent;

    @Column(name = "regular_market_time")
    private Long regularMarketTime;

    @Column(name = "market_state")
    private String marketState;

    @Column(name = "financial_currency")
    private String financialCurrency;

    @Column(name = "currency")
    private String currency;

    @Column(name = "trailing_pe")
    private BigDecimal trailingPe;

    @Column(name = "market_cap")
    private Long marketCap;

    @Column(name = "forward_pe")
    private BigDecimal forwardPe;

    @Column(name = "exchange_timezone_name")
    private String exchangeTimezoneName;

    @Column(name = "exchange_timezone_short_name")
    private String exchangeTimezoneShortName;

    @Column(name = "fifty_two_week_low")
    private BigDecimal fiftyTwoWeekLow;

    @Column(name = "fifty_two_week_high")
    private BigDecimal fiftyTwoWeekHigh;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "exchange")
    private String exchange;
}