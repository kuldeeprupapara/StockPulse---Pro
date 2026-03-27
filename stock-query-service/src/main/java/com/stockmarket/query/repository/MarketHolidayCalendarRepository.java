package com.stockmarket.query.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MarketHolidayCalendarRepository {

    private final JdbcTemplate jdbcTemplate;

    public MarketHolidayCalendarRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<LocalDate> findHolidayDatesByCountry(Integer countryId) {
        String sql = """
                SELECT holiday_date
                FROM market_holiday_calendar
                WHERE countryid = ?
                  AND is_trading_day = false
                ORDER BY holiday_date
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getDate("holiday_date").toLocalDate(), countryId);
    }
}

