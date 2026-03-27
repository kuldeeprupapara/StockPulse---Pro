package com.stockmarket.query.repository;

import com.stockmarket.query.entity.SymbolDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SymbolDetailRepository extends JpaRepository<SymbolDetail,Long> {
}
