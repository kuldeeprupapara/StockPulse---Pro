package com.stockmarket.kafka.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
//@JsonIgnoreProperties(ignoreUnknown = true)
public class IndexQuoteKafkaEvent {
    private Integer        indexId;        // DB primary key e.g. 26
    private String         indexSymbol;    // Kafka message KEY e.g. "^NSEI"
    private String         indexName;      // Display name e.g. "NIFTY 50"
    private Integer        countryId;
    private String         countryCode;
    private String         exchange;       // e.g. "NSE", "BSE"
    private String         timezone;
    private String         currency;
    private BigDecimal     price;          // Current market price
    private BigDecimal change;         // Price change from previous close
    private BigDecimal     changePercent;  // % change from previous close
    private BigDecimal     open;           // Opening price
    private BigDecimal     high;           // Day high
    private BigDecimal     low;            // Day low
    private BigDecimal     previousClose;  // Previous day closing price
    private String         marketState;    // REGULAR, PRE, POST, CLOSED
    private OffsetDateTime updatedAt;      // Market data timestamp (UTC)
}
