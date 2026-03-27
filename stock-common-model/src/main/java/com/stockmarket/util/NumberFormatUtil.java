package com.stockmarket.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public final class NumberFormatUtil {

    private NumberFormatUtil() {}

    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");
    private static final BigDecimal ONE_BILLION = new BigDecimal("1000000000");
    private static final BigDecimal ONE_TRILLION = new BigDecimal("1000000000000");

    // ⚠️ NumberFormat is NOT thread-safe — never store as static field!
    // Use ThreadLocal so each thread gets its own instance
    private static final ThreadLocal<NumberFormat> INDIAN_FORMAT =
            ThreadLocal.withInitial(() -> {
                NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("en", "IN"));
                fmt.setMinimumFractionDigits(2);
                fmt.setMaximumFractionDigits(2);
                return fmt;
            });

    public static String formatIndianPrice(double price) {
        return INDIAN_FORMAT.get().format(price);  // each thread uses its own instance
    }

    public static String formatChangePercent(double value) {
        // String.format() is stateless — thread-safe
        return String.format("%s%.2f%%", value >= 0 ? "+" : "", value);
    }

    public static double roundTo(double value, int places) {
        // BigDecimal.valueOf() creates new object — thread-safe
        return BigDecimal.valueOf(value)
                .setScale(places, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public static BigDecimal roundToTwo(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    // Compact market-cap representation for UI: 12.56M, 4.37B, 1.02T
    public static String formatMarketCap(BigDecimal marketCap) {
        if (marketCap == null) {
            return null;
        }

        BigDecimal abs = marketCap.abs();
        if (abs.compareTo(ONE_TRILLION) >= 0) {
            return roundToTwo(marketCap.divide(ONE_TRILLION, 2, RoundingMode.HALF_UP)).toPlainString() + "T";
        }
        if (abs.compareTo(ONE_BILLION) >= 0) {
            return roundToTwo(marketCap.divide(ONE_BILLION, 2, RoundingMode.HALF_UP)).toPlainString() + "B";
        }
        if (abs.compareTo(ONE_MILLION) >= 0) {
            return roundToTwo(marketCap.divide(ONE_MILLION, 2, RoundingMode.HALF_UP)).toPlainString() + "M";
        }
        return roundToTwo(marketCap).toPlainString();
    }

    public static String formatVolume(long volume) {
        if (volume >= 10_000_000) return String.format("%.2fCr", volume / 10_000_000.0);
        if (volume >= 100_000)    return String.format("%.2fL",  volume / 100_000.0);
        if (volume >= 1_000)      return String.format("%.2fK",  volume / 1_000.0);
        return String.valueOf(volume);
    }

    // ── IMPORTANT: Call this in filter/interceptor to prevent memory leaks
    // in thread-pool environments (Tomcat reuses threads!)
    public static void clean() {
        INDIAN_FORMAT.remove();
    }
}
