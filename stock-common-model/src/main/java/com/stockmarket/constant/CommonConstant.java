package com.stockmarket.constant;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class CommonConstant {
    public CommonConstant(){
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    // Time zone specific constant
    public static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");


    // various date time formatter

    public static final DateTimeFormatter CSV_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yy");

}
