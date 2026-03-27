package com.stockmarket.query.entity;

import com.stockmarket.query.entity.composite.IndexQuoteHistoryId;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "index_quote_history",
        schema = "public",
        indexes = {
                @Index(name = "idx_iqh_indexid_recorded",
                        columnList = "indexid, recorded_at DESC")
        }
)
@IdClass(IndexQuoteHistoryId.class)  // composite PK
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class IndexQuoteHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "index_quote_history_seq")
    @SequenceGenerator(
            name       = "index_quote_history_seq",
            sequenceName = "index_quote_history_historyid_seq",
            allocationSize = 1
    )
    @Column(name = "historyid", nullable = false, updatable = false)
    private Long historyId;

    @Id
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "indexid", nullable = false)
    private Integer indexId;

    @Column(name = "price", nullable = false, precision = 20, scale = 8)
    private BigDecimal price;

    @Column(name = "change", precision = 20, scale = 8)
    private BigDecimal change;

    @Column(name = "change_percent", precision = 10, scale = 6)
    private BigDecimal changePercent;

    @Column(name = "high", precision = 20, scale = 8)
    private BigDecimal high;

    @Column(name = "low", precision = 20, scale = 8)
    private BigDecimal low;

    @Column(name = "volume")
    private Long volume;

    @Column(name = "market_state")
    private Short marketState;
}
