package com.stockmarket.query.controller;

import com.stockmarket.dto.APIRespDTO;
import com.stockmarket.enums.MarketMoverType;
import com.stockmarket.model.ChartResponse;
import com.stockmarket.model.CompanySummary;
import com.stockmarket.model.StockQuoteEvent;
import com.stockmarket.query.service.StockQueryService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/query")
@RequiredArgsConstructor
public class StockQueryController {

    private final StockQueryService stockQueryService;

    @GetMapping("/{symbol}/summary")
    public ResponseEntity<CompanySummary> getSummary(
            @PathVariable String symbol,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String exchange
    ) {
        return ResponseEntity.ok(stockQueryService.getStockSummary(symbol, countryCode, exchange));
    }

    @GetMapping("/{symbol}/chart")
    public ResponseEntity<ChartResponse> getChart(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1m") String interval,
            @RequestParam(defaultValue = "1d") String range,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String exchange) {
        return ResponseEntity.ok(stockQueryService.getStockChart(symbol, interval, range, countryCode, exchange));
    }

    @GetMapping("/movers")
    public ResponseEntity<List<StockQuoteEvent>> getMovers(
            @RequestParam(defaultValue = "GAINERS") MarketMoverType type,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String exchange
    ) {
        return ResponseEntity.ok(stockQueryService.getMarketMovers(type, countryCode, exchange));
    }

    @GetMapping("/stocks")
    public ResponseEntity<APIRespDTO<Object>> getFilteredStocks(
            @RequestParam(required = false) String exchange,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Double minChangePercent,
            @RequestParam(required = false) Double maxChangePercent,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "25") Integer size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        Map<String, Object> response = stockQueryService.filterStocks(
                exchange,
                symbol,
                minPrice,
                maxPrice,
                minChangePercent,
                maxChangePercent,
                page,
                size,
                sortBy,
                sortDir
        );

        return ResponseEntity.ok(APIRespDTO.builder().status(1).data(response).build());
    }

    @GetMapping("/aggregates/market-breadth")
    public ResponseEntity<APIRespDTO<Object>> getMarketBreadth(@RequestParam(required = false) String exchange) {
        return ResponseEntity.ok(APIRespDTO.builder()
                .status(1)
                .data(stockQueryService.getMarketBreadth(exchange))
                .build());
    }

    @GetMapping("/aggregates/top-movers")
    public ResponseEntity<APIRespDTO<Object>> getTopMovers(
            @RequestParam(required = false) String exchange,
            @RequestParam(defaultValue = "GAINERS") MarketMoverType type,
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        return ResponseEntity.ok(APIRespDTO.builder()
                .status(1)
                .data(stockQueryService.getTopMovers(exchange, type, limit))
                .build());
    }

}
