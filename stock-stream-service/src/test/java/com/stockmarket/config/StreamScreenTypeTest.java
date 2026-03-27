package com.stockmarket.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StreamScreenTypeTest {

    @Test
    void fromParsesCommonValues() {
        assertEquals(StreamScreenType.LISTING, StreamScreenType.from("listing"));
        assertEquals(StreamScreenType.SEARCH_SELECTED, StreamScreenType.from("search-selected"));
        assertEquals(StreamScreenType.PORTFOLIO, StreamScreenType.from("PORTFOLIO"));
    }

    @Test
    void fromFallsBackToGenericWhenUnknown() {
        assertEquals(StreamScreenType.GENERIC, StreamScreenType.from(null));
        assertEquals(StreamScreenType.GENERIC, StreamScreenType.from(""));
        assertEquals(StreamScreenType.GENERIC, StreamScreenType.from("unknown-screen"));
    }
}

