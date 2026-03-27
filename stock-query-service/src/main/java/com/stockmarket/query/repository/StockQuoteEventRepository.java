package com.stockmarket.query.repository;

import com.stockmarket.query.entity.StockQuoteEvents;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface StockQuoteEventRepository extends JpaRepository<StockQuoteEvents, Long>,
        JpaSpecificationExecutor<StockQuoteEvents> {

    @Query(value = """
    SELECT symbol_id as symbolId,
           regular_market_price as LTP,
           regular_market_day_high as marketDayHigh,
           regular_market_day_low as marketDayLow,
           regular_market_day_open as marketDayOpen,
           regular_market_change as marketChange,
           regular_market_change_percent as marketChangePercent,
           trailing_pe as trailingPe,
           forward_pe as forwardPe,
           fifty_two_week_low as fiftyTwoWeekLow,
           fifty_two_week_high as fiftyTwoWeekHigh
        FROM stock_quote_events
        WHERE symbol_id in (:symbolIds) AND exchange=:exchange;
        """, nativeQuery = true)
    List<Map<String,Object>> getStockQuotes(List<Integer> symbolIds, String exchange);

    @Query(value = """
        SELECT
            COUNT(*) AS total,
            SUM(CASE WHEN regular_market_change > 0 THEN 1 ELSE 0 END) AS gainers,
            SUM(CASE WHEN regular_market_change < 0 THEN 1 ELSE 0 END) AS losers,
            SUM(CASE WHEN regular_market_change = 0 THEN 1 ELSE 0 END) AS unchanged
        FROM stock_quote_events
        WHERE (:exchange IS NULL OR exchange = :exchange)
        """, nativeQuery = true)
    Map<String, Object> getMarketBreadth(String exchange);

    @Query(value = """
        SELECT
            sym.symbolid AS symbolId,
            sym.symbolname AS symbolName,
            sqe.exchange AS exchange,
            sqe.regular_market_price AS price,
            sqe.regular_market_change AS change,
            sqe.regular_market_change_percent AS changePercent,
            sqe.updated_at AS updatedAt
        FROM stock_quote_events sqe
        INNER JOIN symbol sym ON sym.symbolid = sqe.symbol_id
        WHERE (:exchange IS NULL OR sqe.exchange = :exchange)
        ORDER BY sqe.regular_market_change_percent DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Map<String, Object>> getTopGainers(String exchange, Integer limit);

    @Query(value = """
        SELECT
            sym.symbolid AS symbolId,
            sym.symbolname AS symbolName,
            sqe.exchange AS exchange,
            sqe.regular_market_price AS price,
            sqe.regular_market_change AS change,
            sqe.regular_market_change_percent AS changePercent,
            sqe.updated_at AS updatedAt
        FROM stock_quote_events sqe
        INNER JOIN symbol sym ON sym.symbolid = sqe.symbol_id
        WHERE (:exchange IS NULL OR sqe.exchange = :exchange)
        ORDER BY sqe.regular_market_change_percent ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<Map<String, Object>> getTopLosers(String exchange, Integer limit);

    /**
     * Get all distinct symbols from stock_quote_events table with their exchange details.
     * Only fetches symbols that have existing quote data.
     */
    @Query(value = """
        SELECT DISTINCT
            sqe.eventid,        
            sym.symbolid,
            sym.symbolname,
            sqe.exchange,
            CONCAT(sym.symbolname, 
                CASE 
                    WHEN sqe.exchange IN ('NSE', 'NSI') THEN '.NS'
                    WHEN sqe.exchange = 'BSE' THEN '.BO'
                    ELSE ''
                END
            ) AS full_symbol
        FROM stock_quote_events sqe
        INNER JOIN symbol sym ON sqe.symbol_id = sym.symbolid
        INNER JOIN symbol_details sdt ON sdt.symbolid = sym.symbolid 
        WHERE sqe.exchange IN ('NSE', 'NSI', 'BSE')
        ORDER BY sym.symbolname, sqe.exchange
        """, nativeQuery = true)
    List<Map<String, Object>> getAllSymbolsFromQuoteEvents();

    List<StockQuoteEvents> findBySymbol_SymbolIdInAndExchangeIn(List<Integer> symbolIds, List<String> exchanges);

    Optional<StockQuoteEvents> findBySymbol_SymbolIdAndExchange(Integer symbolId, String exchange);
}