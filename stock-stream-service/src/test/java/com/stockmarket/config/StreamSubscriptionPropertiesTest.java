package com.stockmarket.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StreamSubscriptionPropertiesTest {

    @Test
    void maxSymbolsForScreenUsesPerScreenValues() {
        StreamSubscriptionProperties properties = new StreamSubscriptionProperties();
        properties.setMaxSymbolsDefault(25);
        properties.setMaxSymbolsListing(10);
        properties.setMaxSymbolsDetail(1);
        properties.setMaxSymbolsSearchSelected(1);
        properties.setMaxSymbolsWatchlist(40);
        properties.setMaxSymbolsPortfolio(30);

        assertEquals(10, properties.maxSymbolsForScreen(StreamScreenType.LISTING));
        assertEquals(1, properties.maxSymbolsForScreen(StreamScreenType.DETAIL));
        assertEquals(1, properties.maxSymbolsForScreen(StreamScreenType.SEARCH_SELECTED));
        assertEquals(40, properties.maxSymbolsForScreen(StreamScreenType.WATCHLIST));
        assertEquals(30, properties.maxSymbolsForScreen(StreamScreenType.PORTFOLIO));
        assertEquals(25, properties.maxSymbolsForScreen(StreamScreenType.GENERIC));
    }
}

