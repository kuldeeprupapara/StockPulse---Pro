package com.stockmarket.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChartPoint {
    private long timestamp;   // epoch millis
    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;
}
