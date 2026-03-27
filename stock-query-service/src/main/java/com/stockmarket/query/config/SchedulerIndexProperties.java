package com.stockmarket.query.config;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scheduler.index")
@Getter
@Setter
public class SchedulerIndexProperties {

    // Countries that scheduler is allowed to process.
    private List<Integer> enabledCountryIds = new ArrayList<>();

    // Neutral defaults used when a country-specific schedule is missing.
    private String defaultZoneId = "UTC";
    private LocalTime defaultMarketOpen = LocalTime.MIDNIGHT;
    private LocalTime defaultMarketClose = LocalTime.of(23, 59);
    private String defaultHolidayFile = "";

    // Per-country market timing/zone/holiday file settings.
    private Map<Integer, CountrySchedule> countrySchedules = new HashMap<>();

    @Getter
    @Setter
    public static class CountrySchedule {
        private String zoneId;
        private LocalTime marketOpen;
        private LocalTime marketClose;
        private String holidayFile;
    }
}

