package com.stockmarket.util;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class CommonMarketUtils {

    private CommonMarketUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    public static String normalizeExchange(String exchange) {
        if (exchange == null) {
            return null;
        }
        String normalized = exchange.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return null;
        }
        return "NSI".equals(normalized) ? "NSE" : normalized;
    }

    public static String normalizeCountryCode(String countryCode) {
        if (countryCode == null) {
            return null;
        }
        String normalized = countryCode.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    public static boolean matchesExchange(String actualExchange, String requestedExchange) {
        String normalizedRequested = normalizeExchange(requestedExchange);
        if (normalizedRequested == null) {
            return true;
        }
        return normalizedRequested.equals(normalizeExchange(actualExchange));
    }

    public static String resolveCountryCodeByExchange(String exchange, Map<String, String> countryByExchange) {
        String normalizedExchange = normalizeExchange(exchange);
        if (normalizedExchange == null || countryByExchange == null || countryByExchange.isEmpty()) {
            return null;
        }
        return countryByExchange.get(normalizedExchange);
    }

    public static String defaultExchangeByCountry(String countryCode, Map<String, String> exchangeByCountry) {
        String normalizedCountry = normalizeCountryCode(countryCode);
        if (normalizedCountry == null || exchangeByCountry == null || exchangeByCountry.isEmpty()) {
            return null;
        }
        return normalizeExchange(exchangeByCountry.get(normalizedCountry));
    }

    public static List<String> normalizeExchanges(List<String> exchanges) {
        if (exchanges == null || exchanges.isEmpty()) {
            return List.of();
        }
        return exchanges.stream()
                .map(CommonMarketUtils::normalizeExchange)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    public static String buildDisplaySymbol(String symbolName, String exchange, Map<String, String> yahooSuffixByExchange) {
        if (symbolName == null || symbolName.isBlank()) {
            return symbolName;
        }
        if (yahooSuffixByExchange == null || yahooSuffixByExchange.isEmpty()) {
            return symbolName;
        }

        String normalizedExchange = normalizeExchange(exchange);
        if (normalizedExchange == null) {
            return symbolName;
        }

        String suffix = yahooSuffixByExchange.get(normalizedExchange);
        if (suffix == null || suffix.isBlank()) {
            return symbolName;
        }
        return symbolName + suffix;
    }

    public static ZoneId resolveZoneId(
            String countryCode,
            String exchange,
            ZoneId defaultZone,
            Map<String, ZoneId> zoneByCountry,
            Map<String, ZoneId> zoneByExchange
    ) {
        ZoneId fallback = defaultZone == null ? ZoneId.of("UTC") : defaultZone;

        String normalizedExchange = normalizeExchange(exchange);
        if (normalizedExchange != null && zoneByExchange != null) {
            ZoneId zone = zoneByExchange.get(normalizedExchange);
            if (zone != null) {
                return zone;
            }
        }

        if (countryCode != null && !countryCode.isBlank() && zoneByCountry != null) {
            ZoneId zone = zoneByCountry.get(countryCode.trim().toUpperCase(Locale.ROOT));
            if (zone != null) {
                return zone;
            }
        }

        return fallback;
    }

    public static boolean isMarketOpen(ZonedDateTime now, LocalTime marketOpen, LocalTime marketClose) {
        Objects.requireNonNull(now, "now must not be null");
        Objects.requireNonNull(marketOpen, "marketOpen must not be null");
        Objects.requireNonNull(marketClose, "marketClose must not be null");

        LocalTime current = now.toLocalTime();
        return !current.isBefore(marketOpen) && !current.isAfter(marketClose);
    }
}

