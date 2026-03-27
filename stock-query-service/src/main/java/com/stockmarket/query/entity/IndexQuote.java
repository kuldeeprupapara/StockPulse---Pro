package com.stockmarket.query.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "index_quote")
@Getter
@Setter
@NoArgsConstructor
public class IndexQuote {

    @Id
    @Column(name = "indexid")
    private Integer indexId;

    @Column(name = "open", precision = 20, scale = 8)
    private BigDecimal open;

    @Column(name = "high", precision = 20, scale = 8)
    private BigDecimal high;

    @Column(name = "low", precision = 20, scale = 8)
    private BigDecimal low;

    @Column(name = "close", precision = 20, scale = 8)
    private BigDecimal close;

    @Column(name = "previous_close", precision = 20, scale = 8)
    private BigDecimal previousClose;

    @Column(name = "change", precision = 20, scale = 8)
    private BigDecimal change;

    @Column(name = "change_percent", precision = 10, scale = 6)
    private BigDecimal changePercent;

    @Column(name = "volume")
    private Long volume;

    @Column(name = "avg_volume_3month")
    private Long avgVolume3Month;

    @Column(name = "avg_volume_10day")
    private Long avgVolume10Day;

    @Column(name = "fifty_two_week_high", precision = 20, scale = 8)
    private BigDecimal fiftyTwoWeekHigh;

    @Column(name = "fifty_two_week_low", precision = 20, scale = 8)
    private BigDecimal fiftyTwoWeekLow;

    @Column(name = "fifty_two_week_change_pct", precision = 10, scale = 6)
    private BigDecimal fiftyTwoWeekChangePct;

    @Column(name = "fifty_day_average", precision = 20, scale = 8)
    private BigDecimal fiftyDayAverage;

    @Column(name = "two_hundred_day_average", precision = 20, scale = 8)
    private BigDecimal twoHundredDayAverage;

    @Column(name = "market_state")
    private Short marketState;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "fifty_two_week_low_change_pct", precision = 20, scale = 8)
    private BigDecimal fiftyTwoWeekLowChangePct;

    @Column(name = "fifty_two_week_high_change_pct", precision = 20, scale = 8)
    private BigDecimal fiftyTwoWeekHighChangePct;

    @Column(name = "fifty_two_week_low_change", precision = 20, scale = 8)
    private BigDecimal fiftyTwoWeekLowChange;

    @Column(name = "fifty_two_week_high_change", precision = 20, scale = 8)
    private BigDecimal fiftyTwoWeekHighChange;

    @Column(name = "fifty_two_week_range")
    private String fiftyTwoWeekRange;

    // FK relation — read only, use indexId for inserts
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "indexid", referencedColumnName = "indexid",insertable = false, updatable = false)
    private MasterIndex masterIndex;
}
