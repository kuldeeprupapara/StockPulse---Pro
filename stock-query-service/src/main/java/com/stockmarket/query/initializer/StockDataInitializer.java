package com.stockmarket.query.initializer;

import com.stockmarket.query.dto.SymbolFetchRequest;
import com.stockmarket.query.interfaces.StockQuoteProvider;
import com.stockmarket.query.repository.StockQuoteEventRepository;
import com.stockmarket.query.service.MarketCalendarService;
import com.stockmarket.util.CommonUtils;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes stock data on application startup based on market calendar.
 * Updates quotes for symbols that already exist in stock_quote_events table.
 *
 * @author kuldeep rupapara
 * @version 1.0
 * @since 2026-01-29
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StockDataInitializer implements ApplicationRunner {

    private final MarketCalendarService marketCalendar;
    private final StockQuoteEventRepository stockQuoteEventRepository;
    private final StockQuoteProvider stockQuoteProvider;

    private static final int BATCH_SIZE = 50;

    @Override
    public void run(ApplicationArguments args) {
        log.info("==================== Stock Data Initialization Started ====================");

        try {
            LocalDate today = CommonUtils.CURRENT_DATE;
            LocalDate targetDate;

            // Step 1: Check if today is a trading day
            boolean isTradingDay = marketCalendar.isTradingDay(today);

            if (isTradingDay) {
                log.info("Trading day found, Date: {}", today);
                targetDate = today;
            } else {
                targetDate = marketCalendar.getPreviousTradingDay(today);
                log.info("Today ({}) is NOT a trading day. Loading data from: {}", today, targetDate);
            }

            // Step 2: Get all symbols that exist in stock_quote_events table
            List<Map<String, Object>> symbolData = stockQuoteEventRepository.getAllSymbolsFromQuoteEvents();

            if (symbolData.isEmpty()) {
                log.warn("No symbols found in stock_quote_events table. Skipping initialization.");
                log.info("==================== Stock Data Initialization Finished ====================");
                return;
            }

            log.info("Found {} existing symbol-exchange combinations in stock_quote_events for date: {}",
                    symbolData.size(), targetDate);

            // Step 3: Prepare symbol fetch requests
            List<SymbolFetchRequest> fetchRequests = prepareSymbolRequests(symbolData);

            if (fetchRequests.isEmpty()) {
                log.warn("No valid symbol fetch requests prepared. Skipping initialization.");
                log.info("==================== Stock Data Initialization Finished ====================");
                return;
            }

            log.info("Prepared {} symbol fetch requests for update", fetchRequests.size());

            // Step 4: Process in batches
            int totalProcessed = 0;
            int totalSuccess = 0;

            for (int i = 0; i < fetchRequests.size(); i += BATCH_SIZE) {
                int endIndex = Math.min(i + BATCH_SIZE, fetchRequests.size());
                List<SymbolFetchRequest> batch = fetchRequests.subList(i, endIndex);

                int batchNumber = (i / BATCH_SIZE) + 1;
                int totalBatches = (fetchRequests.size() + BATCH_SIZE - 1) / BATCH_SIZE;

                log.info("Processing batch {}/{} ({} symbols)...",
                        batchNumber, totalBatches, batch.size());

                try {
                    int successCount = stockQuoteProvider.fetchAndSaveQuotesBatch(batch);
                    totalSuccess += successCount;
                    totalProcessed += batch.size();

                    log.info("Batch {}/{} completed. Success: {}/{}",
                            batchNumber, totalBatches, successCount, batch.size());

                    // Small delay between batches to avoid overwhelming the API
                    if (i + BATCH_SIZE < fetchRequests.size()) {
                        Thread.sleep(1000); // 1 second delay
                    }

                } catch (Exception e) {
                    totalProcessed += batch.size();
                    log.error("Error processing batch {}/{}", batchNumber, totalBatches, e);
                }
            }

            int failureCount = totalProcessed - totalSuccess;
            double successRate = totalProcessed > 0 ? (totalSuccess * 100.0 / totalProcessed) : 0;

            log.info("Stock data initialization completed.");
            log.info("Target Date: {}", targetDate);
            log.info("Total Processed: {}, Success: {}, Failed: {}, Success Rate: {:.2f}%",
                    totalProcessed, totalSuccess, failureCount, successRate);
            log.info("==================== Stock Data Initialization Finished ====================");

        } catch (Exception e) {
            log.error("Stock Data Initialization Failed with critical error", e);
        }
    }

    /**
     * Prepare symbol fetch requests from stock_quote_events table data.
     * Maps raw database rows to SymbolFetchRequest objects.
     */
    private List<SymbolFetchRequest> prepareSymbolRequests(List<Map<String, Object>> symbolData) {
        List<SymbolFetchRequest> requests = new ArrayList<>();

        for (Map<String, Object> row : symbolData) {
            try {
                Integer symbolId = (Integer) row.get("symbolid");
                String symbolName = (String) row.get("symbolname");
                String exchange = (String) row.get("exchange");
                String fullSymbol = (String) row.get("full_symbol");
                Long eventId = ((Number) row.get("eventid")).longValue();

                // Only add if full_symbol is valid
                if (fullSymbol != null && !fullSymbol.isEmpty() && !fullSymbol.endsWith(".")) {
                    requests.add(new SymbolFetchRequest(
                            eventId,
                            symbolId,
                            symbolName,      // e.g., "GOENKA"
                            fullSymbol,      // e.g., "GOENKA.NS"
                            exchange        // e.g., "NSE", "NSI", "BSE"
                    ));

                    log.debug("Added fetch request for existing quote: {} ({})", fullSymbol, exchange);
                } else {
                    log.warn("Skipping invalid symbol from stock_quote_events: symbolId={}, symbolName={}, exchange={}",
                            symbolId, symbolName, exchange);
                }
            } catch (Exception e) {
                log.error("Failed to parse symbol data from stock_quote_events: {}", row, e);
            }
        }

        log.info("Prepared {} valid symbol fetch requests from {} database rows",
                requests.size(), symbolData.size());
        return requests;
    }
}
