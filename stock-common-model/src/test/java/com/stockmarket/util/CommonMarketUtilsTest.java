package com.stockmarket.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CommonMarketUtilsTest {

    @Test
    void normalizeExchangeTreatsNsiAsNse() {
        assertEquals("NSE", CommonMarketUtils.normalizeExchange("nsi"));
        assertEquals("NSE", CommonMarketUtils.normalizeExchange(" NSE "));
        assertNull(CommonMarketUtils.normalizeExchange("  "));
    }

    @Test
    void buildDisplaySymbolUsesConfiguredSuffix() {
        Map<String, String> suffixByExchange = Map.of("NSE", ".NS", "BSE", ".BO");

        assertEquals("RELIANCE.NS", CommonMarketUtils.buildDisplaySymbol("RELIANCE", "NSI", suffixByExchange));
        assertEquals("TCS.BO", CommonMarketUtils.buildDisplaySymbol("TCS", "BSE", suffixByExchange));
        assertEquals("AAPL", CommonMarketUtils.buildDisplaySymbol("AAPL", "NMS", suffixByExchange));
    }

    @Test
    void resolveZoneIdPrefersExchangeThenCountryThenDefault() {
        ZoneId zone = CommonMarketUtils.resolveZoneId(
                "IN",
                "NSE",
                ZoneId.of("UTC"),
                Map.of("IN", ZoneId.of("Asia/Kolkata")),
                Map.of("NSE", ZoneId.of("Asia/Kolkata"))
        );
        assertEquals(ZoneId.of("Asia/Kolkata"), zone);

        ZoneId fallbackCountry = CommonMarketUtils.resolveZoneId(
                "US",
                null,
                ZoneId.of("UTC"),
                Map.of("US", ZoneId.of("America/New_York")),
                Map.of()
        );
        assertEquals(ZoneId.of("America/New_York"), fallbackCountry);

        ZoneId fallbackDefault = CommonMarketUtils.resolveZoneId(
                null,
                null,
                ZoneId.of("UTC"),
                Map.of(),
                Map.of()
        );
        assertEquals(ZoneId.of("UTC"), fallbackDefault);
    }

    @Test
    void isMarketOpenReturnsExpectedWindow() {
        ZonedDateTime inWindow = ZonedDateTime.of(2026, 3, 14, 10, 0, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime afterClose = ZonedDateTime.of(2026, 3, 14, 18, 0, 0, 0, ZoneId.of("UTC"));

        assertTrue(CommonMarketUtils.isMarketOpen(inWindow, LocalTime.of(9, 0), LocalTime.of(16, 0)));
        assertFalse(CommonMarketUtils.isMarketOpen(afterClose, LocalTime.of(9, 0), LocalTime.of(16, 0)));
    }

    @Test
    void resolveCountryCodeByExchangeUsesNormalizedExchange() {
        Map<String, String> countryByExchange = Map.of("NSE", "IN", "NMS", "US");
        assertEquals("IN", CommonMarketUtils.resolveCountryCodeByExchange("nsi", countryByExchange));
        assertEquals("US", CommonMarketUtils.resolveCountryCodeByExchange("NMS", countryByExchange));
        assertNull(CommonMarketUtils.resolveCountryCodeByExchange("", countryByExchange));
    }

    @Test
    void defaultExchangeByCountryUsesNormalizedCountryCode() {
        Map<String, String> exchangeByCountry = Map.of("IN", "NSI", "US", "NMS");
        assertEquals("NSE", CommonMarketUtils.defaultExchangeByCountry(" in ", exchangeByCountry));
        assertEquals("NMS", CommonMarketUtils.defaultExchangeByCountry("US", exchangeByCountry));
        assertNull(CommonMarketUtils.defaultExchangeByCountry("BR", exchangeByCountry));
    }

    @Test
    void normalizeExchangesRemovesDuplicatesAndInvalidValues() {
        assertEquals(List.of("NSE", "BSE"), CommonMarketUtils.normalizeExchanges(List.of("nsi", "NSE", "", "BSE")));
        assertTrue(CommonMarketUtils.normalizeExchanges(null).isEmpty());
    }
}

