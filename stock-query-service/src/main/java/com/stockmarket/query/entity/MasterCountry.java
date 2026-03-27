package com.stockmarket.query.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "master_country")
public class MasterCountry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "countryid")
    private Integer countryId;

    @Column(name = "countrycode")
    private String countryCode;

    @Column(name = "countryname")
    private String countryName;

    @Column(name = "phonecode")
    private String phoneCode;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "displayorder")
    private Integer displayOrder;

    @Column(name = "showastab")
    private Boolean showAsTab;

}