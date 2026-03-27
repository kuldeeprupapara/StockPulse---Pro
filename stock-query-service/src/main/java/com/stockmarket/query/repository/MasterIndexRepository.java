package com.stockmarket.query.repository;

import com.stockmarket.query.entity.MasterIndex;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MasterIndexRepository extends JpaRepository<MasterIndex, Integer> {

    @Query(value = "select indexid, countryid, exchange,displayorder,indexname,index_symbol from master_index where isactive =:isActive and countryid=:countryId", nativeQuery = true)
    List<Map<String,Object>> getIndexDetails(boolean isActive, Integer countryId);

    @Query(value = "select indexid, countryid, exchange, indexname, index_symbol from master_index where isactive = true", nativeQuery = true)
    List<Map<String, Object>> getAllActiveIndexes();
}