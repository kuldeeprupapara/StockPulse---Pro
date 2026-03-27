package com.stockmarket.query.config;

import com.stockmarket.query.service.IndicesDataService;
import com.stockmarket.query.service.MarketCalendarService;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SchedularConfig {

	private final IndicesDataService indicesDataService;
	private final MarketCalendarService marketCalendarService;
	private final SchedulerIndexProperties schedulerIndexProperties;

	@Scheduled(fixedDelayString = "${scheduler.index.fixed-delay:30000}")
	public void fetchIndexData() {
		Set<Integer> configuredCountryIds = new LinkedHashSet<>(schedulerIndexProperties.getEnabledCountryIds());
		if (configuredCountryIds.isEmpty()) {
			log.debug("Skipping index sync: no enabled countries configured");
			return;
		}

		for (Integer countryId : configuredCountryIds) {
			if (!marketCalendarService.isTodayTradingDay(countryId)) {
				log.debug("Skipping country {}: today is not a trading day", countryId);
				continue;
			}

			if (!marketCalendarService.isMarketOpen(countryId)) {
				LocalTime timeUntilOpen = marketCalendarService.getTimeUntilMarketOpen(countryId);
				if (timeUntilOpen != null) {
					log.debug("Skipping country {}: market not open yet, opens in {}", countryId, timeUntilOpen);
				} else {
					log.debug("Skipping country {}: market session is closed for today", countryId);
				}
				continue;
			}

			try {
				indicesDataService.storeIndexData(countryId);
			} catch (Exception e) {
				log.error("Scheduled index sync failed for countryId {}", countryId, e);
			}
		}
	}
}
