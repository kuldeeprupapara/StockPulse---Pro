package com.stockmarket.query.repository;

import com.stockmarket.query.entity.IndexQuote;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface IndexQuoteRepository extends JpaRepository<IndexQuote, Integer> {

    @Query(value = """
            select iq.indexid, mi.index_symbol , iq.open, iq.high, iq.low, iq.close, iq.change, iq.change_percent, mi.isfeatured 
            from index_quote iq
            inner join master_index mi 
            on mi.indexid  = iq.indexid
            and mi.countryid =:countryId
                """, nativeQuery = true)
    List<Map<String,Object>> getIndexQuoteDetails(Integer countryId);
}