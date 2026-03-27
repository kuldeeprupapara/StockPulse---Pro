package com.stockmarket.query.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "master_index")
public class MasterIndex {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "indexid")
    private Integer indexId;

    @Column(name = "countryid")
    private Long countryId;

    @Column(name = "indexname")
    private String indexName;

    @Column(name = "index_symbol")
    private String indexSymbol;

    @Column(name = "exchange")
    private String exchange;

    @Column(name = "displayorder")
    private Integer displayOrder;

    @Column(name = "isfeatured")
    private Boolean isFeatured;

    @Column(name = "isactive")
    private Boolean isActive;

    @Column(name = "createdat")
    private OffsetDateTime createdAt;
}