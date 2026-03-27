package com.stockmarket.query.service;

import com.stockmarket.query.config.SchedulerIndexProperties;
import com.stockmarket.query.repository.MarketHolidayCalendarRepository;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import static com.stockmarket.constant.CommonConstant.CSV_DATE_FORMAT;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketCalendarService {

    private static final int DEFAULT_COUNTRY_ID = 1;

    private final SchedulerIndexProperties schedulerIndexProperties;
    private final MarketHolidayCalendarRepository marketHolidayCalendarRepository;

    private final Map<Integer, Set<LocalDate>> holidaysByCountry = new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() {
        loadCountryHolidayCalendars();
    }

    private void loadCountryHolidayCalendars() {
        Set<Integer> countryIds = new LinkedHashSet<>();
        countryIds.addAll(schedulerIndexProperties.getEnabledCountryIds());
        countryIds.addAll(schedulerIndexProperties.getCountrySchedules().keySet());

        if (countryIds.isEmpty()) {
            countryIds.add(DEFAULT_COUNTRY_ID);
        }

        for (Integer countryId : countryIds) {
            Set<LocalDate> dbHolidays = loadHolidaysFromDb(countryId);
            if (!dbHolidays.isEmpty()) {
                holidaysByCountry.put(countryId, dbHolidays);
                continue;
            }

            SchedulerIndexProperties.CountrySchedule schedule =
                    schedulerIndexProperties.getCountrySchedules().get(countryId);
            String holidayFile = schedule == null || schedule.getHolidayFile() == null || schedule.getHolidayFile().isBlank()
                    ? schedulerIndexProperties.getDefaultHolidayFile()
                    : schedule.getHolidayFile();
            holidaysByCountry.put(countryId, loadHolidaysFromCsv(holidayFile));
        }
    }

    private Set<LocalDate> loadHolidaysFromDb(Integer countryId) {
        try {
            List<LocalDate> holidays = marketHolidayCalendarRepository.findHolidayDatesByCountry(countryId);
            if (holidays.isEmpty()) {
                return Set.of();
            }
            return new HashSet<>(holidays);
        } catch (Exception ex) {
            log.warn("Failed to load holidays from DB for countryId {}", countryId, ex);
            return Set.of();
        }
    }

    private Set<LocalDate> loadHolidaysFromCsv(String holidayFilePath) {
        Set<LocalDate> holidays = new HashSet<>();

        if (holidayFilePath == null || holidayFilePath.isBlank()) {
            return holidays;
        }

        try {
            ClassPathResource resource = new ClassPathResource(holidayFilePath);
            if (!resource.exists()) {
                log.warn("Holiday file not found on classpath: {}", holidayFilePath);
                return holidays;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream()))) {

                String line;
                int count = 0;

                // Skip the first line (header)
                reader.readLine();

                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    // Skip empty lines and comments
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }

                    try {
                        // Split by comma and take the first column (date)
                        String[] parts = line.split(",");
                        if (parts.length > 0) {
                            String dateStr = parts[0].trim();
                            LocalDate holidayDate = LocalDate.parse(dateStr, CSV_DATE_FORMAT);
                            holidays.add(holidayDate);
                            count++;
                        }

                    } catch (Exception e) {
                        log.warn("Failed to parse holiday date: {}", line);
                    }
                }

                log.info("Successfully loaded {} holidays from {}", count, holidayFilePath);

            }
        } catch (Exception e) {
            log.error("Failed to load holidays from CSV: {}", holidayFilePath, e);
        }

        return holidays;
    }

    /**
     * Checks if the given date is a NSE declared holiday.
     *
     * @param date the date to check
     * @return true if the date is a holiday, false otherwise
     */
    public boolean isHoliday(LocalDate date) {
        return isHoliday(DEFAULT_COUNTRY_ID, date);
    }

    public boolean isHoliday(Integer countryId, LocalDate date) {
        return getHolidays(countryId).contains(date);
    }

    /**
     * Checks if today is a NSE declared holiday.
     *
     * @return true if today is a holiday, false otherwise
     */
    public boolean isTodayHoliday() {
        return isTodayHoliday(DEFAULT_COUNTRY_ID);
    }

    public boolean isTodayHoliday(Integer countryId) {
        return isHoliday(countryId, LocalDate.now(resolveZone(countryId)));
    }

    /**
     * Checks if the given date is a trading day (not a weekend and not a holiday).
     *
     * @param date the date to check
     * @return true if it's a trading day, false otherwise
     */
    public boolean isTradingDay(LocalDate date) {
        return isTradingDay(DEFAULT_COUNTRY_ID, date);
    }

    public boolean isTradingDay(Integer countryId, LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        // Check if weekend
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }

        // Check if holiday
        return !isHoliday(countryId, date);
    }

    /**
     * Checks if today is a trading day.
     *
     * @return true if today is a trading day, false otherwise
     */
    public boolean isTodayTradingDay() {
        return isTodayTradingDay(DEFAULT_COUNTRY_ID);
    }

    public boolean isTodayTradingDay(Integer countryId) {
        return isTradingDay(countryId, LocalDate.now(resolveZone(countryId)));
    }

    /**
     * Checks if the market is currently open for trading.
     * Considers both trading day validation and market hours (9:15 AM - 3:30 PM IST).
     *
     * @return true if market is open, false otherwise
     */
    public boolean isMarketOpen() {
        return isMarketOpen(DEFAULT_COUNTRY_ID);
    }

    public boolean isMarketOpen(Integer countryId) {
        ZoneId zone = resolveZone(countryId);
        LocalDate today = LocalDate.now(zone);

        // Check if it's a trading day
        if (!isTradingDay(countryId, today)) {
            return false;
        }

        // Check market hours
        LocalTime currentTime = LocalTime.now(zone);
        LocalTime marketOpen = getMarketOpen(countryId);
        LocalTime marketClose = getMarketClose(countryId);
        return !currentTime.isBefore(marketOpen)
                && !currentTime.isAfter(marketClose);
    }

    /**
     * Checks if trading orders can be placed right now.
     * Alias for isMarketOpen() for semantic clarity in trading contexts.
     *
     * @return true if orders can be placed, false otherwise
     */
    public boolean canPlaceOrder() {
        return isMarketOpen();
    }

    /**
     * Gets the next trading day after the given date.
     *
     * @param fromDate the starting date
     * @return the next trading day
     */
    public LocalDate getNextTradingDay(LocalDate fromDate) {
        LocalDate nextDay = fromDate.plusDays(1);
        while (!isTradingDay(nextDay)) {
            nextDay = nextDay.plusDays(1);
        }
        return nextDay;
    }

    /**
     * Gets the next trading day from today.
     *
     * @return the next trading day
     */
    public LocalDate getNextTradingDay() {
        return getNextTradingDay(LocalDate.now(resolveZone(DEFAULT_COUNTRY_ID)));
    }

    /**
     * Gets the previous trading day before the given date.
     *
     * @param fromDate the starting date
     * @return the previous trading day
     */
    public LocalDate getPreviousTradingDay(LocalDate fromDate) {
        LocalDate previousDay = fromDate.minusDays(1);
        while (!isTradingDay(previousDay)) {
            previousDay = previousDay.minusDays(1);
        }
        return previousDay;
    }

    /**
     * Gets the previous trading day from today.
     *
     * @return the previous trading day
     */
    public LocalDate getPreviousTradingDay() {
        return getPreviousTradingDay(LocalDate.now(resolveZone(DEFAULT_COUNTRY_ID)));
    }

    /**
     * Counts the number of trading days in a given date range (inclusive).
     *
     * @param startDate start of the range
     * @param endDate end of the range
     * @return number of trading days
     */
    public long countTradingDays(LocalDate startDate, LocalDate endDate) {
        return startDate.datesUntil(endDate.plusDays(1))
                .filter(this::isTradingDay)
                .count();
    }

    /**
     * Gets all trading days in a given date range.
     *
     * @param startDate start of the range
     * @param endDate end of the range
     * @return set of trading days
     */
    public Set<LocalDate> getTradingDaysInRange(LocalDate startDate, LocalDate endDate) {
        return startDate.datesUntil(endDate.plusDays(1))
                .filter(this::isTradingDay)
                .collect(Collectors.toSet());
    }

    /**
     * Gets all holidays in a given date range.
     *
     * @param startDate start of the range
     * @param endDate end of the range
     * @return set of holidays in the range
     */
    public Set<LocalDate> getHolidaysInRange(LocalDate startDate, LocalDate endDate) {
        return getHolidays(DEFAULT_COUNTRY_ID).stream()
                .filter(date -> !date.isBefore(startDate) && !date.isAfter(endDate))
                .collect(Collectors.toSet());
    }

    /**
     * Gets all loaded holidays.
     *
     * @return unmodifiable set of all holidays
     */
    public Set<LocalDate> getAllHolidays() {
        return Set.copyOf(getHolidays(DEFAULT_COUNTRY_ID));
    }

    /**
     * Gets time remaining until market opens.
     *
     * @return LocalTime representing time until market opens, or null if market is open
     */
    public LocalTime getTimeUntilMarketOpen() {
        return getTimeUntilMarketOpen(DEFAULT_COUNTRY_ID);
    }

    public LocalTime getTimeUntilMarketOpen(Integer countryId) {
        ZoneId zone = resolveZone(countryId);
        LocalTime now = LocalTime.now(zone);
        LocalTime marketOpen = getMarketOpen(countryId);

        if (now.isBefore(marketOpen)) {
            return LocalTime.ofSecondOfDay(
                    marketOpen.toSecondOfDay() - now.toSecondOfDay()
            );
        }

        return null; // Market is already open or closed for the day
    }

    /**
     * Gets time remaining until market closes.
     *
     * @return LocalTime representing time until market closes, or null if market is closed
     */
    public LocalTime getTimeUntilMarketClose() {
        return getTimeUntilMarketClose(DEFAULT_COUNTRY_ID);
    }

    public LocalTime getTimeUntilMarketClose(Integer countryId) {
        ZoneId zone = resolveZone(countryId);
        LocalTime now = LocalTime.now(zone);
        LocalTime marketOpen = getMarketOpen(countryId);
        LocalTime marketClose = getMarketClose(countryId);

        if (now.isAfter(marketOpen) && now.isBefore(marketClose)) {
            return LocalTime.ofSecondOfDay(
                    marketClose.toSecondOfDay() - now.toSecondOfDay()
            );
        }
        return null; // Market is not open
    }

    private ZoneId resolveZone(Integer countryId) {
        SchedulerIndexProperties.CountrySchedule schedule = schedulerIndexProperties
                .getCountrySchedules()
                .get(countryId);

        if (schedule == null || schedule.getZoneId() == null || schedule.getZoneId().isBlank()) {
            return ZoneId.of(schedulerIndexProperties.getDefaultZoneId());
        }
        return ZoneId.of(schedule.getZoneId());
    }

    private LocalTime getMarketOpen(Integer countryId) {
        SchedulerIndexProperties.CountrySchedule schedule = schedulerIndexProperties
                .getCountrySchedules()
                .get(countryId);
        return schedule == null || schedule.getMarketOpen() == null
                ? schedulerIndexProperties.getDefaultMarketOpen()
                : schedule.getMarketOpen();
    }

    private LocalTime getMarketClose(Integer countryId) {
        SchedulerIndexProperties.CountrySchedule schedule = schedulerIndexProperties
                .getCountrySchedules()
                .get(countryId);
        return schedule == null || schedule.getMarketClose() == null
                ? schedulerIndexProperties.getDefaultMarketClose()
                : schedule.getMarketClose();
    }

    private Set<LocalDate> getHolidays(Integer countryId) {
        return holidaysByCountry.getOrDefault(countryId, Set.of());
    }
}
