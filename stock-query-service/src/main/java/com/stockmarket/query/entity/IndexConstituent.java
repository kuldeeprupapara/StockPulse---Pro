package com.stockmarket.query.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

@Getter
@Setter
@Entity
@Table(name = "index_constituents")
@NoArgsConstructor
@DynamicUpdate
public class IndexConstituent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "constituentid")
    private Integer constituentId;

    @Column(name = "indexid")
    private Long indexId;

    @Column(name = "symbolid")
    private Integer symbolId;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "isactive")
    private Boolean isActive;
}