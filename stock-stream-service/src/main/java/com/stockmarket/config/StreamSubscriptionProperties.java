package com.stockmarket.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "stream.subscription")
@Getter
@Setter
public class StreamSubscriptionProperties {

    private long emitterTimeoutMs = Long.MAX_VALUE;

    private int maxSymbolsDefault = 25;
    private int maxSymbolsListing = 10;
    private int maxSymbolsDetail = 1;
    private int maxSymbolsSearchSelected = 1;
    private int maxSymbolsWatchlist = 50;
    private int maxSymbolsPortfolio = 50;

    public int maxSymbolsForScreen(StreamScreenType screenType) {
        if (screenType == null) {
            return maxSymbolsDefault;
        }

        return switch (screenType) {
            case LISTING -> maxSymbolsListing;
            case DETAIL -> maxSymbolsDetail;
            case SEARCH_SELECTED -> maxSymbolsSearchSelected;
            case WATCHLIST -> maxSymbolsWatchlist;
            case PORTFOLIO -> maxSymbolsPortfolio;
            case GENERIC -> maxSymbolsDefault;
        };
    }
}

