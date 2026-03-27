package com.stockmarket.util;

import java.util.Collection;

public final class ValidationUtil {

    private ValidationUtil() {}

    // All methods are purely stateless — parameters live on the stack
    // No shared fields = inherently thread-safe

    public static boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isNullOrEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static double defaultIfNull(Double value, double fallback) {
        return value != null ? value : fallback;
    }

    public static String defaultIfBlank(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static boolean isValidSymbol(String symbol) {
        // No shared state — regex applied on local parameter
        return symbol != null && symbol.matches("^[A-Z0-9\\-\\.]{1,20}$");
    }
}
