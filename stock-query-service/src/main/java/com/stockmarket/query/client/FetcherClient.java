package com.stockmarket.query.client;

import com.stockmarket.dto.SymbolReq;
import com.stockmarket.enums.MarketMoverType;
import com.stockmarket.model.ChartResponse;
import com.stockmarket.model.CompanySummary;
import com.stockmarket.model.StockQuoteEvent;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "stock-fetcher-service", url = "${stock.fetcher.url:http://localhost:8081}")
public interface FetcherClient {

    @PostMapping("/internal/market/quotes")
    List<StockQuoteEvent> getQuotes(@RequestBody SymbolReq symbolReq);

    @GetMapping("/internal/market/chart")
    ChartResponse getChart(
            @RequestParam("symbol") String symbol,
            @RequestParam("interval") String interval,
            @RequestParam("range") String range,
            @RequestParam(value = "countryCode", required = false) String countryCode,
            @RequestParam(value = "exchange", required = false) String exchange);

    @GetMapping("/internal/market/summary/{symbol}")
    CompanySummary getSummary(
            @PathVariable("symbol") String symbol,
            @RequestParam(value = "countryCode", required = false) String countryCode,
            @RequestParam(value = "exchange", required = false) String exchange
    );

    @GetMapping("/internal/market/movers")
    List<StockQuoteEvent> getMovers(
            @RequestParam("type") MarketMoverType type,
            @RequestParam(value = "countryCode", required = false) String countryCode,
            @RequestParam(value = "exchange", required = false) String exchange
    );
}
