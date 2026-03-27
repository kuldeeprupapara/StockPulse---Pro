package com.stockmarket.controller;

import com.stockmarket.dto.APIRespDTO;
import com.stockmarket.model.SearchResult;
import com.stockmarket.service.StockSearchService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/search")
public class SearchController {

    private final StockSearchService stockSearchService;

    public SearchController(StockSearchService stockSearchService) {
        this.stockSearchService = stockSearchService;
    }

    @GetMapping("/symbols")
    public ResponseEntity<APIRespDTO<Object>> searchSymbols(
            @RequestParam("q") String query,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String exchange
    ) {
        List<SearchResult> results = stockSearchService.search(query, countryCode, exchange);
        return ResponseEntity.ok(APIRespDTO.success(results));
    }
}

