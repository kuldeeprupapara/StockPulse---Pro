package com.stockmarket.config;

import java.util.Locale;

public enum StreamScreenType {
    LISTING,
    DETAIL,
    SEARCH_SELECTED,
    WATCHLIST,
    PORTFOLIO,
    GENERIC;

    public static StreamScreenType from(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return GENERIC;
        }

        String normalized = rawValue.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        try {
            return StreamScreenType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return GENERIC;
        }
    }
}

