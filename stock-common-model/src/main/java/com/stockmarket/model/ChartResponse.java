package com.stockmarket.model;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChartResponse{
    private String symbol;
    private String interval;   // 1m, 5m, 15m, 1d, etc.
    private String range;      // 1d, 5d, 1mo, etc.
    private List<ChartPoint> data;
}