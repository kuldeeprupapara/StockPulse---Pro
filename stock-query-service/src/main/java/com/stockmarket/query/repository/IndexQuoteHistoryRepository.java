package com.stockmarket.query.repository;

import com.stockmarket.query.entity.IndexQuoteHistory;
import com.stockmarket.query.entity.composite.IndexQuoteHistoryId;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface IndexQuoteHistoryRepository extends JpaRepository<IndexQuoteHistory, IndexQuoteHistoryId> {

	@Query(value = """
			SELECT
				to_timestamp(
					floor(extract(epoch FROM created_at) / (:bucketMinutes * 60)) * (:bucketMinutes * 60)
				) AT TIME ZONE 'UTC' AS bucketTime,
				(array_agg(price ORDER BY created_at ASC))[1]  AS open,
				MAX(price)                                     AS high,
				MIN(price)                                     AS low,
				(array_agg(price ORDER BY created_at DESC))[1] AS close,
				SUM(COALESCE(volume, 0))                       AS volume
			FROM index_quote_history
			WHERE indexid = :indexId
			  AND created_at >= :fromTs
			  AND created_at <= :toTs
			GROUP BY bucketTime
			ORDER BY bucketTime
			""", nativeQuery = true)
	List<Map<String, Object>> getIndexOhlc(
			Integer indexId,
			OffsetDateTime fromTs,
			OffsetDateTime toTs,
			Integer bucketMinutes
	);
}