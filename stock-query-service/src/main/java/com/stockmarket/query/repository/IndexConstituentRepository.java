package com.stockmarket.query.repository;

import com.stockmarket.query.entity.IndexConstituent;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface IndexConstituentRepository extends JpaRepository<IndexConstituent, Integer> {

	@Query(value = """
			SELECT DISTINCT ON (ic.symbolid)
				ic.indexid,
				ic.symbolid,
				s.symbolname,
				sd.exchange,
				CONCAT(s.symbolname, COALESCE(me.yahoo_suffix, '')) AS complete_symbol
			FROM index_constituents ic
			JOIN symbol s
				ON s.symbolid = ic.symbolid
			JOIN symbol_details sd
				ON sd.symbolid = ic.symbolid
			JOIN master_exchange me
				ON me.exchangecode = sd.exchange
			WHERE ic.indexid = :indexId
			  AND COALESCE(ic.isactive, true) = true
			  AND me.is_active = true
			  AND me.exchangecode IN (:exchanges)
			ORDER BY
				ic.symbolid,
				COALESCE(me.yahoo_suffix, ''),
				sd.detailid
			""", nativeQuery = true)
	List<Map<String, Object>> getConstituentSymbolsForIndex(Integer indexId, List<String> exchanges);

	@Query(value = """
			SELECT
				COALESCE(sd.name, s.symbolname)                  AS name,
				q.market_cap                                     AS marketCap,
				q.fifty_two_week_low                             AS fiftyTwoWeekLow,
				q.fifty_two_week_high                            AS fiftyTwoWeekHigh,
				q.regular_market_price                           AS ltp,
				q.regular_market_change_percent                  AS regularMarketChangePercent,
				CASE
					WHEN q.fifty_two_week_low IS NULL
						OR q.fifty_two_week_low = 0
						OR q.regular_market_price IS NULL
					THEN NULL
					ELSE ((q.regular_market_price - q.fifty_two_week_low) / q.fifty_two_week_low) * 100
				END                                              AS fiftyTwoWeekChangePercent
			FROM index_constituents ic
			INNER JOIN symbol s
				ON s.symbolid = ic.symbolid
			CROSS JOIN LATERAL (
				SELECT me.countryid, COALESCE(me.yahoo_suffix, '') AS yahoo_suffix
				FROM master_exchange me
				WHERE me.exchangecode = :exchange
				  AND me.is_active = true
				LIMIT 1
			) selected_exchange
			LEFT JOIN LATERAL (
				SELECT d.name
				FROM symbol_details d
				JOIN master_exchange me_d
				  ON me_d.exchangecode = d.exchange
				WHERE d.symbolid = s.symbolid
				  AND me_d.is_active = true
				  AND me_d.countryid = selected_exchange.countryid
				  AND COALESCE(me_d.yahoo_suffix, '') = selected_exchange.yahoo_suffix
				ORDER BY
					d.detailid
				LIMIT 1
			) sd ON true
			LEFT JOIN LATERAL (
				SELECT
					sqe.market_cap,
					sqe.fifty_two_week_low,
					sqe.fifty_two_week_high,
					sqe.regular_market_price,
					sqe.regular_market_change_percent,
					sqe.updated_at
				FROM stock_quote_events sqe
				JOIN master_exchange me_q
				  ON me_q.exchangecode = sqe.exchange
				WHERE sqe.symbol_id = s.symbolid
				  AND me_q.is_active = true
				  AND me_q.countryid = selected_exchange.countryid
				  AND COALESCE(me_q.yahoo_suffix, '') = selected_exchange.yahoo_suffix
				ORDER BY sqe.updated_at DESC NULLS LAST
				LIMIT 1
			) q ON true
			WHERE ic.indexid = :indexId
			  AND COALESCE(ic.isactive, true) = true
			ORDER BY q.market_cap DESC NULLS LAST
			LIMIT 10
			""", nativeQuery = true)
	List<Map<String, Object>> getTopConstituentsByMarketCap(Integer indexId, String exchange);
}