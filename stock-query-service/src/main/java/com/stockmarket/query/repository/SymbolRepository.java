package com.stockmarket.query.repository;

import com.stockmarket.query.entity.Symbol;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SymbolRepository extends JpaRepository<Symbol, Integer> {

    @Query(value = """
        SELECT
            sym.symbolid,
            sym.symbolname,
            sdt.detailid,
            MAX(CASE WHEN me.yahoo_suffix = '.NS'
                THEN CONCAT(sym.symbolname, me.yahoo_suffix) END) AS nse_symbol,
            MAX(CASE WHEN me.yahoo_suffix = '.BO'
                THEN CONCAT(sym.symbolname, me.yahoo_suffix) END) AS bse_symbol
        FROM symbol sym
        INNER JOIN symbol_details sdt ON sdt.symbolid = sym.symbolid
        INNER JOIN master_exchange me ON me.exchangecode = sdt.exchange
        WHERE sym.symbolname IN (:symbol)
        AND me.exchangecode IN (:exchange)
        AND me.is_active = true
        GROUP BY sym.symbolid, sdt.detailid, sym.symbolname
        """, nativeQuery = true)
    List<Map<String, Object>> getSymbol(List<String> symbol, List<String> exchange);

    /**
     * Get all symbols that exist in symbol_details table with their exchange info.
     * Only fetches symbols that have entries in symbol_details.
     */
    @Query(value = """
        SELECT DISTINCT
            sym.symbolid,
            sym.symbolname,
            sdt.detailid,
            sdt.exchange,
            CONCAT(sym.symbolname, COALESCE(me.yahoo_suffix, '')) AS full_symbol
        FROM symbol_details sdt
        INNER JOIN symbol sym ON sdt.symbolid = sym.symbolid
        INNER JOIN master_exchange me ON me.exchangecode = sdt.exchange
        WHERE me.is_active = true
        AND sdt.type = 'EQUITY'
        ORDER BY sym.symbolname, sdt.exchange
        """, nativeQuery = true)
    List<Map<String, Object>> getAllSymbolsFromDetails();

    Boolean existsBySymbolname(String symbol);


    @Query(value = """
            SELECT 
                w.symbolname,
                CASE 
                    WHEN sym.symbolid IS NULL THEN 0
                    ELSE 1
                END AS status,
                sym.symbolid,
                mi.indexid,
                mi.indexname,
                mi.index_symbol
            FROM (VALUES
                ('HDFCBANK'),
                ('ICICIBANK'),
                ('RELIANCE'),
                ('BHARTIARTL'),
                ('INFY'),
                ('LT'),
                ('SBIN'),
                ('AXISBANK'),
                ('TCS'),
                ('M&M'),
                ('ITC'),
                ('KOTAKBANK'),
                ('BAJFINANCE'),
                ('ETERNAL'),
                ('MARUTI'),
                ('HINDUNILVR'),
                ('SUNPHARMA'),
                ('HCLTECH'),
                ('TITAN'),
                ('NTPC'),
                ('TATASTEEL'),
                ('ULTRACEMCO'),
                ('HINDALCO'),
                ('SHRIRAMFIN'),
                ('POWERGRID'),
                ('BAJAJFINSV'),
                ('JSWSTEEL'),
                ('GRASIM'),
                ('BAJAJ-AUTO'),
                ('INDIGO'),
                ('EICHERMOT'),
                ('ADANIPORTS'),
                ('COALINDIA'),
                ('JIOFIN'),
                ('NESTLEIND'),
                ('SBILIFE'),
                ('TRENT'),
                ('MAXHEALTH'),
                ('TATACONSUM'),
                ('APOLLOHOSP'),
                ('WIPRO'),
                ('DRREDDY'),
                ('TMPV'),
                ('HDFCLIFE'),
                ('CIPLA'),
                ('ASIANPAINT'),
                ('ONGC'),
                ('TECHM'),
                ('BEL'),
                ('ADANIENT')
            ) AS w(symbolname)
            LEFT JOIN symbol sym ON sym.symbolname = w.symbolname,
            (SELECT indexid, indexname, index_symbol 
             FROM master_index 
             WHERE indexid =:indexId) mi  
            ORDER BY status, w.symbolname
            """, nativeQuery = true
        )
    List<Map<String, Object>> getSymbolListForIndex(Integer indexId);
}
