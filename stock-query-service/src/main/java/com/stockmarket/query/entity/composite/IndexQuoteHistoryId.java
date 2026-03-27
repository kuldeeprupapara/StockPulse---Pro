package com.stockmarket.query.entity.composite;

import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.*;
import java.io.Serializable;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class IndexQuoteHistoryId implements Serializable {
    private Long historyId;
    private OffsetDateTime createdAt;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IndexQuoteHistoryId that = (IndexQuoteHistoryId) o;
        return Objects.equals(historyId, that.historyId) && Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(historyId, createdAt);
    }
}
