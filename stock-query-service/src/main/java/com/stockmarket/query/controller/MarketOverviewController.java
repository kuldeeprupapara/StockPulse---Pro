package com.stockmarket.query.controller;

import com.stockmarket.dto.APIRespDTO;
import com.stockmarket.query.service.MarketOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/query")
public class MarketOverviewController {

private final MarketOverviewService marketOverviewService;

    @GetMapping("/overview/{stockExchange}")
    public APIRespDTO<Object> getMarketOverview(@PathVariable Integer stockExchange) {
        return marketOverviewService.getMarketOverview(stockExchange);
    }
}
