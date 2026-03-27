package com.stockmarket.util;

import java.time.*;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtil {

    private DateTimeUtil() {}

    // DateTimeFormatter is immutable & thread-safe in Java 8+
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final LocalTime MARKET_OPEN  = LocalTime.of(9, 15);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(15, 30);
    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public static ZonedDateTime nowIST() {
        // ZonedDateTime.now() is stateless — new object per call, thread-safe
        return ZonedDateTime.now(IST);
    }

    public static boolean isMarketOpen() {
        ZonedDateTime now = nowIST();  // local variable — stack-bound, thread-safe
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) return false;
        LocalTime time = now.toLocalTime();
        return !time.isBefore(MARKET_OPEN) && !time.isAfter(MARKET_CLOSE);
    }

    public static String formatToDisplay(LocalDateTime dt) {
        // DISPLAY_FMT is immutable, dt is passed in — no shared state
        return dt != null ? dt.format(DISPLAY_FMT) : "";
    }

    public static long toEpochMillis(LocalDateTime dt) {
        return dt.atZone(IST).toInstant().toEpochMilli();
    }

    public static LocalDate today() {
        return LocalDate.now(IST);
    }
}
