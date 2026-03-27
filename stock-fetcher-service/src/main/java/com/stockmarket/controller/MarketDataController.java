package com.stockmarket.controller;

import com.stockmarket.dto.APIRespDTO;
import com.stockmarket.dto.SymbolReq;
import com.stockmarket.enums.MarketMoverType;
import com.stockmarket.model.ChartResponse;
import com.stockmarket.model.CompanySummary;
import com.stockmarket.model.SearchResult;
import com.stockmarket.model.StockQuoteEvent;
import com.stockmarket.service.MarketDataService;
import com.stockmarket.service.SearchAndFilterSymbol;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/internal/market")
@RestController
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataService marketDataService;
    private final SearchAndFilterSymbol searchAndFilterSymbol;


    @PostMapping("/quotes")
    public ResponseEntity<List<StockQuoteEvent>> quotes(@RequestBody SymbolReq symbolReq) {
        return ResponseEntity.ok(marketDataService.getQuotes(symbolReq));
    }

    @GetMapping("/chart")
    public ResponseEntity<ChartResponse> chart(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "1m") String interval,
            @RequestParam(defaultValue = "1d") String range,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String exchange) {
        return ResponseEntity.ok(marketDataService.getChart(symbol, interval, range, countryCode, exchange));
    }

    @GetMapping("/search")
    public ResponseEntity<List<SearchResult>> search(
            @RequestParam String q,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String exchange
    ) {
        return ResponseEntity.ok(marketDataService.search(q, countryCode, exchange));
    }

    @GetMapping("/summary/{symbol}")
    public ResponseEntity<CompanySummary> summary(
            @PathVariable String symbol,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String exchange
    ) {
        return ResponseEntity.ok(marketDataService.getCompanySummary(symbol, countryCode, exchange));
    }

    @GetMapping("/movers")
    public ResponseEntity<List<StockQuoteEvent>> movers(
            @RequestParam(defaultValue = "GAINERS") MarketMoverType type,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String exchange
    ) {
        return ResponseEntity.ok(marketDataService.getMarketMovers(type, countryCode, exchange));
    }

    @GetMapping("/index/{symbol}")
    public ResponseEntity<ChartResponse> index(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1d") String interval,
            @RequestParam(defaultValue = "1mo") String range) {
        return ResponseEntity.ok(marketDataService.getIndexChart(symbol, interval, range));
    }

    @GetMapping("/get-symbol-details-from-file")
    public ResponseEntity<APIRespDTO<Object>> getDetails(){
        return ResponseEntity.ok(searchAndFilterSymbol.search());
    }
}
